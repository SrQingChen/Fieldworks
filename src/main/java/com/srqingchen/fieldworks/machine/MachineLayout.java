package com.srqingchen.fieldworks.machine;

/**
 * 机器 GUI 布局 v3(261012规划 T3.3：浅色底板 + 消除全部坐标重叠)。
 * 坐标系：窗口 176×H；机器功能区 y∈[17,83]；背包 y=84(STD)/108(TALL)。
 * 升级芯槽列移到主框右外侧(x=183，AE2 UpgradesPanel 同做法)。
 * Fit 徽章在主框左上角外(屏幕层绘制, 不占布局)。
 */
public final class MachineLayout {
    public enum Template { WIDE, TANK, GEN, STORE, COMPACT, PART, PUMP }

    public final Template template;
    public final MachineSpec.Family family;

    private MachineLayout(Template t, MachineSpec.Family f) {
        this.template = t;
        this.family = f;
    }

    public static MachineLayout of(MachineSpec spec) {
        Template t = switch (spec.mode()) {
            case GENERATOR, FLUID_BURNER -> Template.GEN;
            case STORAGE -> Template.STORE;
            case PUMP -> Template.PUMP;
            default -> spec.id().getPath().equals("part_bench") ? Template.PART
                    : switch (spec.tanks()) {
                case 0 -> spec.inputSlots() >= 2 ? Template.WIDE : Template.COMPACT;
                default -> Template.TANK;
            };
        };
        return new MachineLayout(t, spec.family());
    }

    // ── 窗口与玩家背包 ──

    public int windowW() {
        return 176;
    }

    public int windowH() {
        return template == Template.TANK ? 190 : 166;
    }

    public int playerInvX() {
        return 8;
    }

    public int playerInvY() {
        return template == Template.TANK ? 108 : 84;
    }

    // ── 槽位坐标(全部与部件/背包无重叠：机器区 y 17..79) ──

    public int[] inputSlot(int i) {
        return switch (template) {
            // 部件台 3×3 网格(261014 T5)：x=30..84, y=26..80
            case PART -> new int[]{30 + (i % 3) * 18, 26 + (i / 3) * 18};
            case WIDE -> new int[]{44 + i * 18, 36};
            case TANK -> new int[]{44, 36};          // 电解类 0 物品槽，仅兜底
            case GEN -> new int[]{56, 30};           // 燃料槽
            case STORE -> new int[]{44, 40};         // 储罐 0 物品槽，仅兜底
            case PUMP -> new int[]{56, 30};          // 0 物品槽，仅兜底
            default -> new int[]{44 + i * 18, 36};   // COMPACT
        };
    }

    public int[] outputSlot(int i) {
        return switch (template) {
            case PART -> new int[]{116, 44};
            case WIDE, TANK -> new int[]{116 + i * 18, 36};
            case GEN -> new int[]{116, 30};
            case STORE -> new int[]{116, 40};
            case PUMP -> new int[]{116, 30};         // 0 物品槽，仅兜底
            default -> new int[]{116 + i * 18, 36};
        };
    }

    /** 升级芯列：主框右外侧(AE2 式外挂面板, UPGPANEL 22×90 贴其下)。 */
    public int upgradeX() {
        return windowW() + 7;
    }

    public int upgradeY(int i) {
        return 17 + i * 18;
    }

    // ── 部件坐标(屏幕端, 相对窗口) ──

    /** 主 gauge(输入 tank)：[x, y, size]。 */
    public int[] gaugeIn() {
        return switch (template) {
            case STORE -> new int[]{79, 18, FW_SIZE_STD};
            case GEN -> new int[]{24, 18, FW_SIZE_STD};
            case TANK -> new int[]{24, 20, FW_SIZE_STD};
            case PUMP -> new int[]{79, 22, FW_SIZE_STD};   // 水泵储水罐居中
            default -> new int[]{24, 22, FW_SIZE_STD};
        };
    }

    /** 输出 gauge(第二 tank)；无则 null。 */
    public int[] gaugeOut() {
        return template == Template.TANK ? new int[]{134, 20, FW_SIZE_STD} : null;
    }

    /** 进度部件 [x, y, kindIndex(0=ARROW 1=BI 2=FLAME 3=DOWN)]；STORE/PUMP 无进度。 */
    public int[] progress() {
        return switch (template) {
            case WIDE -> new int[]{82, 36, 0};
            case TANK -> new int[]{78, 44, 1};
            case GEN -> new int[]{56, 50, 2};
            case PART -> new int[]{90, 43, 0};
            case STORE, PUMP -> null;
            default -> new int[]{82, 36, 0};
        };
    }

    /** 能量条(竖 8×56)。 */
    public int[] energyBar() {
        return new int[]{8, 22};
    }

    public static final int FW_SIZE_STD = 0, FW_SIZE_SMALL = 1, FW_SIZE_TALL = 2;

    /** 家族色(浅色底板适配的柔和五线色)。 */
    public static int familyBand(MachineSpec.Family f) {
        return switch (f) {
            case ENERGY -> 0xFFD98E2B;
            case MATERIAL -> 0xFF7A8691;
            case FIELD -> 0xFF1E9E96;
            case ENVIRONMENT -> 0xFF3FA35C;
            case CONVENIENCE -> 0xFF8E6FC9;
        };
    }
}
