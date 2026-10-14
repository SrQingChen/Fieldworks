package com.srqingchen.fieldworks.envelope;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.atmosphere.AtmosphereRegistry;
import com.srqingchen.fieldworks.api.atmosphere.TraitVector;
import com.srqingchen.fieldworks.fieldengine.FWFields;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import com.srqingchen.fieldworks.registry.FWItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jspecify.annotations.Nullable;

/**
 * 人工场五件套方块实体(§11.1)：框架/输入器/耦合器/约束器/稳定器。
 * 框架成形 = 一次结构校验(事件驱动)——扫描包络内部(一次性，非每 tick)：
 * 内部中央输入器 + 介质 + 供能 → 包络登记(§11.1.1)。
 */
public class FieldPartBlockEntity extends BlockEntity {
    public enum Kind {FRAME, INJECTOR, COUPLER, CONSTRAINER, STABILIZER}

    public final Kind kind;
    /** 介质槽(输入器)：放什么介质成什么场(§11.1)。 */
    public ItemStack medium = ItemStack.EMPTY;
    /** 介质损耗(耐用品+缓耗：1%/现实小时，§11.2)。 */
    public float mediumWear;
    /** 输入器选择的场型(GUI 模式旋钮)。 */
    public Identifier selectedField = Fieldworks.id("em_field");
    public final SimpleEnergyHandler energy = new SimpleEnergyHandler(64_000, 512, 0);
    /** 框架包络尺寸(边长，3..32，§11.1.1)。 */
    public int frameSize = 3;
    public boolean formed;

