package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.machine.MachineBlockEntity;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.Identifier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 管道 BE v2(§10.5 / 参考文档《管道设计》§4.3)：
 * <ul>
 *   <li>逐格直传：每格 1000mB 缓冲；PULL=从机器输出面取，PUSH=注入机器输入面/相邻低水位管道</li>
 *   <li>流体类型锁：buffer 锁定首入流体，类型不符不吸收(不同内容物不互通)</li>
 *   <li>休眠：buffer 空且无邻居变化零逻辑(形状更新即唤醒)</li>
 *   <li>存档语义自带：逐格 buffer 即网络状态，无全图网络(可舍弃项见参考文档 I.6)</li>
 * </ul>
 */
public class PipeBlockEntity extends BlockEntity {
    public static final int CAPACITY = 1000;
    /** 每次传输量(mB)。 */
    public static final int TRANSFER = 100;
    /** 传输节拍(每 5 tick 一轮，空载零逻辑)。 */
    public static final int INTERVAL = 5;

    public FluidStack buffer = FluidStack.EMPTY;
    private int idleTicks;

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.PIPE.get(), pos, state);
    }

    public int amount() {
        return buffer.isEmpty() ? 0 : buffer.getAmount();
    }

    public int free() {
        return CAPACITY - amount();
    }

    /** 注入(遵守类型锁)。返回实际注入量。 */
    public int accept(FluidStack stack) {
        if (stack.isEmpty() || free() <= 0) return 0;
        if (buffer.isEmpty()) {
            int amt = Math.min(free(), stack.getAmount());
            buffer = stack.copyWithAmount(amt);
            sync();
            return amt;
        }
        if (buffer.getFluid() != stack.getFluid()) return 0;
        int amt = Math.min(free(), stack.getAmount());
        buffer = buffer.copyWithAmount(buffer.getAmount() + amt);
        sync();
        return amt;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, PipeBlockEntity be) {
        // 空载休眠：空 buffer 降频检查(1/20 概率)
        if (be.buffer.isEmpty()) {
            if (++be.idleTicks % 20 != 0) return;
        } else {
            be.idleTicks = 0;
        }
        if (level.getGameTime() % INTERVAL != 0) return;

        // ── 1. PULL：从机器输出面抽取(§10.5 机器 IO 决定流向) ──
        if (be.free() > 0) {
            for (Direction dir : Direction.values()) {
                if (state.getValue(PipeBlock.PROP_BY_DIR.get(dir)) != PipeBlock.Conn.MACHINE) continue;
                BlockPos nPos = pos.relative(dir);
                if (level.getBlockEntity(nPos) instanceof MachineBlockEntity m) {
                    FluidStack cur = m.tanks().isEmpty() ? FluidStack.EMPTY : m.tanks().get(0).getFluid();
                    if (be.buffer.isEmpty() || (!cur.isEmpty() && cur.getFluid() == be.buffer.getFluid())) {
                        FluidStack taken = m.drainForPipe(dir.getOpposite(), Math.min(TRANSFER, be.free()));
                        if (!taken.isEmpty()) be.accept(taken);
                    }
                }
            }
        }

        // ── 2. PUSH：优先机器输入面，其次相邻低水位管道(水位平衡，天然无环) ──
        if (!be.buffer.isEmpty()) {
            // 2a. 机器
            for (Direction dir : Direction.values()) {
                if (state.getValue(PipeBlock.PROP_BY_DIR.get(dir)) != PipeBlock.Conn.MACHINE) continue;
                BlockPos nPos = pos.relative(dir);
                if (level.getBlockEntity(nPos) instanceof MachineBlockEntity m) {
                    int filled = m.fillFromPipe(dir.getOpposite(),
                            be.buffer.copyWithAmount(Math.min(TRANSFER, be.amount())));
                    if (filled > 0) {
                        be.buffer = be.buffer.copyWithAmount(be.amount() - filled);
                        if (be.buffer.getAmount() <= 0) be.buffer = FluidStack.EMPTY;
                        be.sync();
                        if (be.buffer.isEmpty()) break;
                    }
                }
            }
            // 2b. 相邻管道(水位平衡：只流向 buffer 更不满的)
            if (!be.buffer.isEmpty()) {
                for (Direction dir : Direction.values()) {
                    if (state.getValue(PipeBlock.PROP_BY_DIR.get(dir)) != PipeBlock.Conn.PIPE) continue;
                    BlockPos nPos = pos.relative(dir);
                    if (level.getBlockEntity(nPos) instanceof PipeBlockEntity other) {
                        if (other.amount() + 1 < be.amount()) {
                            int move = Math.min(TRANSFER, (be.amount() - other.amount()) / 2);
                            if (move > 0) {
                                int moved = other.accept(be.buffer.copyWithAmount(move));
                                if (moved > 0) {
                                    be.buffer = be.buffer.copyWithAmount(be.amount() - moved);
                                    if (be.buffer.getAmount() <= 0) be.buffer = FluidStack.EMPTY;
                                    be.sync();
                                    if (be.buffer.isEmpty()) break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── 持久化(buffer 即网络状态，参考文档 I.6 saveShare 可舍弃) ──

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        if (!buffer.isEmpty()) {
            var list = out.list("fluid", FluidStack.OPTIONAL_CODEC);
            list.add(buffer);
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        for (FluidStack s : in.listOrEmpty("fluid", FluidStack.OPTIONAL_CODEC)) buffer = s;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!buffer.isEmpty()) {
            tag.putInt("amt", buffer.getAmount());
            var fid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(buffer.getFluid());
            tag.putString("fluid", fid == null ? "" : fid.toString());
        }
        return tag;
    }

    @Override
    public void onDataPacket(Connection conn, ValueInput in) {
        super.onDataPacket(conn, in);
        int amt = in.getIntOr("amt", 0);
        String fid = in.getStringOr("fluid", "");
        if (amt > 0 && !fid.isEmpty()) {
            var fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getValue(Identifier.parse(fid));
            if (fluid != null) {
                if (buffer.isEmpty() || buffer.getFluid() == fluid) {
                    buffer = new FluidStack(fluid, amt);
                } else {
                    buffer = buffer.copyWithAmount(amt);
                }
                return;
            }
        }
        buffer = FluidStack.EMPTY;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
