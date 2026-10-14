package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 物品管道 BE(261014 T2)：与流体管同构的逐格直传。
 * <ul>
 *   <li>缓冲：单物品类型锁(首入定类型)，上限 {@link #CAPACITY} 件</li>
 *   <li>PULL=从机器输出面取成品；PUSH=送入机器输入面/相邻低水位管道(水位平衡天然无环)</li>
 *   <li>休眠：空缓冲且无邻居变化零逻辑</li>
 * </ul>
 */
public class ItemPipeBlockEntity extends BlockEntity {
    public static final int CAPACITY = 16;
    /** 每 5t 一轮、每轮最多搬运件数。 */
    public static final int TRANSFER = 4;
    public static final int INTERVAL = 5;

    public ItemStack buffer = ItemStack.EMPTY;
    private int idleTicks;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.ITEM_PIPE.get(), pos, state);
    }

    public int count() {
        return buffer.isEmpty() ? 0 : buffer.getCount();
    }

    public int free() {
        return CAPACITY - count();
    }

    /** 注入(同物品锁)。返回实际放入件数。 */
    public int accept(ItemStack stack) {
        if (stack.isEmpty() || free() <= 0) return 0;
        if (buffer.isEmpty()) {
            int amt = Math.min(free(), stack.getCount());
            buffer = stack.copyWithCount(amt);
            sync();
            return amt;
        }
        if (!ItemStack.isSameItemSameComponents(buffer, stack)) return 0;
        int amt = Math.min(free(), stack.getCount());
        buffer = buffer.copyWithCount(buffer.getCount() + amt);
        sync();
        return amt;
    }

    private void sync() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, ItemPipeBlockEntity be) {
        if (be.buffer.isEmpty()) {
            if (++be.idleTicks % 20 != 0) return;
        } else {
            be.idleTicks = 0;
        }
        if (level.getGameTime() % INTERVAL != 0) return;

        // ── 1. PULL：从机器输出面取成品 ──
        if (be.free() > 0) {
            for (Direction dir : Direction.values()) {
                if (state.getValue(PipeBlock.PROP_BY_DIR.get(dir)) != PipeBlock.Conn.MACHINE) continue;
                if (level.getBlockEntity(pos.relative(dir)) instanceof
                        com.srqingchen.fieldworks.machine.MachineBlockEntity m) {
                    ItemStack taken = m.extractItemsForPipe(dir.getOpposite(), Math.min(TRANSFER, be.free()));
                    if (!taken.isEmpty()) {
                        if (be.buffer.isEmpty() || ItemStack.isSameItemSameComponents(be.buffer, taken)) {
                            be.accept(taken);
                        } else {
                            // 类型不符：放回机器
                            m.insertItemFromPipe(dir.getOpposite(), taken);
                        }
                    }
                }
            }
        }

        // ── 2. PUSH：机器输入面 → 相邻低水位物品管 ──
        if (!be.buffer.isEmpty()) {
            for (Direction dir : Direction.values()) {
                if (state.getValue(PipeBlock.PROP_BY_DIR.get(dir)) != PipeBlock.Conn.MACHINE) continue;
                if (level.getBlockEntity(pos.relative(dir)) instanceof
                        com.srqingchen.fieldworks.machine.MachineBlockEntity m) {
                    int move = Math.min(TRANSFER, be.count());
                    int left = m.insertItemFromPipe(dir.getOpposite(), be.buffer.copyWithCount(move));
                    int moved = move - left;
                    if (moved > 0) {
                        be.buffer = be.buffer.copyWithCount(be.count() - moved);
                        if (be.buffer.getCount() <= 0) be.buffer = ItemStack.EMPTY;
                        be.sync();
                        if (be.buffer.isEmpty()) break;
                    }
                }
            }
            if (!be.buffer.isEmpty()) {
                for (Direction dir : Direction.values()) {
                    if (state.getValue(PipeBlock.PROP_BY_DIR.get(dir)) != PipeBlock.Conn.PIPE) continue;
                    if (level.getBlockEntity(pos.relative(dir)) instanceof ItemPipeBlockEntity other) {
                        if (other.count() + 1 < be.count()
                                && (other.buffer.isEmpty()
                                || ItemStack.isSameItemSameComponents(other.buffer, be.buffer))) {
                            int move = Math.min(TRANSFER, (be.count() - other.count()) / 2);
                            if (move > 0) {
                                int moved = other.accept(be.buffer.copyWithCount(move));
                                if (moved > 0) {
                                    be.buffer = be.buffer.copyWithCount(be.count() - moved);
                                    if (be.buffer.getCount() <= 0) be.buffer = ItemStack.EMPTY;
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

    // ── 持久化与同步 ──

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!buffer.isEmpty()) {
            var list = out.list("item", ItemStack.OPTIONAL_CODEC);
            list.add(buffer);
        }
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        buffer = ItemStack.EMPTY;
        for (ItemStack s : in.listOrEmpty("item", ItemStack.OPTIONAL_CODEC)) buffer = s;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        if (!buffer.isEmpty()) {
            tag.putInt("count", buffer.getCount());
            Identifier id = BuiltInRegistries.ITEM.getKey(buffer.getItem());
            tag.putString("item", id == null ? "" : id.toString());
        }
        return tag;
    }

    @Override
    public void onDataPacket(Connection conn, ValueInput in) {
        super.onDataPacket(conn, in);
        int count = in.getIntOr("count", 0);
        String id = in.getStringOr("item", "");
        if (count > 0 && !id.isEmpty()) {
            var item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
            if (item != null) {
                if (buffer.isEmpty() || buffer.is(item)) {
                    buffer = new ItemStack(item, Math.min(count, CAPACITY));
                } else {
                    buffer = buffer.copyWithCount(Math.min(count, CAPACITY));
                }
                return;
            }
        }
        buffer = ItemStack.EMPTY;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
