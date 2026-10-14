package com.srqingchen.fieldworks.machine;

import net.minecraft.core.NonNullList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/**
 * 机器气体/流体罐：基于新版 transfer API 的 FluidStacksResourceHandler。
 * 管道、能力系统与序列化(ValueIOSerializable)三合一。
 */
public class FWTank extends FluidStacksResourceHandler {
    private Runnable onChange = () -> {};

    public FWTank(int capacity) {
        super(1, capacity);
    }

    public FWTank(int capacity, Runnable onChange) {
        super(1, capacity);
        this.onChange = onChange;
    }

    @Override
    protected void onContentsChanged(int index, FluidStack previousContents) {
        onChange.run();
    }

    public FluidStack getFluid() {
        return this.stacks.get(0);
    }

    public NonNullList<FluidStack> rawStacks() {
        return this.stacks;
    }

    public boolean isEmpty() {
        return getFluid().isEmpty();
    }

    public int getAmount() {
        return getFluid().getAmount();
    }

    public int getCapacity() {
        return this.capacity;
    }

    /** 内部直接填充(不走事务)。返回实际填充量。 */
    public int fillInternal(FluidStack stack) {
        if (stack.isEmpty()) return 0;
        FluidStack cur = getFluid();
        if (cur.isEmpty()) {
            int amt = Math.min(stack.getAmount(), capacity);
            setStackDirect(stack.copyWithAmount(amt));
            return amt;
        }
        if (cur.getFluid() != stack.getFluid()) return 0;
        int room = capacity - cur.getAmount();
        int amt = Math.min(room, stack.getAmount());
        if (amt > 0) setStackDirect(cur.copyWithAmount(cur.getAmount() + amt));
        return amt;
    }

    /** 内部直接抽取。 */
    public FluidStack drainInternal(int maxAmount) {
        FluidStack cur = getFluid();
        if (cur.isEmpty()) return FluidStack.EMPTY;
        int amt = Math.min(maxAmount, cur.getAmount());
        FluidStack out = cur.copyWithAmount(amt);
        setStackDirect(cur.copyWithAmount(cur.getAmount() - amt));
        return out;
    }

    private void setStackDirect(FluidStack stack) {
        this.stacks.set(0, stack);
        onChange.run();
    }
}
