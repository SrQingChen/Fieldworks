package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.fieldengine.EngineSavedData;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 谐振钻头 BE：钻穿基岩(数分钟 + 巨量能量，§4.4)→ 登记虚空界面 → 虚空气可采集。
 */
public class ResonantDrillBlockEntity extends BlockEntity {
    private static final int DRILL_TICKS = 6000;   // 5 分钟
    private int progress;
    private boolean drilled;

    public ResonantDrillBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.RESONANT_DRILL.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ResonantDrillBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel sl)) return;
        if (be.drilled) return;
        // 必须放在基岩上方
        BlockPos below = pos.below();
        if (!sl.getBlockState(below).is(Blocks.BEDROCK)) return;
        be.progress++;
        if (sl.getGameTime() % 40 == 0) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5, pos.getY() - 0.2, pos.getZ() + 0.5, 6, 0.3, 0.1, 0.3, 0.02);
        }
        if (be.progress >= DRILL_TICKS) {
            be.drilled = true;
            // 贯穿：基岩换为谐振孔洞，登记虚空界面(§4.4)
            sl.setBlock(below, Blocks.AIR.defaultBlockState(), 3);
            EngineSavedData d = FieldEngine.data();
            if (d != null) {
                d.portals.computeIfAbsent("void_interface", k -> new java.util.ArrayList<>())
                        .add(new long[]{below.getX(), below.getY(), below.getZ()});
                d.setDirty();
            }
            sl.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.5f, 0.6f);
            for (var p : sl.players()) {
                if (p instanceof ServerPlayer sp && sp.position().distanceTo(below.getCenter()) < 32) {
                    sp.sendSystemMessage(Component.translatable("message.fieldworks.void_interface_open"));
                }
            }
            FieldEngine.invalidateAll(sl);
            be.setChanged();
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("progress", progress);
        out.putBoolean("drilled", drilled);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        progress = in.getIntOr("progress", 0);
        drilled = in.getBooleanOr("drilled", false);
    }
}
