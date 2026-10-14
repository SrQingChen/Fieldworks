package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 能量管道 BE(261014 T2)：FE 逐格直传，几何/节拍与流体管同构。
 * <ul>
 *   <li>PULL=从发电机(OUTPUT/AUTO 面)取电；PUSH=送入耗电机器(INPUT/AUTO 面)与相邻低水位管道</li>
 *   <li>水位平衡(只流向更不满的管道)天然无环、无网络重建成本</li>
 * </ul>
 */
public class EnergyPipeBlockEntity extends BlockEntity {
    public static final int CAPACITY = 2000;
    /** 每 5t 一轮、每轮最多 200FE(=40FE/t 吞吐)。 */
    public static final int TRANSFER = 200;
    public static final int INTERVAL = 5;

    public int buffer;
    private int idleTicks;

    public EnergyPipeBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.ENERGY_PIPE.get(), pos, state);
    }

    public int amount() {
        return buffer;
    }

    public int free() {
        return CAPACITY - buffer;
    }

    /** 注入。返回实际注入量。 */
    public int accept(int amount) {
        if (amount <= 0 || free() <= 0) return 0;
        int amt = Math.min(free(), amount);
        buffer += amt;
        sync();
        return amt;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, EnergyPipeBlockEntity be) {
        if (be.buffer <= 0) {
            if (++be.idleTicks % 20 != 0) return;
        } else {
            be.idleTicks = 0;
        }
        if (level.getGameTime() % INTERVAL != 0) return;

        // ── 1. PULL：发电机输出面取电 ──
        if (be.free() > 0) {
            for (Direction dir : Direction.values()) {
                if (state.getValue(PipeBlock.PROP_BY_DIR.get(dir)) != PipeBlock.Conn.MACHINE) continue;
                if (level.getBlockEntity(pos.relative(dir)) instanceof
                        com.srqingchen.fieldworks.machine.MachineBlockEntity m) {
                    int taken = m.extractEnergyForPipe(dir.getOpposite(), Math.min(TRANSFER, be.free()));
                    if (taken > 0) be.accept(taken);
                }
            }
        }

        // ── 2. PUSH：耗电机器 → 相邻低水位能量管 ──
        if (be.buffer > 0) {
            for (Direction dir : Direction.values()) {
                if (state.getValue(PipeBlock.PROP_BY_DIR.get(dir)) != PipeBlock.Conn.MACHINE) continue;
                if (level.getBlockEntity(pos.relative(dir)) instanceof
                        com.srqingchen.fieldworks.machine.MachineBlockEntity m) {
                    int moved = m.insertEnergyFromPipe(dir.getOpposite(), Math.min(TRANSFER, be.buffer));
                    if (moved > 0) {
                        be.buffer -= moved;
                        be.sync();
                        if (be.buffer <= 0) break;
                    }
                }
            }
            if (be.buffer > 0) {
                for (Direction dir : Direction.values()) {
                    if (state.getValue(PipeBlock.PROP_BY_DIR.get(dir)) != PipeBlock.Conn.PIPE) continue;
                    if (level.getBlockEntity(pos.relative(dir)) instanceof EnergyPipeBlockEntity other) {
                        if (other.amount() + 1 < be.buffer) {
                            int move = Math.min(TRANSFER, (be.buffer - other.amount()) / 2);
                            if (move > 0) {
                                int moved = other.accept(move);
                                if (moved > 0) {
                                    be.buffer -= moved;
                                    be.sync();
                                    if (be.buffer <= 0) break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("fe", buffer);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        buffer = Math.max(0, Math.min(CAPACITY, in.getIntOr("fe", 0)));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("amt", buffer);
        return tag;
    }

    @Override
    public void onDataPacket(Connection conn, ValueInput in) {
        super.onDataPacket(conn, in);
        buffer = Math.max(0, Math.min(CAPACITY, in.getIntOr("amt", 0)));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