    public FieldPartBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.FIELD_PART.get(), pos, state);
        this.kind = kindOf(state);
    }

    public static Kind kindOf(BlockState state) {
        var b = state.getBlock();
        if (b == com.srqingchen.fieldworks.registry.FWBlocks.FIELD_INJECTOR.get()) return Kind.INJECTOR;
        if (b == com.srqingchen.fieldworks.registry.FWBlocks.FIELD_COUPLER.get()) return Kind.COUPLER;
        if (b == com.srqingchen.fieldworks.registry.FWBlocks.FIELD_CONSTRAINER.get()) return Kind.CONSTRAINER;
        if (b == com.srqingchen.fieldworks.registry.FWBlocks.FIELD_STABILIZER.get()) return Kind.STABILIZER;
        return Kind.FRAME;
    }

    /**
     * 框架结构校验与成形(§11.1 搭建流程)：放置/变更事件触发，一次性扫描。
     * 简化为"框架立方体边框完整 + 内部≥1 输入器 + 供能"→ 包络登记。
     */
    public void tryForm(ServerLevel level) {
        if (kind != Kind.FRAME) return;
        int half = frameSize / 2;
        BlockPos min = worldPosition.offset(-half, -half, -half);
        BlockPos max = worldPosition.offset(half, half, half);
        // 校验 8 角是框架(简化结构规则：角 + 中央输入器；整壳校验成本高)
        boolean ok = isFrame(level, min) && isFrame(level, max)
                && isFrame(level, new BlockPos(min.getX(), min.getY(), max.getZ()))
                && isFrame(level, new BlockPos(max.getX(), max.getY(), min.getZ()));
        boolean constrained = false, stabilized = false;
        FieldPartBlockEntity injector = null;
        int injectors = 0;
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(p) instanceof FieldPartBlockEntity part) {
                switch (part.kind) {
                    case INJECTOR -> {
                        injectors++;
                        if (injector == null) injector = part;
                    }
                    case CONSTRAINER -> constrained = true;
                    case STABILIZER -> stabilized = true;
                    default -> {}
                }
            }
        }
        formed = ok && injectors >= 1;
        if (!formed) {
            EnvelopeManager.remove(level, worldPosition.asLong());
            setChanged();
            return;
        }
        // 耦合增强表(§11.2)：×1.6/个 同场型；耦合容量 1+边长/8
        int cap = 1 + frameSize / 8;
        float strength = baseStrength(injector.selectedField);
        int extra = Math.min(cap, injectors) - 1;
        strength = couplingScale(injector.selectedField, strength, extra);
        if (stabilized) strength *= 1.2f;   // 稳定器 +20% 上限(§11.1)
        Identifier field = injector.selectedField;
        // 生态穹顶类包络(四大场全控)由 eco_dome 巨物提供，此处仅场型包络
        var env = new EnvelopeManager.Envelope(
                new EnvelopeManager.ResourceKey(level.dimension().identifier().toString()),
                new AABB(min), worldPosition.asLong(), field, strength, constrained, stabilized, null, null);
        EnvelopeManager.register(level, env);
        // 温度/湿度等四大场的人工覆盖(生态穹顶之外的简化：电磁/引力/真空/辐射/虚拟场为主)
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
    }

    private boolean isFrame(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).is(com.srqingchen.fieldworks.registry.FWBlocks.FIELD_FRAME.get());
    }

    /** 单输入器场强上限(§11.2 参数表)。 */
    public static float baseStrength(Identifier field) {
        return switch (field.getPath()) {
            case "em_field" -> 8f;
            case "grav_field" -> 5f;
            case "vac_field" -> 1e2f;        // 10²Pa(越低越真空)
            case "rad_field" -> 1e3f;
            case "harmonic" -> 6f;
            case "tame_field" -> 40f;
            case "warp" -> 3f;
            default -> 1f;
        };
    }

    /** 耦合增强(§11.2 表)：电磁 ×1.6/引力 ×1.5/真空 −10²Pa/辐射 ×3/谐律 +2/驯息 +20。 */
    public static float couplingScale(Identifier field, float base, int extraInjectors) {
        if (extraInjectors <= 0) return base;
        return switch (field.getPath()) {
            case "em_field" -> Math.min(100f, base * (float) Math.pow(1.6, extraInjectors));
            case "grav_field" -> Math.min(50f, base * (float) Math.pow(1.5, extraInjectors));
            case "vac_field" -> Math.max(1e-9f, (float) (base * Math.pow(0.01, extraInjectors)));
            case "rad_field" -> Math.min(1e6f, base * (float) Math.pow(3, extraInjectors));
            case "harmonic" -> Math.min(12f, base + 2f * extraInjectors);
            case "tame_field" -> Math.min(100f, base + 20f * extraInjectors);
            default -> base;
        };
    }

    /** 介质→场型映射(§11.2 介质材料列)。 */
    public static Identifier fieldOfMedium(ItemStack medium) {
        if (medium.isEmpty()) return Fieldworks.id("em_field");
        if (medium.getItem() == FWItems.MAGNETIZED_CORE.get() || medium.getItem() == FWItems.PERMALLOY_INGOT.get()) return Fieldworks.id("em_field");
        if (medium.getItem() == FWItems.TIDAL_RESONATOR.get() || medium.getItem() == FWItems.FLUX_ALLOY_INGOT.get()) return Fieldworks.id("grav_field");
        if (medium.getItem() == net.minecraft.world.item.Items.PHANTOM_MEMBRANE || medium.getItem() == FWItems.VOID_LENS.get()) return Fieldworks.id("vac_field");
        if (medium.getItem() == FWItems.THORIUM_PELLET.get()) return Fieldworks.id("rad_field");
        if (medium.getItem() == FWItems.TUNING_FORK.get() || medium.getItem() == FWItems.AMETHYST_RESONATOR.get()) return Fieldworks.id("harmonic");
        if (medium.getItem() == net.minecraft.world.item.Items.GLOWSTONE_DUST || medium.getItem() == net.minecraft.world.item.Items.RABBIT_FOOT) return Fieldworks.id("tame_field");
        if (medium.getItem() == FWItems.VOID_LENS.get() || medium.getItem() == net.minecraft.world.item.Items.NETHER_STAR) return Fieldworks.id("warp");
        return Fieldworks.id("em_field");
    }

    // ── 持久化 ──

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("frame_size", frameSize);
        out.putBoolean("formed", formed);
        out.putFloat("wear", mediumWear);
        out.putString("selected_field", selectedField.toString());
        if (!medium.isEmpty()) {
            var list = out.list("medium", ItemStack.OPTIONAL_CODEC);
            list.add(medium);
        }
        energy.serialize(out);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        frameSize = in.getIntOr("frame_size", 3);
        formed = in.getBooleanOr("formed", false);
        mediumWear = in.getFloatOr("wear", 0f);
        try {
            selectedField = Identifier.parse(in.getStringOr("selected_field", Fieldworks.id("em_field").toString()));
        } catch (Exception ignored) {}
        for (ItemStack s : in.listOrEmpty("medium", ItemStack.OPTIONAL_CODEC)) {
            medium = s;
        }
        energy.deserialize(in);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("formed", formed);
        tag.putInt("size", frameSize);
        tag.putString("field", selectedField.toString());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void setMedium(ItemStack stack) {
        this.medium = stack;
        this.selectedField = fieldOfMedium(stack);
        setChanged();
    }
}
