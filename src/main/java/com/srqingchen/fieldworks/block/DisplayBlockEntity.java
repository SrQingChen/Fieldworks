package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 显示设备 BE(占位：读数为即时采样，无状态)。 */
public class DisplayBlockEntity extends BlockEntity {
    public DisplayBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.DISPLAY.get(), pos, state);
    }
}
