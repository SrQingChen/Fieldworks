package com.srqingchen.fieldworks.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * 巨物投影锚点(§13 蓝图系统)：
 * 技术方块 —— 不可见碰撞(小芯)，持蓝图右键地面放置；
 * Shift+右键触发自动搭建(材料消耗与缺料提示见 BE)。
 */
public class ProjectionBlock extends BaseEntityBlock {
    public static final MapCodec<ProjectionBlock> CODEC = simpleCodec(ProjectionBlock::new);
    private static final VoxelShape SHAPE = Block.box(6, 6, 6, 10, 10, 10);

    public ProjectionBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                                  BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected float getShadeBrightness(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        return 1.0f;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProjectionBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        if (level.isClientSide() || !(level instanceof net.minecraft.server.level.ServerLevel sl)) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ProjectionBlockEntity be
                && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            // 任意手持 Shift+右键 → 自动搭建(§13 用户语义)
            if (player.isShiftKeyDown()) {
                be.tryBuild(sl, sp);
            } else {
                be.sendStatus(sl, sp);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    public void onBlockStateChange(net.minecraft.world.level.LevelReader level, BlockPos pos,
                                   BlockState oldState, BlockState newState) {
        if (level instanceof Level l && !newState.is(this)) {
            if (l.getBlockEntity(pos) instanceof ProjectionBlockEntity be && !be.building) {
                be.dismissAndDrop(l);
            }
        }
    }
}
