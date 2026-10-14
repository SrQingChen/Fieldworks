package com.srqingchen.fieldworks.block;

import com.mojang.serialization.MapCodec;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 巨物控制器方块(§13.1)：
 * 放置后自动吸附检测结构(Thermal 式)——检查周围 9³ 内的结构三件套数量；
 * 成形即自带区块加载(无需通电，§13.1)；错误位置粒子标注缺失件(诊断到方块，D5)。
 */
public class GiantControllerBlock extends BaseEntityBlock {
    public static final MapCodec<GiantControllerBlock> CODEC = simpleCodec(GiantControllerBlock::new);

    /** 0=聚能塔 1=深井电解阵列 2=生态穹顶 3=曲率锚 4=谐律大教堂 5=行星调谐阵列。 */
    private final int variant;

    public GiantControllerBlock(Properties props, int variant) {
        super(props);
        this.variant = variant;
    }

    public GiantControllerBlock(Properties props) {
        this(props, 0);
    }

    public int variant() {
        return variant;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GiantControllerEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, FWBlockEntities.GIANT.get(), GiantControllerEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level instanceof ServerLevel sl)) return InteractionResult.TRY_WITH_EMPTY_HAND;
        if (level.getBlockEntity(pos) instanceof GiantControllerEntity be) {
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.translatable("gui.fieldworks.giant.status." + (be.formed ? "formed" : "incomplete"),
                        be.structureProgress()));
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    /**
     * 261014 T8：手持木棍点击控制器 → 尽力搭建。
     * 4 向旋转择优(匹配数最多、阻挡最少)，背包有料即放；缺料不阻塞，
     * 结束后聊天栏报告 已放置 N/总数 + 缺料清单(物品名×数量)。
     */
    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
                                          BlockHitResult hit) {
        if (!stack.is(net.minecraft.world.item.Items.STICK) || !(level instanceof ServerLevel sl)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.CONSUME;
        stickBuild(sl, pos, sp);
        return InteractionResult.CONSUME;
    }

    private void stickBuild(ServerLevel level, BlockPos pos, ServerPlayer player) {
        var gs = com.srqingchen.fieldworks.giant.GiantStructure.load(level.getServer(), GiantControllerEntity.VARIANT_NAMES[variant]);
        if (gs == null || gs.blocks().isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.fieldworks.stickbuild.invalid"));
            return;
        }
        int[] c = gs.controllerOffset();
        // ── 1. 四向旋转择优：匹配多者优先，同分取阻挡少者 ──
        int bestRot = 0, bestScore = -1, bestBlocked = Integer.MAX_VALUE;
        for (int rot = 0; rot < 4; rot++) {
            int match = 0, blocked = 0;
            for (var p : gs.blocks()) {
                BlockPos wp = rotateOffset(pos, p, c, rot);
                if (wp.equals(pos)) continue;
                var cur = level.getBlockState(wp);
                if (cur.getBlock() == p.block()) match++;
                else if (!cur.canBeReplaced()) blocked++;
            }
            int score = match * 100 - blocked * 10;
            if (score > bestScore || (score == bestScore && blocked < bestBlocked)) {
                bestScore = score;
                bestBlocked = blocked;
                bestRot = rot;
            }
        }

        // ── 2. 背包清点(方块物品；控制核心位不消耗) ──
        var inv = player.getInventory();
        java.util.Map<net.minecraft.world.level.block.Block, Integer> have = new java.util.HashMap<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
                have.merge(bi.getBlock(), s.getCount(), Integer::sum);
            }
        }

        // ── 3. 尽力放置：已正确的不动；有料即扣即放；缺料累计(不阻塞其余位置) ──
        int placed = 0, already = 0;
        java.util.Map<net.minecraft.world.level.block.Block, Integer> missing = new java.util.LinkedHashMap<>();
        for (var p : gs.blocks()) {
            BlockPos wp = rotateOffset(pos, p, c, bestRot);
            if (wp.equals(pos)) continue;   // 控制器自身
            var cur = level.getBlockState(wp);
            if (cur.getBlock() == p.block()) {
                already++;
                continue;
            }
            if (!cur.canBeReplaced()) continue;   // 阻挡位跳过(不拆玩家建筑)
            int got = have.getOrDefault(p.block(), 0);
            if (got > 0) {
                have.put(p.block(), got - 1);
                consumeBlock(level, inv, p.block(), 1);
                level.setBlock(wp, p.block().defaultBlockState(), 3);
                placed++;
            } else {
                missing.merge(p.block(), 1, Integer::sum);
            }
        }

        // ── 4. 报告 ──
        int total = gs.blocks().size();
        player.sendSystemMessage(Component.translatable("message.fieldworks.stickbuild.result",
                placed, already, total, bestRot * 90));
        if (!missing.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.fieldworks.stickbuild.missing_header"));
            for (var e : missing.entrySet()) {
                player.sendSystemMessage(Component.translatable("message.fieldworks.stickbuild.missing_entry",
                        e.getKey().getName(), e.getValue()));
            }
        }
        if (placed > 0) {
            level.playSound(null, pos, net.minecraft.sounds.SoundEvents.SMITHING_TABLE_USE,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1f, 0.8f);
        }
    }

    private static BlockPos rotateOffset(BlockPos center, com.srqingchen.fieldworks.giant.GiantStructure.Placed p,
                                         int[] ctrl, int rot) {
        int dx = p.x() - ctrl[0], dy = p.y() - ctrl[1], dz = p.z() - ctrl[2];
        int rx, rz;
        switch (rot) {
            case 1 -> { rx = dz; rz = -dx; }
            case 2 -> { rx = -dx; rz = -dz; }
            case 3 -> { rx = -dz; rz = dx; }
            default -> { rx = dx; rz = dz; }
        }
        return center.offset(rx, dy, rz);
    }

    /** 从背包逐槽扣除指定方块物品 count 件。 */
    private static void consumeBlock(Level level, net.minecraft.world.entity.player.Inventory inv,
                                     net.minecraft.world.level.block.Block block, int count) {
        int left = count;
        for (int i = 0; i < inv.getContainerSize() && left > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() == block) {
                int take = Math.min(left, s.getCount());
                s.shrink(take);
                left -= take;
                if (s.isEmpty()) inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }
}
