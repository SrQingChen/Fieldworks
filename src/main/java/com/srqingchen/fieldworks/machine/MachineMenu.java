package com.srqingchen.fieldworks.machine;

import com.srqingchen.fieldworks.registry.FWBlockEntities;
import com.srqingchen.fieldworks.registry.FWItems;
import com.srqingchen.fieldworks.registry.FWMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * 机器菜单 v2(§15.2 / 参考文档 §8)：
 * <ul>
 *   <li>语义槽位：INPUT/OUTPUT/POWER/UPGRADE，坐标由 {@link MachineLayout} 模板驱动(§9 布局模板)</li>
 *   <li>数据槽 12：进度/能量lo/hi/Fit/active/燃烧%/tank0/tank1 量与上限</li>
 *   <li>quickMoveStack 按 REMAINING 语义重排</li>
 * </ul>
 */
public class MachineMenu extends AbstractContainerMenu {
    public final MachineBlockEntity machine;
    private final BlockPos pos;
    private final MachineLayout layout;

    // 数据槽索引(v3: 12→18, 新增环境实时镜像 §T4)
    public static final int D_PROGRESS = 0, D_ENERGY_LO = 1, D_ENERGY_HI = 2, D_FIT = 3, D_ACTIVE = 4,
            D_BURN = 5, D_TANK0 = 6, D_TANK0_CAP = 7, D_TANK1 = 8, D_TANK1_CAP = 9, D_MODE = 10, D_SPARE = 11,
            D_ENV_TEMP = 12, D_ENV_HUM = 13, D_ENV_LIGHT = 14, D_ENV_MAG = 15, D_ENV_GRAV = 16, D_ENV_ATMO = 17;

    private final ContainerData data;
    /** 客户端数据槽镜像(261014 T3 接线修复)：26.1.2 的 updateDataSlot 走 DataSlot.set()，
     *  原实现 set() 为空 → 客户端收到的能量/储罐/进度/环境值被丢弃，GUI 永远读客户端 BE 的
     *  静态旧值("发电不显示"根因；Jade 走 BE 数据包故正常)。现在 set() 写缓存、get() 客户端读缓存。 */
    private final int[] clientCache = new int[18];

    public MachineMenu(int id, Inventory inv, MachineBlockEntity machine) {
        super(FWMenus.MACHINE.get(), id);
        this.machine = machine;
        this.pos = machine.getBlockPos();
        this.layout = MachineLayout.of(machine.spec);
        boolean client = inv.player.level().isClientSide();
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return client ? clientCache[index] : live(index);
            }

            @Override
            public void set(int index, int value) {
                clientCache[index] = value;
            }

