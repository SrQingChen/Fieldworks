package com.srqingchen.fieldworks.block;

import com.mojang.serialization.MapCodec;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import com.srqingchen.fieldworks.fieldengine.FieldSnapshot;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
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
 * 观测/显示设备(§17 探测器分级 T1—T3)：磁场仪/潮汐计/碳压计/观测灯塔/场谱仪。
 * 右键 → 服务端采样 → 聊天栏回报读数(T2 场谱仪走 GUI 由客户端 Screen 实现)。
 */
public class DisplayBlock extends BaseEntityBlock {
    public static final MapCodec<DisplayBlock> CODEC = simpleCodec(DisplayBlock::new);
    private final String mode;

    public DisplayBlock(Properties props, String mode) {
        super(props);
        this.mode = mode;
    }

    public DisplayBlock(Properties props) {
        this(props, "magnetometer");
    }

    public String mode() {
        return mode;
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
        return new DisplayBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        FieldSnapshot snap = FieldEngine.snapshot(sl, pos);
        switch (mode) {
            case "magnetometer" -> sp.sendSystemMessage(Component.literal("§b[磁场仪] §7").append(
                    Component.literal(String.format("%.2f T", snap.value(com.srqingchen.fieldworks.fieldengine.FWFields.MAGNETIC)))));
            case "tidal_meter" -> {
                var phase = sl.environmentAttributes().getValue(
                        net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE, pos);
                sp.sendSystemMessage(Component.literal("§b[潮汐计] §7月相 " + phase.name()
                        + " 引力 " + String.format("%.2f g", snap.value(com.srqingchen.fieldworks.fieldengine.FWFields.GRAVITY))));
            }
            case "carbon_gauge" -> {
                sp.sendSystemMessage(Component.literal("§b[碳压计] §7cp = "
                        + String.format("%.1f", snap.value(com.srqingchen.fieldworks.fieldengine.FWFields.CARBON))
                        + " (平衡点 200)"));
                com.srqingchen.fieldworks.util.AdvancementHelper.award(sp, "carbon_ledger");
            }
            case "spectrometer" -> {
                StringBuilder sb = new StringBuilder("§b[场谱仪] §7");
                sb.append(String.format("温 %.1f℃ 湿 %.0f%% 光 %d | ", snap.value(com.srqingchen.fieldworks.fieldengine.FWFields.TEMPERATURE),
                        snap.value(com.srqingchen.fieldworks.fieldengine.FWFields.HUMIDITY),
                        (int) snap.value(com.srqingchen.fieldworks.fieldengine.FWFields.LIGHT)));
                sb.append(String.format("磁 %.2fT 引 %.2fg 空 %.1e帕 | ", snap.value(com.srqingchen.fieldworks.fieldengine.FWFields.MAGNETIC),
                        snap.value(com.srqingchen.fieldworks.fieldengine.FWFields.GRAVITY),
                        snap.value(com.srqingchen.fieldworks.fieldengine.FWFields.VACUUM)));
                sb.append("大气 ").append(snap.atmosphereType().getPath());
                sp.sendSystemMessage(Component.literal(sb.toString()));
            }
            default -> sp.sendSystemMessage(Component.literal("§b[观测灯塔] §7全服 cp = "
                    + String.format("%.1f", com.srqingchen.fieldworks.carbon.CarbonState.cp())));
        }
        return InteractionResult.CONSUME;
    }
}
