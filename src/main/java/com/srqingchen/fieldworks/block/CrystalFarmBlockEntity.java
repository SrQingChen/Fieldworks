package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.registry.FWBlockEntities;
import com.srqingchen.fieldworks.registry.FWItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 晶圃 BE(§10.3)：在磁场环境中缓慢培育谐振晶簇碎片——
 * 每 20 分钟产 1 谐振碎片(磁场越强越快，场感知驱动)。
 */
public class CrystalFarmBlockEntity extends BlockEntity {
    private int growth;

    public CrystalFarmBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.CRYSTAL_FARM.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, CrystalFarmBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel sl)) return;
        // 场感知：磁场强度决定生长速度(§10.3 晶圃)
        float magnetic = com.srqingchen.fieldworks.fieldengine.FieldEngine.sample(sl, pos,
                com.srqingchen.fieldworks.fieldengine.FWFields.MAGNETIC);
        if (magnetic < 0.1f) return;
        be.growth += (int) (magnetic * 20);
        if (be.growth >= 24000) {   // ~20 分钟(强场更快)
            be.growth = 0;
            var drop = new net.minecraft.world.entity.item.ItemEntity(sl,
                    pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5,
                    new ItemStack(FWItems.RESONANT_SHARD.get()));
            sl.addFreshEntity(drop);
        }
        be.setChanged();
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("growth", growth);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        growth = in.getIntOr("growth", 0);
    }
}
