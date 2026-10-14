package com.srqingchen.fieldworks.block;

import com.mojang.serialization.MapCodec;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import com.srqingchen.fieldworks.registry.FWItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
 * 石制捕获器(§8.3 分钟 10—14)：空手右键充能，每击 +34%，三次充满；
 * 充满即弹出压缩空气(通用标签 fieldworks:compressed_air)并重置。
 */
public class CaptureBlock extends BaseEntityBlock {
    public static final MapCodec<CaptureBlock> CODEC = simpleCodec(CaptureBlock::new);

    public CaptureBlock(Properties props) {
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
        return new CaptureBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;   // 无 tick：纯交互驱动
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl)) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof CaptureBlockEntity be) {
            // 空手右键充能(V1.1：每击 +34%，三次充满，小仪式感)
            be.charge += 34;
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.6f, 0.8f + be.charge * 0.01f);
            if (be.charge >= 100) {
                be.charge = 0;
                ItemStack air = new ItemStack(FWItems.COMPRESSED_AIR.get(), 1);
                if (!player.getInventory().add(air)) {
                    net.minecraft.world.entity.item.ItemEntity drop =
                            new net.minecraft.world.entity.item.ItemEntity(sl, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, air);
                    sl.addFreshEntity(drop);
                }
                level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.8f, 1.4f);
            }
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
