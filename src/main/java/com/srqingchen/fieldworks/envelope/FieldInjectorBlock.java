package com.srqingchen.fieldworks.envelope;

import com.mojang.serialization.MapCodec;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
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

/** 场输入器(§11.1)：注入能量与介质，决定场型；耦合器的耦合对象。 */
public class FieldInjectorBlock extends BaseEntityBlock {
    public static final MapCodec<FieldInjectorBlock> CODEC = simpleCodec(FieldInjectorBlock::new);

    public FieldInjectorBlock(Properties props) {
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

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;   // 输入器无独立 tick(框架周期统一复检)
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var stack = player.getMainHandItem();
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FieldPartBlockEntity be) {
            if (!stack.isEmpty() && !be.medium.isEmpty() && be.medium.getCount() < 4) {
                be.medium.grow(1);
                stack.shrink(1);
                be.setChanged();
                return InteractionResult.CONSUME;
            }
            if (!stack.isEmpty() && be.medium.isEmpty()) {
                be.setMedium(stack.copyWithCount(1));
                stack.shrink(1);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
