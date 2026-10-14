package com.srqingchen.fieldworks.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * 无限水泵 BE(261014 T7)：
 * <ul>
 *   <li><b>水源判定</b>：六面中 ≥2 面对着的格子是水源方块才工作(浸入式取水)</li>
 *   <li><b>仅耗电</b>：不消耗任何物品/流体，把水直接产入内置罐(管道经 PULL 取走)并直推相邻机器</li>
 *   <li><b>三档位</b>：低 50mB/t(0.8FE/mB) · 高 150mB/t(1.2FE/mB) ·
 *       无限档按管道实际吞量输出，耗电 = 流量 × 0.8 × 2^(流量/500) 指数上升</li>
 *   <li>需求驱动：罐满且无人取水 → 零产水零耗电(效率自然取决于管道网络)</li>
 * </ul>
 */
public class InfinitePumpBlockEntity extends MachineBlockEntity {

    /** 各档位每 tick 目标产水量(mB)。无限档 1000 为硬上限，实际受管道吞吐约束。 */
    public static final int[] RATE_MB_PER_TICK = {50, 150, 1000};
    /** 低/高档位每 mB 耗电(FE)。 */
    public static final double FLAT_COST_0 = 0.8, FLAT_COST_1 = 1.2;

    /** 0=低档 1=高档 2=无限档。 */
    public int gear = 1;

    public InfinitePumpBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineSpec spec) {
        super(type, pos, state, spec);
    }

    public int gear() {
        return Math.max(0, Math.min(2, gear));
    }

    public void setGear(int g) {
        gear = Math.max(0, Math.min(2, g));
        setChanged();
    }

    /** 六面邻格中的水源方块数(仅 level=0 的源)。 */
    private int countWaterFaces(ServerLevel level) {
        int n = 0;
        for (Direction dir : Direction.values()) {
            var state = level.getBlockState(worldPosition.relative(dir));
            if (state.getBlock() == Blocks.WATER
                    && state.getOptionalValue(net.minecraft.world.level.block.LiquidBlock.LEVEL).orElse(0) == 0) {
                n++;
            }
        }
        return n;
    }

    @Override
    protected void tickPump(ServerLevel level) {
        if (countWaterFaces(level) < 2) {
            setActive(false);
            return;
        }
        int energyStored = getEnergyStored();
        if (energyStored <= 0 || tanks.isEmpty()) {
            setActive(false);
            return;
        }
        int g = gear();
        // ── 1. 能量预算 → 本 tick 可负担的最大流量 ──
        int affordable = maxFlowByEnergy(g, energyStored);
        int desired = Math.min(RATE_MB_PER_TICK[g], affordable);
        if (desired <= 0) {
            setActive(false);
            return;
        }

        // ── 2. 直推相邻机器(真实需求优先) ──
        FluidStack water = new FluidStack(net.minecraft.world.level.material.Fluids.WATER, desired);
        int pushed = 0;
        for (Direction dir : Direction.values()) {
            if (pushed >= desired) break;
            if (level.getBlockEntity(worldPosition.relative(dir)) instanceof MachineBlockEntity m) {
                water.setAmount(desired - pushed);
                pushed += m.fillFromPipe(dir.getOpposite(), water.copy());
            }
        }

        // ── 3. 余量产入内置罐(管道 PULL 取水；罐满/异流体不收——需求驱动+类型锁) ──
        int toTank = Math.min(desired - pushed,
                tanks.get(0).getCapacity() - tanks.get(0).getAmount());
        int stored = toTank > 0
                ? tanks.get(0).fillInternal(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, toTank))
                : 0;
        if (stored > 0) setChanged();

        // ── 4. 按实际输出流量结算耗电 ──
        int flow = pushed + stored;
        if (flow > 0) {
            double cost = costOf(g, flow);
            com.srqingchen.fieldworks.util.EnergyIo.extractSafe(energy, (int) Math.min(cost, getEnergyStored()));
            setActive(true);
        } else {
            setActive(false);
        }
    }

    /** 无限档：解 f·0.8·2^(f/500) ≤ energy 的最大 f(二分；平价档直接除)。 */
    private static int maxFlowByEnergy(int gear, int energyStored) {
        if (energyStored <= 0) return 0;
        if (gear == 0) return (int) (energyStored / FLAT_COST_0);
        if (gear == 1) return (int) (energyStored / FLAT_COST_1);
        int lo = 0, hi = RATE_MB_PER_TICK[2];
        while (lo < hi) {
            int mid = (lo + hi + 1) / 2;
            if (costOf(2, mid) <= energyStored) lo = mid;
            else hi = mid - 1;
        }
        return lo;
    }

    /** 实际输出 flow mB 的耗电(FE)。 */
    public static double costOf(int gear, int flow) {
        return switch (gear) {
            case 0 -> flow * FLAT_COST_0;
            case 1 -> flow * FLAT_COST_1;
            default -> flow * 0.8 * Math.pow(2.0, flow / 500.0);
        };
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putInt("gear", gear);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        gear = in.getIntOr("gear", 1);
    }
}
