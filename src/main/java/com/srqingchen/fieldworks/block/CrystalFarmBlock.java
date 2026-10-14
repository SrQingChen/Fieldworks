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

/** 晶圃(P2，§10.3)：人工培育谐振晶簇。 */
public class CrystalFarmBlock extends BaseEntityBlock {
    public static final MapCodec<CrystalFarmBlock> CODEC = simpleCodec(CrystalFarmBlock::new);

    public CrystalFarmBlock(Properties props) {
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
        return new CrystalFarmBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, FWBlockEntities.CRYSTAL_FARM.get(), CrystalFarmBlockEntity::serverTick);
    }
}
