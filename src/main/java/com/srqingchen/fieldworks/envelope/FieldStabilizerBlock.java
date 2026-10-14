package com.srqingchen.fieldworks.envelope;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 场稳定器(§11.1)：平抑外部扰动，场强上限 +20%，失谐延缓 3 倍(§11.4)。 */
public class FieldStabilizerBlock extends BaseEntityBlock {
    public static final MapCodec<FieldStabilizerBlock> CODEC = simpleCodec(FieldStabilizerBlock::new);

    public FieldStabilizerBlock(Properties props) {
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
        return new FieldPartBlockEntity(pos, state);
    }
}
