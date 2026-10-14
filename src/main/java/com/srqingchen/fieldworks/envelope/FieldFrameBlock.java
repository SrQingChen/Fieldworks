package com.srqingchen.fieldworks.envelope;

import com.mojang.serialization.MapCodec;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
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

/** 场框架(§11.1)：结构骨架，定义包络尺寸；成形即温和区块加载(§11.1.1)。 */
public class FieldFrameBlock extends BaseEntityBlock {
    public static final MapCodec<FieldFrameBlock> CODEC = simpleCodec(FieldFrameBlock::new);

    public FieldFrameBlock(Properties props) {
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
        if (level.isClientSide()) return null;
        return createTickerHelper(type, FWBlockEntities.FIELD_PART.get(), FieldFrameBlock::serverTick);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FieldPartBlockEntity be) {
        if (!(level instanceof ServerLevel sl) || be.kind != FieldPartBlockEntity.Kind.FRAME) return;
        if (level.getGameTime() % 100 != (pos.hashCode() & 63)) return;
        be.tryForm(sl);   // 周期复检(结构变更事件之外的兜底，§11.1 事件驱动为主)
        EnvelopeManager.tickDecay(sl);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // 放置交互：对框架右键介质物品 = 设定场型(简化交互，§11.1 搭积木)
        var stack = player.getMainHandItem();
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof FieldPartBlockEntity be) {
            if (!stack.isEmpty() && FieldPartBlockEntity.fieldOfMedium(stack) != null) {
                be.setMedium(stack.copyWithCount(1));
                stack.shrink(1);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }
}
