package com.srqingchen.fieldworks.block;

import com.mojang.serialization.MapCodec;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** 相位物品桥(§14.1 功能型机器)：本模组机器间的轻量直达通道(不做通用物流，§1.3)。 */
public class PhaseBridgeBlock extends BaseEntityBlock {
    public static final MapCodec<PhaseBridgeBlock> CODEC = simpleCodec(PhaseBridgeBlock::new);

    public PhaseBridgeBlock(Properties props) {
        super(props);
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
        return new PhaseBridgeBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, FWBlockEntities.PHASE_BRIDGE.get(), PhaseBridgeBlockEntity::serverTick);
    }
}
