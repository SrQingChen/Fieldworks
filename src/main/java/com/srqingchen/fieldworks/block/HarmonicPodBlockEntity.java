package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.harmonic.HarmonicStyle;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** 谐律附魔舱 BE(§14.1)：家用的弱化谐律腔(固定 4 律，范围 4 格)。 */
public class HarmonicPodBlockEntity extends BlockEntity {
    public HarmonicStyle style = HarmonicStyle.NONE;
    public float intensity = 4f;

    public HarmonicPodBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.HARMONIC_POD.get(), pos, state);
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putString("style", style.name());
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        try {
            style = HarmonicStyle.valueOf(in.getStringOr("style", "NONE"));
        } catch (IllegalArgumentException ignored) {}
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return new CompoundTag();
    }
}
