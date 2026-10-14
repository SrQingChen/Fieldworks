package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.machine.IOSide;
import com.srqingchen.fieldworks.machine.MachineBlockEntity;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 相位物品桥 BE(§14.1)：配对桥(16 格内最近的同型桥)之间直传物品——
 * 输出面抽取 → 对端输入面注入，跳过路径(轻量直达，非通用物流)。
 */
public class PhaseBridgeBlockEntity extends BlockEntity {
    private BlockPos peer;

    public PhaseBridgeBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.PHASE_BRIDGE.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PhaseBridgeBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel sl)) return;
        if (sl.getGameTime() % 20 != (pos.hashCode() & 15)) return;
        if (be.peer == null) {
            be.peer = findPeer(sl, pos);
            if (be.peer != null) be.setChanged();
            else return;
        }
        if (!(sl.getBlockEntity(be.peer) instanceof PhaseBridgeBlockEntity other)) {
            be.peer = null;
            return;
        }
        // 相邻机器的输出面 → 对端相邻机器的输入面
        var from = adjacentOutput(sl, pos);
        var to = adjacentInput(sl, be.peer);
        if (from == null || to == null) return;
        for (int i = from.spec.inputSlots(); i < from.items().size() && i < from.spec.inputSlots() + 1; i++) {
            ItemStack out = from.items().get(i);
            if (out.isEmpty()) continue;
            int slot = to.spec.inputSlots();
            if (slot < to.items.size() && to.items().get(slot).isEmpty()) {
                to.items().set(slot, out.copy());
                from.items().set(i, ItemStack.EMPTY);
                from.setChanged();
                to.setChanged();
            }
            break;
        }
    }

    private static MachineBlockEntity adjacentOutput(ServerLevel sl, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (sl.getBlockEntity(pos.relative(d)) instanceof MachineBlockEntity m) {
                IOSide side = m.ioOf(d.getOpposite());
                if (side == IOSide.OUTPUT || side == IOSide.AUTO) return m;
            }
        }
        return null;
    }

    private static MachineBlockEntity adjacentInput(ServerLevel sl, BlockPos pos) {
        for (Direction d : Direction.values()) {
            if (sl.getBlockEntity(pos.relative(d)) instanceof MachineBlockEntity m) {
                IOSide side = m.ioOf(d.getOpposite());
                if (side == IOSide.INPUT || side == IOSide.AUTO) return m;
            }
        }
        return null;
    }

    private static BlockPos findPeer(ServerLevel sl, BlockPos pos) {
        BlockPos best = null;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-16, -16, -16), pos.offset(16, 16, 16))) {
            if (p.equals(pos)) continue;
            if (sl.getBlockEntity(p) instanceof PhaseBridgeBlockEntity) {
                if (best == null || p.distSqr(pos) < best.distSqr(pos)) {
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        if (peer != null) {
            out.putIntArray("peer", new int[]{peer.getX(), peer.getY(), peer.getZ()});
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        int[] p = in.getIntArray("peer").orElse(new int[0]);
        if (p.length == 3) peer = new BlockPos(p[0], p[1], p[2]);
    }
}
