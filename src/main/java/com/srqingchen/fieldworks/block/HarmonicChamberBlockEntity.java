package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.harmonic.HarmonicStyle;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import com.srqingchen.fieldworks.registry.FWItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 谐律腔 BE(§12.1)：周围 8 格内的附魔台享受谐律效果；
 * 风格由腔内调谐叉数量决定(V1.2 语义群组：祸律/禄律/庇律)，
 * 不同风格的谐律腔不叠加——区域内以最强腔为准(防堆叠白嫖 D8)。
 */
public class HarmonicChamberBlockEntity extends BlockEntity {
    public HarmonicStyle style = HarmonicStyle.NONE;
    /** 谐律强度(0—12 律)。 */
    public float intensity;
    public ItemStack fork = ItemStack.EMPTY;

    public HarmonicChamberBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.HARMONIC_CHAMBER.get(), pos, state);
    }

    /** 区域最强腔查询(§12.1)：附魔事件驱动调用(非每 tick)，扫描周围 17³ 域。 */
    public static HarmonicChamberBlockEntity strongest(ServerLevel level, BlockPos pos) {
        HarmonicChamberBlockEntity best = null;
        BlockPos min = pos.offset(-8, -8, -8);
        BlockPos max = pos.offset(8, 8, 8);
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(p) instanceof HarmonicChamberBlockEntity c) {
                if (c.intensity > 0 && (best == null || c.intensity > best.intensity)) {
                    best = c;
                }
            }
        }
        return best;
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putString("style", style.name());
        out.putFloat("intensity", intensity);
        if (!fork.isEmpty()) {
            var list = out.list("fork", ItemStack.OPTIONAL_CODEC);
            list.add(fork);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        try {
            style = HarmonicStyle.valueOf(in.getStringOr("style", "NONE"));
        } catch (IllegalArgumentException ignored) {}
        intensity = in.getFloatOr("intensity", 0);
        for (ItemStack s : in.listOrEmpty("fork", ItemStack.OPTIONAL_CODEC)) fork = s;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("intensity", intensity);
        tag.putString("style", style.name());
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
