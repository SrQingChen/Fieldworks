package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import com.srqingchen.fieldworks.giant.GiantStructure;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 投影锚点 BE(§13 蓝图投影搭建)：
 * <ul>
 *   <li>begin：读取结构 JSON，为目标方块生成 block_display 全息投影(vanilla 显示实体，零自研渲染)</li>
 *   <li>tryBuild：Shift+右键 → 清点背包 → 足额则自动放置全部方块并移除投影；缺料则聊天栏逐项提示</li>
 *   <li>dismiss：锚点破坏/移除时回收全部投影实体；破坏返还蓝图</li>
 * </ul>
 */
public class ProjectionBlockEntity extends BlockEntity {
    public String variant = "";
    public String owner = "";
    /** 建造中标记：抑制 onBlockStateChange 的蓝图返还(防重复)。 */
    public boolean building;
    private final List<UUID> displays = new ArrayList<>();

    public ProjectionBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.PROJECTION.get(), pos, state);
    }

    /** 放置投影(锚点即结构 bottom-center 原点)。 */
    public void begin(String variant, net.minecraft.world.entity.player.Player player) {
        this.variant = variant;
        this.owner = player.getGameProfile().name();
        setChanged();
        if (level instanceof ServerLevel sl) {
            spawnDisplays(sl);
        }
    }

    private void spawnDisplays(ServerLevel sl) {
        dismiss(sl);
        GiantStructure struct = GiantStructure.load(sl.getServer(), variant);
        if (struct == null) return;
        for (GiantStructure.Placed p : struct.blocks()) {
            BlockPos worldPos = worldPosition.offset(p.x(), p.y(), p.z());
            BlockState blockState = p.block().defaultBlockState();
            if (blockState.isAir() || worldPos.equals(worldPosition)) continue;
            // vanilla 召唤路径：NBT → loadEntityRecursive(全尺寸方块的精确投影)
            CompoundTag tag = new CompoundTag();
            tag.putString("id", "minecraft:block_display");
            tag.put("block_state", NbtUtils.writeBlockState(blockState));
            tag.putBoolean("NoGravity", true);
            tag.putBoolean("Silent", true);
            Entity e = EntityType.loadEntityRecursive(tag, sl, EntitySpawnReason.COMMAND, ent -> {
                ent.snapTo(worldPos.getX(), worldPos.getY(), worldPos.getZ(), 0f, 0f);
                return ent;
            });
            if (e != null && sl.tryAddFreshEntityWithPassengers(e)) {
                displays.add(e.getUUID());
            }
        }
    }

    /** 清点+搭建(§13：自动消耗背包材料；缺料提示)。 */
    public void tryBuild(ServerLevel sl, ServerPlayer player) {
        GiantStructure struct = GiantStructure.load(sl.getServer(), variant);
        if (struct == null || struct.blocks().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.fieldworks.projection.invalid"));
            return;
        }
        // ── 1. 材料清单(方块→数量；控制器位置消耗 巨物核心 giant_core 代替) ──
        Map<net.minecraft.world.level.block.Block, Integer> needed = new HashMap<>();
        int cores = 0;
        for (GiantStructure.Placed p : struct.blocks()) {
            if (p.block() instanceof GiantControllerBlock || p.at(worldPosition).equals(worldPosition)) {
                cores++;
                continue;
            }
            needed.merge(p.block(), 1, Integer::sum);
        }
        // ── 2. 空间校验(可被替换：空气/水/雪) ──
        for (GiantStructure.Placed p : struct.blocks()) {
            BlockPos worldPos = p.at(worldPosition);
            if (worldPos.equals(worldPosition)) continue;
            BlockState cur = sl.getBlockState(worldPos);
            if (!cur.canBeReplaced()) {
                player.sendSystemMessage(Component.translatable("message.fieldworks.projection.blocked",
                        worldPos.getX(), worldPos.getY(), worldPos.getZ()));
                return;
            }
        }
        // ── 3. 背包清点 ──
        var inv = player.getInventory();
        Map<net.minecraft.world.level.block.Block, Integer> have = new HashMap<>();
        int haveCores = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (s.getItem() instanceof BlockItem bi) {
                have.merge(bi.getBlock(), s.getCount(), Integer::sum);
            }
            if (s.getItem() == com.srqingchen.fieldworks.registry.FWItems.giantCoreItem()) {
                haveCores += s.getCount();
            }
        }
        List<Component> missing = new ArrayList<>();
        for (var e : needed.entrySet()) {
            int need = e.getValue(), got = have.getOrDefault(e.getKey(), 0);
            if (got < need) {
                missing.add(Component.translatable("message.fieldworks.projection.missing_entry",
                        e.getKey().getName(), need - got));
            }
        }
        if (cores > haveCores) {
            missing.add(Component.translatable("message.fieldworks.projection.missing_core", cores - haveCores));
        }
        if (!missing.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.fieldworks.projection.missing_header"));
            for (Component c : missing) player.sendSystemMessage(c);
            return;
        }
        // ── 4. 消耗(逐槽扣减) ──
        for (var e : needed.entrySet()) {
            int left = e.getValue();
            for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (s.getItem() instanceof BlockItem bi && bi.getBlock() == e.getKey()) {
                    int take = Math.min(left, s.getCount());
                    s.shrink(take);
                    left -= take;
                    if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
                }
            }
        }
        int leftCores = cores;
        for (int i = 0; i < inv.getContainerSize() && leftCores > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() == com.srqingchen.fieldworks.registry.FWItems.giantCoreItem()) {
                int take = Math.min(leftCores, s.getCount());
                s.shrink(take);
                leftCores -= take;
                if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
            }
        }
        // ── 5. 放置(控制器放锚点位置) ──
        BlockPos controllerPos = null;
        for (GiantStructure.Placed p : struct.blocks()) {
            BlockPos worldPos = p.at(worldPosition);
            if (p.block() instanceof GiantControllerBlock || worldPos.equals(worldPosition)) {
                controllerPos = worldPos;
                continue;
            }
            sl.setBlock(worldPos, p.block().defaultBlockState(), 3);
        }
        if (controllerPos != null) {
            BlockState ctrl = com.srqingchen.fieldworks.registry.FWBlocks.GIANT_CONTROLLER_OF(variant);
            sl.setBlock(controllerPos, ctrl != null ? ctrl : net.minecraft.world.level.block.Blocks.IRON_BLOCK.defaultBlockState(), 3);
        }
        sl.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 1f);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5,
                120, 2, 2, 2, 0.05);
        player.sendSystemMessage(Component.translatable("message.fieldworks.projection.built"));
        // ── 6. 移除投影(building 标记抑制蓝图返还) ──
        building = true;
        dismiss(sl);
        sl.removeBlock(worldPosition, false);
        building = false;
    }

    public void sendStatus(ServerLevel sl, ServerPlayer player) {
        GiantStructure struct = GiantStructure.load(sl.getServer(), variant);
        int count = struct == null ? 0 : struct.blocks().size();
        player.sendSystemMessage(Component.translatable("message.fieldworks.projection.status", count));
        player.sendSystemMessage(Component.translatable("message.fieldworks.projection.hint"));
    }

    /** 回收投影实体并返还蓝图。 */
    public void dismissAndDrop(Level level) {
        if (!variant.isEmpty() && level instanceof ServerLevel) {
            var stack = new net.minecraft.world.item.ItemStack(com.srqingchen.fieldworks.registry.FWItems.GIANT_BLUEPRINT.get());
            stack.set(com.srqingchen.fieldworks.registry.FWDataComponents.GIANT_VARIANT.get(),
                    Identifier.fromNamespaceAndPath(Fieldworks.MODID, variant));
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5,
                    worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, stack);
        }
        dismiss(level);
    }

    /** 回收投影实体。 */
    public void dismiss(Level level) {
        if (level instanceof ServerLevel sl) {
            for (UUID id : displays) {
                Entity e = sl.getEntity(id);
                if (e != null) e.discard();
            }
        }
        displays.clear();
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putString("variant", variant);
        out.putString("owner", owner);
        var list = out.list("displays", net.minecraft.core.UUIDUtil.CODEC);
        for (UUID id : displays) list.add(id);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        variant = in.getStringOr("variant", "");
        owner = in.getStringOr("owner", "");
        displays.clear();
        for (UUID id : in.listOrEmpty("displays", net.minecraft.core.UUIDUtil.CODEC)) {
            displays.add(id);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return new CompoundTag();
    }
}
