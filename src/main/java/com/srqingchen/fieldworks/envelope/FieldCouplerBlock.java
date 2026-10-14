package com.srqingchen.fieldworks.envelope;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 场耦合器(§11.1)：对输入器耦合——同场型增强 / 异场型多场叠加(场编程基础)。 */
public class FieldCouplerBlock extends BaseEntityBlock {
    public static final MapCodec<FieldCouplerBlock> CODEC = simpleCodec(FieldCouplerBlock::new);

    public FieldCouplerBlock(Properties props) {
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
