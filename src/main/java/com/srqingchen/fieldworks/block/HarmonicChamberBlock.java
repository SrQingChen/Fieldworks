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

/** 谐律腔(P3，§12.1)：附魔交互；调谐叉摆放决定谐律风格(祸律/禄律/庇律)。 */
public class HarmonicChamberBlock extends BaseEntityBlock {
    public static final MapCodec<HarmonicChamberBlock> CODEC = simpleCodec(HarmonicChamberBlock::new);

    public HarmonicChamberBlock(Properties props) {
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
        return new HarmonicChamberBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }
}