            @Override
            public int getCount() {
                return 18;
            }
        };
        layoutSlots(inv);
        addDataSlots(data);
    }

    private int live(int index) {
        MachineBlockEntity m = this.machine;
        if (m == null) return 0;
        return switch (index) {
            case D_PROGRESS -> m.getProgressPercent();
            case D_ENERGY_LO -> m.getEnergyStored() & 0xFFFF;
            case D_ENERGY_HI -> m.getEnergyStored() >>> 16;
            case D_FIT -> (int) (m.getFit() * 100);
            case D_ACTIVE -> m.isActive() ? 1 : 0;
            case D_BURN -> m.burnPercent();
            case D_TANK0 -> m.tankAmount(0);
            case D_TANK0_CAP -> m.tankCapacity(0);
            case D_TANK1 -> m.tankAmount(1);
            case D_TANK1_CAP -> m.tankCapacity(1);
            // 水泵档位(261014 T7)：0=低 1=高 2=无限
            case D_MODE -> m instanceof InfinitePumpBlockEntity p ? p.gear() : 0;
            // 环境实时镜像(§T4)：×10 定点，GUI 打开期间每 tick 自动同步
            case D_ENV_TEMP -> (int) (m.envTemp * 10);
            case D_ENV_HUM -> (int) (m.envHumidity * 10);
            case D_ENV_LIGHT -> (int) (m.envLight * 10);
            case D_ENV_MAG -> (int) (m.envMagnetic * 100);
            case D_ENV_GRAV -> (int) (m.envGravity * 100);
            case D_ENV_ATMO -> m.envAtmoIndex;
            default -> 0;
        };
    }

    /** 客户端构造(IMenuTypeExtension 工厂，buf 携带 pos)。 */
    public MachineMenu(int id, Inventory inv, RegistryFriendlyByteBuf buf) {
        this(id, inv, resolve(inv, buf.readBlockPos()));
    }

    @Nullable
    private static MachineBlockEntity resolve(Inventory inv, BlockPos pos) {
        if (inv.player.level().getBlockEntity(pos) instanceof MachineBlockEntity m) return m;
        return null;
    }

    public MachineLayout layout() {
        return layout;
    }

    private void layoutSlots(Inventory inv) {
        if (machine == null) {
            layoutPlayer(inv, layout.playerInvX(), layout.playerInvY());
            return;
        }
        MachineSpec spec = machine.spec;
        int inputs = spec.inputSlots();
        int outputs = spec.outputSlots();
        // 输入槽(语义坐标：布局模板)
        for (int i = 0; i < inputs; i++) {
            int[] xy = layout.inputSlot(i);
            addSlot(new Slot(machine, i, xy[0], xy[1]));
        }
        // 输出槽(仅取出)
        for (int i = 0; i < outputs; i++) {
            int slot = inputs + i;
            int[] xy = layout.outputSlot(i);
            addSlot(new Slot(machine, slot, xy[0], xy[1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        // 燃料槽(发电机)：复用输入槽 0 坐标 + overlay 由屏幕处理
        // 升级芯槽 ×4(§10.4)：主框右外侧列(AE2 UpgradesPanel 式, x=183)
        for (int i = 0; i < 4; i++) {
            int slot = inputs + outputs + i;
            addSlot(new Slot(machine, slot, layout.upgradeX(), layout.upgradeY(i)) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isUpgradeCore(stack);
                }
            });
        }
        layoutPlayer(inv, layout.playerInvX(), layout.playerInvY());
    }

    public static boolean isUpgradeCore(ItemStack stack) {
        return stack.getItem() == FWItems.UPGRADE_EFFICIENCY_1.get()
                || stack.getItem() == FWItems.UPGRADE_EFFICIENCY_2.get()
                || stack.getItem() == FWItems.UPGRADE_EFFICIENCY_3.get()
                || stack.getItem() == FWItems.UPGRADE_FIELD_CAPACITY.get()
                || stack.getItem() == FWItems.UPGRADE_RECTIFIER.get()
                || stack.getItem() == FWItems.UPGRADE_ISOLATION.get()
                || stack.getItem() == FWItems.UPGRADE_SUPERCONDUCTOR.get();
    }

    private void layoutPlayer(Inventory inv, int x, int y) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, x + col * 18, y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, x + col * 18, y + 58));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machineEnd = this.slots.size() - 36;
        if (index >= machineEnd) {
            // 玩家背包 → 机器槽(升级芯优先，其次输入)
            int upgStart = machineEnd - 4;
            if (isUpgradeCore(stack) && moveItemStackTo(stack, upgStart, machineEnd, false)) {
                return copy;
            }
            if (moveItemStackTo(stack, 0, upgStart, false)) {
                return copy;
            }
        } else {
            if (this.moveItemStackTo(stack, machineEnd, this.slots.size(), false)) {
                return copy;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        if (machine == null) return false;
        return stillValid(ContainerLevelAccess.create(player.level(), pos), player,
                machine.getBlockState().getBlock());
    }

    // ── 客户端读数 ──

    public int progress() {
        return data.get(D_PROGRESS);
    }

    public int energy() {
        return data.get(D_ENERGY_LO) | (data.get(D_ENERGY_HI) << 16);
    }

    public int fit() {
        return data.get(D_FIT);
    }

    public boolean active() {
        return data.get(D_ACTIVE) == 1;
    }

    public int burn() {
        return data.get(D_BURN);
    }

    /** 无限水泵当前档位(0=低 1=高 2=无限)。 */
    public int pumpGear() {
        return data.get(D_MODE);
    }

    public int tankAmount(int i) {
        return data.get(i == 0 ? D_TANK0 : D_TANK1);
    }

    public int tankCapacity(int i) {
        return data.get(i == 0 ? D_TANK0_CAP : D_TANK1_CAP);
    }

    // ── 环境实时读数(§T4, /10 还原) ──

    public float envTemp() {
        return data.get(D_ENV_TEMP) / 10f;
    }

    public float envHumidity() {
        return data.get(D_ENV_HUM) / 10f;
    }

    public float envLight() {
        return data.get(D_ENV_LIGHT) / 10f;
    }

    public float envMagnetic() {
        return data.get(D_ENV_MAG) / 100f;
    }

    public float envGravity() {
        return data.get(D_ENV_GRAV) / 100f;
    }

    public int envAtmoIndex() {
        return data.get(D_ENV_ATMO);
    }

    /** 静态工具：从 buf 反解 spec(屏幕端用)。 */
    public static MachineSpec specOf(RegistryFriendlyByteBuf buf) {
        var id = buf.readIdentifier();
        return MachineSpecs.get(id);
    }
}
