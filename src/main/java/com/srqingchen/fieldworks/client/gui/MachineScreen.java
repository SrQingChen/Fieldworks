package com.srqingchen.fieldworks.client.gui;

import com.srqingchen.fieldworks.api.atmosphere.AtmosphereRegistry;
import com.srqingchen.fieldworks.api.atmosphere.AtmosphereType;
import com.srqingchen.fieldworks.machine.IOSide;
import com.srqingchen.fieldworks.machine.MachineBlockEntity;
import com.srqingchen.fieldworks.machine.MachineLayout;
import com.srqingchen.fieldworks.machine.MachineMenu;
import com.srqingchen.fieldworks.network.FWPackets;
import com.srqingchen.fieldworks.client.gui.widget.FWWidgets;
import com.srqingchen.fieldworks.client.gui.widget.FWWidgets.EnergyBar;
import com.srqingchen.fieldworks.client.gui.widget.FWWidgets.FitBadge;
import com.srqingchen.fieldworks.client.gui.widget.FWWidgets.Gauge;
import com.srqingchen.fieldworks.client.gui.widget.FWWidgets.Progress;
import com.srqingchen.fieldworks.client.gui.widget.FWWidgets.SlotFrame;
import com.srqingchen.fieldworks.client.gui.widget.FWWidgets.Tab;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * 机器屏幕 v3(261012规划 T3/T4)：
 * <ul>
 *   <li>浅色原版底板(背包格烘入图集) + 青色点缀家族色带</li>
 *   <li>Fit 徽章绘制在主框左上角<b>外</b>侧(实时环境读数+动态 Fit)</li>
 *   <li>升级芯槽外挂主框右侧(AE2 UpgradesPanel 式)</li>
 *   <li>三层 tooltip(§15.5)：默认 / Shift 场归因 / Ctrl 完整诊断</li>
 * </ul>
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private final MachineLayout layout;
    private final List<FWWidgets.FWElement> widgets = new ArrayList<>();
    private Gauge gaugeIn, gaugeOut;
    private Progress progress;
    private EnergyBar energyBar;
    private FitBadge fitBadge;
    private boolean ioWindowOpen;

    public MachineScreen(MachineMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, menu.layout() != null ? menu.layout().windowW() : 176,
                menu.layout() != null ? menu.layout().windowH() : 166);
        this.layout = menu.layout();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = layout != null ? layout.playerInvY() - 12 : 72;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractBackground(g, mouseX, mouseY, partial);
        if (layout == null) return;
        // 1) 底板(浅色, 背包格已烘入)
        if (layout.template == MachineLayout.Template.TANK) {
            FWWidgets.blit(g, leftPos, topPos, FWWidgets.BG_TALL_X, FWWidgets.BG_TALL_Y,
                    FWWidgets.BG_TALL_W, FWWidgets.BG_TALL_H);
        } else {
            FWWidgets.blit(g, leftPos, topPos, FWWidgets.BG_STD_X, FWWidgets.BG_STD_Y,
                    FWWidgets.BG_STD_W, FWWidgets.BG_STD_H);
        }
        // 2) 升级侧板(主框右外侧, 升级槽 x=windowW+7 之下)
        if (layout.template != MachineLayout.Template.STORE) {
            FWWidgets.blit(g, leftPos + layout.windowW() + 5, topPos + 15,
                    FWWidgets.UPGPANEL_X, FWWidgets.UPGPANEL_Y, FWWidgets.UPGPANEL_W, FWWidgets.UPGPANEL_H);
        }
        // 3) 家族色带(§16.1 五线色, 浅色适配) —— 题头下 2px 细带
        int band = MachineLayout.familyBand(layout.family);
        g.fill(leftPos + 7, topPos + 16, leftPos + 169, topPos + 18, band);
        // 4) 部件(含窗外 Fit 徽章/页签/升级面板部件)
        //    注意：IO 配置浮窗不在背景层绘制——26.1.2 渲染顺序为 background→labels→物品槽，
        //    画在这里会被物品压住(261014 T3 修复)；改在 extractContents 之后绘制(全部物品之上)。
        for (FWWidgets.FWElement w : widgets) w.render(g, leftPos, topPos, partial);
    }

    @Override
    public void extractContents(GuiGraphicsExtractor g, int mouseX, int mouseY, float partial) {
        super.extractContents(g, mouseX, mouseY, partial);
        // 261014 T3：IO 配置浮窗在物品层之上绘制(super 已画完全部槽位物品，pose 已复位为绝对坐标)，
        // 仍低于光标携带物/tooltip——"配置浮窗应盖住一切物品"的正确层级。
        if (ioWindowOpen && layout != null) renderIoWindow(g, mouseX, mouseY);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        // 26.1.2：原版在调用本方法前已把 pose 平移到 (leftPos, topPos)
        // (AbstractContainerScreen.extractContents)——这里必须用相对坐标，
        // 再加 leftPos/topPos 会双倍偏移(261013 R1 修复)。
        // 浅色底 → 深色文字(原版 0x404040 惯例)
        g.text(this.font, this.title, this.titleLabelX, this.titleLabelY, 0xFF404040, false);
        g.text(this.font, this.playerInventoryTitle, this.inventoryLabelX,
                this.inventoryLabelY, 0xFF404040, false);
    }

    @Override
    protected void init() {
        super.init();
        widgets.clear();
        if (menu.machine == null || layout == null) return;
        MachineBlockEntity m = menu.machine;

        // ── 槽框(menu 槽数据驱动, 坐标-1 对齐井位) ──
        int inputs = m.spec.inputSlots();
        int outputs = m.spec.outputSlots();
        boolean gen = layout.template == MachineLayout.Template.GEN;
        for (int i = 0; i < inputs; i++) {
            Slot s = menu.slots.get(i);
            widgets.add(new SlotFrame(s.x - 1, s.y - 1, gen ? FWWidgets.SLOT_POWER : FWWidgets.SLOT_INPUT)
                    .overlay(gen ? FWWidgets.OV_BOLT : FWWidgets.OV_IN));
        }
        for (int i = 0; i < outputs; i++) {
            Slot s = menu.slots.get(inputs + i);
            widgets.add(new SlotFrame(s.x - 1, s.y - 1, FWWidgets.SLOT_OUTPUT).overlay(FWWidgets.OV_OUT));
        }
        for (int i = 0; i < 4; i++) {
            Slot s = menu.slots.get(inputs + outputs + i);
            SlotFrame f = new SlotFrame(s.x - 1, s.y - 1, FWWidgets.SLOT_UPGRADE);
            f.visible = () -> layout.template != MachineLayout.Template.STORE;
            widgets.add(f);
        }

        // ── gauge(输入/输出 tank, 流体色) ──
        int[] gi = layout.gaugeIn();
        if (m.spec.tanks() > 0) {
            gaugeIn = new Gauge(gi[0], gi[1], gi[2]);
            gaugeIn.fill = () -> menu.tankCapacity(0) > 0
                    ? (double) menu.tankAmount(0) / menu.tankCapacity(0) : 0;
            gaugeIn.contentColor = tankColor(0);
            gaugeIn.contentTooltip = () -> tankTooltip(0);
            widgets.add(gaugeIn);
        }
        int[] go = layout.gaugeOut();
        if (go != null && m.spec.tanks() > 1) {
            gaugeOut = new Gauge(go[0], go[1], go[2]);
            gaugeOut.fill = () -> menu.tankCapacity(1) > 0
                    ? (double) menu.tankAmount(1) / menu.tankCapacity(1) : 0;
            gaugeOut.contentColor = tankColor(1);
            gaugeOut.contentTooltip = () -> tankTooltip(1);
            widgets.add(gaugeOut);
        }

        // ── 进度 ──
        int[] pr = layout.progress();
        if (pr != null) {
            progress = new Progress(pr[0], pr[1], Progress.Kind.values()[pr[2]]);
            progress.progress = () -> menu.progress() / 100.0;
            widgets.add(progress);
        }

        // ── 能量竖条(井+琥珀填充；261014 T4：点击循环 FE/RF/J 显示单位) ──
        if (m.getEnergyMax() > 0) {
            int[] eb = layout.energyBar();
            energyBar = new EnergyBar(eb[0], eb[1]);
            energyBar.fill = () -> m.getEnergyMax() > 0 ? (double) menu.energy() / m.getEnergyMax() : 0;
            energyBar.tip(() -> List.of(
                    Component.translatable("tooltip.fieldworks.energy",
                            EnergyUnit.format(menu.energy()), EnergyUnit.format(m.getEnergyMax())),
                    Component.translatable("tooltip.fieldworks.energy.unit_hint")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY)));
            energyBar.onClickAction = EnergyUnit::cycle;
            widgets.add(energyBar);
        }

        // ── Fit 徽章(§T3.4/T4：主框左上角外, 动态环境读数) ──
        fitBadge = new FitBadge();
        fitBadge.x = -FWWidgets.FITBADGE_W - 6;
        fitBadge.y = 2;
        fitBadge.font = this.font;
        fitBadge.fit = () -> menu.fit() / 100.0;
        fitBadge.envLine = () -> String.format("%.1f℃ %.0f%% L%.0f",
                menu.envTemp(), menu.envHumidity(), menu.envLight());
        fitBadge.detail = this::fitDetail;
        widgets.add(fitBadge);

        // ── 页签(261014 T3：Fit 徽章高 48 → 页签下移 y=58/80 上下分布，消除与徽章的重叠) ──
        Tab ioTab = new Tab(58, FWWidgets.ICON_IO, false) {
            @Override
            public boolean mouseClicked(double mx, double my, int button, int left, int top) {
                boolean hit = isMouseOver((int) mx, (int) my, left, top);
                if (hit) {
                    ioWindowOpen = !ioWindowOpen;
                    return true;
                }
                return false;
            }
        };
        ioTab.x = -FWWidgets.TAB_W - 6;
        ioTab.activeFlag = () -> ioWindowOpen;
        ioTab.tooltip = () -> List.of(Component.translatable("gui.fieldworks.tab.io"));
        widgets.add(ioTab);

        Tab energyTab = new Tab(80, FWWidgets.ICON_ENERGY, false);
        energyTab.x = -FWWidgets.TAB_W - 6;
        energyTab.tooltip = () -> List.of(
                Component.translatable("gui.fieldworks.tab.energy"),
                Component.translatable("tooltip.fieldworks.energy",
                        EnergyUnit.format(menu.energy()), EnergyUnit.format(m.getEnergyMax())));
        widgets.add(energyTab);

        // ── 无限水泵档位页签(261014 T7：点击循环 低/高/无限) ──
        if (layout.template == MachineLayout.Template.PUMP) {
            Tab gearTab = new Tab(102, FWWidgets.ICON_TRANSFER, false) {
                @Override
                public boolean mouseClicked(double mx, double my, int button, int left, int top) {
                    if (isMouseOver((int) mx, (int) my, left, top)) {
                        int next = (menu.pumpGear() + 1) % 3;
                        ClientPacketDistributor.sendToServer(
                                new FWPackets.C2SPumpGearPayload(menu.machine.getBlockPos(), next));
                        return true;
                    }
                    return false;
                }
            };
            gearTab.x = -FWWidgets.TAB_W - 6;
            gearTab.activeFlag = () -> menu.pumpGear() > 0;
            gearTab.tooltip = () -> List.of(
                    Component.translatable("gui.fieldworks.pump.gear." + menu.pumpGear()),
                    Component.translatable("gui.fieldworks.pump.gear.hint")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
            widgets.add(gearTab);
        }

        Tab upgTab = new Tab(6, FWWidgets.ICON_UPGRADE, true);
        upgTab.x = this.imageWidth + 5 + FWWidgets.UPGPANEL_W + 2;
        upgTab.tooltip = () -> List.of(Component.translatable("gui.fieldworks.tab.upgrade"));
        widgets.add(upgTab);
    }

    private int tankColor(int idx) {
        if (menu.machine != null && idx < menu.machine.tanks().size()) {
            var fluid = menu.machine.tanks().get(idx).getFluid();
            if (!fluid.isEmpty() && fluid.getFluid() != net.minecraft.world.level.material.Fluids.EMPTY) {
                var type = fluid.getFluid().getFluidType();
                int c = com.srqingchen.fieldworks.registry.FWFluidTypes.colorOf(type);
                if (c != 0) return c;
                if (fluid.getFluid() == net.minecraft.world.level.material.Fluids.WATER
                        || fluid.getFluid() == net.minecraft.world.level.material.Fluids.FLOWING_WATER) {
                    return 0xFF3F76E4;   // 原版水色
                }
            }
        }
        return FWWidgets.COL_TEAL;
    }

    private List<Component> tankTooltip(int idx) {
        List<Component> list = new ArrayList<>();
        int amt = menu.tankAmount(idx);
        int cap = menu.tankCapacity(idx);
        if (menu.machine != null && idx < menu.machine.tanks().size()) {
            var fluid = menu.machine.tanks().get(idx).getFluid();
            if (!fluid.isEmpty()) {
                list.add(Component.literal(fluid.getHoverName().getString()));
            } else {
                list.add(Component.translatable("tooltip.fieldworks.tank.empty"));
            }
        }
        list.add(Component.translatable("tooltip.fieldworks.tank.amount", amt, cap));
        return list;
    }

    /** §15.5 三层：Shift 场归因 / Ctrl 完整诊断(数据源=菜单环境数据槽, 每 tick 动态)。 */
    private List<Component> fitDetail() {
        List<Component> list = new ArrayList<>();
        long win = GLFW.glfwGetCurrentContext();
        boolean shift = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS
                || GLFW.glfwGetKey(win, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
        boolean ctrl = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS;
        if (shift || ctrl) {
            list.add(Component.translatable("tooltip.fieldworks.fit.line2"));
            list.add(Component.literal(String.format("  %.1f℃ / %.0f%%RH / L%.0f",
                    menu.envTemp(), menu.envHumidity(), menu.envLight())));
            AtmosphereType atmo = AtmosphereRegistry.byIndex(menu.envAtmoIndex());
            if (ctrl && atmo != null) {
                list.add(Component.literal("  ⌖ ").append(atmo.displayName()));
                list.add(Component.literal(String.format("  ⌂ %.2fT / g %.2f",
                        menu.envMagnetic(), menu.envGravity())));
            }
        }
        return list;
    }

    // ═══════════ 侧面配置浮窗(立方体展开图) ═══════════

    private static final Direction[] FACES = {Direction.UP, Direction.DOWN, Direction.NORTH,
            Direction.SOUTH, Direction.WEST, Direction.EAST};
    private static final int[][] FACE_POS = {
            {68, 34},   // UP
            {68, 74},   // DOWN
            {68, 52},   // NORTH(front)
            {96, 74},   // SOUTH(back)
            {40, 52},   // WEST(left)
            {96, 52}    // EAST(right)
    };

    private void renderIoWindow(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int wx = leftPos + imageWidth / 2 - 78;
        int wy = topPos + 15;
        FWWidgets.blit(g, wx, wy, FWWidgets.SIDEWIN_X, FWWidgets.SIDEWIN_Y, FWWidgets.SIDEWIN_W, FWWidgets.SIDEWIN_H);
        g.text(this.font, Component.translatable("gui.fieldworks.io.title"), wx + 8, wy + 5, FWWidgets.COL_TEXT);
        g.text(this.font, Component.translatable("gui.fieldworks.io.hint"), wx + 8, wy + 118, FWWidgets.COL_MUTED);
        for (int i = 0; i < FACES.length; i++) {
            Direction face = FACES[i];
            int fx = wx + FACE_POS[i][0] - 11;
            int fy = wy + FACE_POS[i][1] - 11;
            IOSide side = menu.machine.ioOf(face);
            FWWidgets.blit(g, fx, fy, FWWidgets.FACEBTN_X + i * FWWidgets.FACEBTN_STEP, FWWidgets.FACEBTN_Y,
                    FWWidgets.FACEBTN_S, FWWidgets.FACEBTN_S);
            FWWidgets.blitTint(g, fx, fy, FWWidgets.CFGOV_X + side.atlasOffset(), FWWidgets.CFGOV_Y,
                    FWWidgets.CFGOV_S, FWWidgets.CFGOV_S, 0xFFFFFFFF);
            String label = face.getName().substring(0, 1).toUpperCase() + face.getName().substring(1);
            g.text(this.font, label, fx + 3, fy + 7, FWWidgets.COL_TEXT);
            if (mouseX >= fx && mouseX < fx + 22 && mouseY >= fy && mouseY < fy + 22) {
                g.fill(fx, fy, fx + 22, fy + 22, 0x40FFFFFF);
            }
        }
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        int button = event.button();
        // 1) IO 浮窗
        if (ioWindowOpen) {
            int wx = leftPos + imageWidth / 2 - 78;
            int wy = topPos + 15;
            for (int i = 0; i < FACES.length; i++) {
                int fx = wx + FACE_POS[i][0] - 11;
                int fy = wy + FACE_POS[i][1] - 11;
                if (mouseX >= fx && mouseX < fx + 22 && mouseY >= fy && mouseY < fy + 22) {
                    long win = GLFW.glfwGetCurrentContext();
                    boolean shift = GLFW.glfwGetKey(win, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;
                    int op = shift ? 2 : button == 1 ? 1 : 0;
                    ClientPacketDistributor.sendToServer(
                            new FWPackets.C2SIoConfigPayload(menu.machine.getBlockPos(), FACES[i], op));
                    menu.machine.setIoLocal(FACES[i],
                            op == 2 ? IOSide.AUTO : op == 1 ? menu.machine.ioOf(FACES[i]).previous()
                                    : menu.machine.ioOf(FACES[i]).next());
                    return true;
                }
            }
            if (mouseX >= wx && mouseX < wx + 156 && mouseY >= wy && mouseY < wy + 135) {
                return true;
            }
        }
        // 2) 页签与部件
        for (int i = widgets.size() - 1; i >= 0; i--) {
            if (widgets.get(i).mouseClicked(mouseX, mouseY, button, leftPos, topPos)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // ── 窗外部件区域(JEI 排除区等外部消费者) ──

    /** Fit 徽章(主框左上角外)屏幕区域。 */
    public net.minecraft.client.renderer.Rect2i fitBadgeArea() {
        return new net.minecraft.client.renderer.Rect2i(leftPos - FWWidgets.FITBADGE_W - 6,
                topPos + 2, FWWidgets.FITBADGE_W, FWWidgets.FITBADGE_H);
    }

    /** 升级侧板(主框右外侧)屏幕区域；无则 null。 */
    public net.minecraft.client.renderer.Rect2i upgradePanelArea() {
        if (layout == null || layout.template == MachineLayout.Template.STORE) return null;
        return new net.minecraft.client.renderer.Rect2i(leftPos + imageWidth + 5,
                topPos + 15, FWWidgets.UPGPANEL_W, FWWidgets.UPGPANEL_H);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        if (ioWindowOpen) {
            int wx = leftPos + imageWidth / 2 - 78;
            int wy = topPos + 15;
            for (int i = 0; i < FACES.length; i++) {
                int fx = wx + FACE_POS[i][0] - 11;
                int fy = wy + FACE_POS[i][1] - 11;
                if (mouseX >= fx && mouseX < fx + 22 && mouseY >= fy && mouseY < fy + 22) {
                    IOSide side = menu.machine.ioOf(FACES[i]);
                    List<Component> lines = new ArrayList<>();
                    lines.add(Component.translatable("gui.fieldworks.io.face." + FACES[i].getName()));
                    lines.add(Component.translatable("gui.fieldworks.io.mode." + side.name().toLowerCase()));
                    var seqs = new ArrayList<net.minecraft.util.FormattedCharSequence>();
                    for (Component c : lines) seqs.add(c.getVisualOrderText());
                    g.setTooltipForNextFrame(seqs, mouseX, mouseY);
                    return;
                }
            }
            return;
        }
        for (int i = widgets.size() - 1; i >= 0; i--) {
            FWWidgets.FWElement w = widgets.get(i);
            if (w.tooltip != null && w.isMouseOver(mouseX, mouseY, leftPos, topPos)) {
                List<Component> lines = w.tooltip.get();
                if (!lines.isEmpty()) {
                    var seqs = new ArrayList<net.minecraft.util.FormattedCharSequence>();
                    for (Component c : lines) seqs.add(c.getVisualOrderText());
                    g.setTooltipForNextFrame(seqs, mouseX, mouseY);
                }
                return;
            }
        }
    }
}
