package com.srqingchen.fieldworks.envelope;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 场约束器(§11.1)：场边界硬化——0 外溢(§11.4 福利边界管理器，非必需品)。 */
public class FieldConstrainerBlock extends BaseEntityBlock {
    public static final MapCodec<FieldConstrainerBlock> CODEC = simpleCodec(FieldConstrainerBlock::new);

    public FieldConstrainerBlock(Properties props) {
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
