package com.srqingchen.fieldworks.client.gui.widget;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

/**
 * 场域工艺 GUI 部件库 v3（261012规划 T3：浅色原版底 + 青色点缀）。
 * 统一图集 {@link #ATLAS}(512×512，gen_atlas_v3.py 生成；
 * 坐标常量与 _texwork/atlas_coords.java.frag 严格镜像，QA 脚本比对)。
 */
public final class FWWidgets {
    public static final Identifier ATLAS = Identifier.fromNamespaceAndPath(Fieldworks.MODID, "textures/guis/atlas.png");
    public static final RenderPipeline PIPELINE = RenderPipelines.GUI_TEXTURED;

    // ── 图集坐标表(gen_atlas_v3.py 为唯一事实源) ──
    public static final int BG_STD_X = 0, BG_STD_Y = 0, BG_STD_W = 176, BG_STD_H = 166;
    public static final int BG_TALL_X = 176, BG_TALL_Y = 0, BG_TALL_W = 176, BG_TALL_H = 190;
    public static final int JEI_BG_X = 356, JEI_BG_Y = 0, JEI_BG_W = 146, JEI_BG_H = 64;
    public static final int JEI_BG_TALL_X = 356, JEI_BG_TALL_Y = 64, JEI_BG_TALL_W = 146, JEI_BG_TALL_H = 88;
    public static final int SLOT_NORMAL = 0, SLOT_INPUT = 19, SLOT_OUTPUT = 38, SLOT_POWER = 57,
            SLOT_UPGRADE = 76, SLOT_TANKPORT = 95, SLOT_Y = 166, SLOT_S = 18;
    public static final int OV_IN = 114, OV_OUT = 133, OV_BOLT = 152, OV_Y = 166, OV_S = 18;
    public static final int PROG_ARROW_BG = 0, PROG_ARROW_FILL = 25, PROG_BI_BG = 50, PROG_BI_FILL = 66,
            PROG_FLAME_BG = 84, PROG_FLAME_FILL = 97, PROG_DOWN_BG = 112, PROG_DOWN_FILL = 120, PROG_Y = 210;
    public static final int GAUGE_STD_X = 0, GAUGE_STD_Y = 240, GAUGE_STD_W = 18, GAUGE_STD_H = 62;
    public static final int GAUGE_SMALL_X = 20, GAUGE_SMALL_Y = 240, GAUGE_SMALL_W = 18, GAUGE_SMALL_H = 34;
    public static final int GAUGE_TALL_X = 40, GAUGE_TALL_Y = 240, GAUGE_TALL_W = 18, GAUGE_TALL_H = 100;
    public static final int ENERGY_WELL_X = 60, ENERGY_FILL_X = 70, ENERGY_Y = 240, ENERGY_W = 8, ENERGY_H = 56;
    public static final int FITBADGE_X = 82, FITBADGE_Y = 240, FITBADGE_W = 66, FITBADGE_H = 48;
    public static final int UPGPANEL_X = 150, UPGPANEL_Y = 240, UPGPANEL_W = 22, UPGPANEL_H = 90;
    public static final int TAB_L_X = 0, TAB_R_X = 28, TAB_Y = 346, TAB_W = 26, TAB_H = 18;
    public static final int ICON_UPGRADE = 0, ICON_REDSTONE = 18, ICON_IO = 36, ICON_ENERGY = 54,
            ICON_WARNING = 72, ICON_INFO = 90, ICON_TRANSFER = 108, ICON_CLOSE = 126, ICON_FLAME = 144,
            ICON_FIELD = 162, ICON_BOOK = 180, ICON_BLUEPRINT = 198, ICON_Y = 366, ICON_S = 18;
    public static final int SIDEWIN_X = 352, SIDEWIN_Y = 166, SIDEWIN_W = 156, SIDEWIN_H = 135;
    public static final int FACEBTN_X = 352, FACEBTN_Y = 306, FACEBTN_S = 22, FACEBTN_STEP = 23;
    public static final int CFGOV_X = 352, CFGOV_Y = 330, CFGOV_S = 22, CFGOV_STEP = 23;
    public static final int CFG_NONE = 0, CFG_INPUT = 23, CFG_OUTPUT = 46, CFG_BOTH = 69;

    // ── 调色板(浅色 §T3.2) ──
    public static final int COL_ACCENT = 0xFFE8A33D, COL_TEAL = 0xFF1E9E96, COL_TEAL_DK = 0xFF0C6B66,
            COL_TEAL_LT = 0xFF7FD8D0;
    public static final int COL_TEXT = 0xFF3F3F46, COL_MUTED = 0xFF6E7B83;
    public static final int COL_GOOD = 0xFF2FA84F, COL_WARN = 0xFFD9A728, COL_BAD = 0xFFC0392B;

    private FWWidgets() {}

    public static void blit(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h) {
        g.blit(PIPELINE, ATLAS, x, y, u, v, w, h, 512, 512, -1);
    }

    public static void blitTint(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h, int argb) {
        g.blit(PIPELINE, ATLAS, x, y, u, v, w, h, 512, 512, argb);
    }

    /** 竖向裁剪绘制(自底部起 fillRatio)。 */
    public static void blitBottomClipped(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h, double ratio) {
        int sh = Mth.clamp((int) Math.round(h * ratio), 0, h);
        if (sh <= 0) return;
        g.blit(PIPELINE, ATLAS, x, y + h - sh, u, v + h - sh, w, sh, 512, 512, -1);
    }

    /** 横向裁剪绘制(进度条用)。 */
    public static void blitLeftClipped(GuiGraphicsExtractor g, int x, int y, int u, int v, int w, int h, double ratio) {
        int sw = Mth.clamp((int) Math.round(w * ratio), 0, w);
        if (sw <= 0) return;
        g.blit(PIPELINE, ATLAS, x, y, u, v, sw, h, 512, 512, -1);
    }

    // ═══════════ 部件基类(Mek GuiElement 式) ═══════════

    public abstract static class FWElement {
        public int x, y, w, h;
        /** 相对主窗口可为负(窗外绘制, AE2 VerticalButtonBar 同做法)。 */
        public BooleanSupplier visible = () -> true;
        protected final List<FWElement> children = new ArrayList<>();
        public Supplier<List<Component>> tooltip;

        protected FWElement(int x, int y, int w, int h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        public final boolean isMouseOver(int mx, int my, int left, int top) {
            return visible.getAsBoolean() && mx >= left + x && mx < left + x + w && my >= top + y && my < top + y + h;
        }

        public void render(GuiGraphicsExtractor g, int left, int top, float partial) {
            if (!visible.getAsBoolean()) return;
            draw(g, left + x, top + y, partial);
        }

        protected abstract void draw(GuiGraphicsExtractor g, int ax, int ay, float partial);

        public boolean mouseClicked(double mx, double my, int button, int left, int top) {
            if (!isMouseOver((int) mx, (int) my, left, top)) return false;
            return onClick(button);
        }

        protected boolean onClick(int button) {
            return false;
        }
    }

    // ═══════════ 槽位框(18×18，浅色井 + 语义角标) ═══════════

    public static class SlotFrame extends FWElement {
        public int frameU = SLOT_NORMAL;
        public int overlayU = -1;
        public BooleanSupplier hovered = () -> false;

        public SlotFrame(int x, int y, int frameU) {
            super(x, y, SLOT_S, SLOT_S);
            this.frameU = frameU;
        }

        public SlotFrame overlay(int u) {
            this.overlayU = u;
            return this;
        }

        @Override
        protected void draw(GuiGraphicsExtractor g, int ax, int ay, float partial) {
            blit(g, ax, ay, frameU, SLOT_Y, SLOT_S, SLOT_S);
            if (overlayU >= 0) blit(g, ax, ay, overlayU, OV_Y, OV_S, OV_S);
            if (hovered.getAsBoolean()) {
                g.fill(ax, ay, ax + SLOT_S, ay + SLOT_S, 0x50FFFFFF);
            }
        }
    }

    // ═══════════ 计量槽(竖向 gauge：运行时流体色 + 浅色框/玻璃光) ═══════════

    public static class Gauge extends FWElement {
        public static final int SIZE_STD = 0, SIZE_SMALL = 1, SIZE_TALL = 2;
        private final int gu, gv;
        public DoubleSupplier fill = () -> 0;
        public int contentColor = COL_TEAL;
        public Supplier<List<Component>> contentTooltip;

        public Gauge(int x, int y, int size) {
            super(x, y, switch (size) { case SIZE_SMALL -> GAUGE_SMALL_W; case SIZE_TALL -> GAUGE_TALL_W;
                default -> GAUGE_STD_W; }, switch (size) { case SIZE_SMALL -> GAUGE_SMALL_H;
                case SIZE_TALL -> GAUGE_TALL_H; default -> GAUGE_STD_H; });
            int[] us = {GAUGE_STD_X, GAUGE_SMALL_X, GAUGE_TALL_X};
            int[] vs = {GAUGE_STD_Y, GAUGE_SMALL_Y, GAUGE_TALL_Y};
            this.gu = us[size];
            this.gv = vs[size];
            this.tooltip = () -> contentTooltip != null ? contentTooltip.get() : List.of();
        }

        @Override
        protected void draw(GuiGraphicsExtractor g, int ax, int ay, float partial) {
            double f = Mth.clamp(fill.getAsDouble(), 0, 1);
            int innerH = h - 4;
            int sh = (int) Math.round(innerH * f);
            if (sh > 0) {
                // 流体色块(半透明) + 顶部亮线(液面感)
                g.fill(ax + 2, ay + 2 + innerH - sh, ax + w - 2, ay + 2 + innerH, (contentColor & 0x00FFFFFF) | 0xE6000000);
                g.fill(ax + 2, ay + 2 + innerH - sh, ax + w - 2, ay + 3 + innerH - sh, 0x60FFFFFF);
            }
            blit(g, ax, ay, gu, gv, w, h);
        }
    }

    // ═══════════ 进度(箭头/双向/火焰/下落，青色填充) ═══════════

    public static class Progress extends FWElement {
        public enum Kind { ARROW(24, 16, PROG_ARROW_BG, PROG_ARROW_FILL), BI(16, 6, PROG_BI_BG, PROG_BI_FILL),
            FLAME(13, 13, PROG_FLAME_BG, PROG_FLAME_FILL), DOWN(8, 20, PROG_DOWN_BG, PROG_DOWN_FILL);
            final int w, h, bgU, fillU;
            Kind(int w, int h, int bgU, int fillU) { this.w = w; this.h = h; this.bgU = bgU; this.fillU = fillU; }
        }
        public final Kind kind;
        public DoubleSupplier progress = () -> 0;

        public Progress(int x, int y, Kind kind) {
            super(x, y, kind.w, kind.h);
            this.kind = kind;
        }

        @Override
        protected void draw(GuiGraphicsExtractor g, int ax, int ay, float partial) {
            blit(g, ax, ay, kind.bgU, PROG_Y, w, h);
            double p = Mth.clamp(progress.getAsDouble(), 0, 1);
            if (kind == Kind.BI) {
                blitLeftClipped(g, ax, ay, kind.fillU + 8, PROG_Y + 2, 8, h - 4, p);
                int lw = (int) Math.round(8 * p);
                if (lw > 0) {
                    g.blit(PIPELINE, ATLAS, ax + 16 - lw, ay + 2, kind.fillU, PROG_Y + 2, lw, h - 4, 512, 512, -1);
                }
            } else {
                blitLeftClipped(g, ax, ay, kind.fillU, PROG_Y, w, h, p);
            }
        }
    }

    // ═══════════ 能量竖条(井 + 琥珀填充；点击切换显示单位) ═══════════

    public static class EnergyBar extends FWElement {
        public DoubleSupplier fill = () -> 0;
        /** 261014 T4：点击回调(切换单位等)。 */
        public Runnable onClickAction;

        public EnergyBar(int x, int y) {
            super(x, y, ENERGY_W, ENERGY_H);
            tooltip = () -> List.of();
        }

        public EnergyBar tip(Supplier<List<Component>> t) {
            tooltip = t;
            return this;
        }

        @Override
        protected void draw(GuiGraphicsExtractor g, int ax, int ay, float partial) {
            blit(g, ax, ay, ENERGY_WELL_X, ENERGY_Y, w, h);
            double f = Mth.clamp(fill.getAsDouble(), 0, 1);
            blitBottomClipped(g, ax, ay, ENERGY_FILL_X, ENERGY_Y, w, h, f);
        }

        @Override
        protected boolean onClick(int button) {
            if (onClickAction != null) {
                onClickAction.run();
                return true;
            }
            return false;
        }
    }

    // ═══════════ 场谱 Fit 徽章(主框外左上角，§T3.4) ═══════════

    public static class FitBadge extends FWElement {
        public DoubleSupplier fit = () -> 1;
        /** 徽章下方的实时环境行(如 "12.3℃ 45% L14")。 */
        public Supplier<String> envLine = () -> "";
        /** 大气名(可空)。 */
        public Supplier<Component> atmosphere = () -> null;
        public Supplier<List<Component>> detail = List::of;
        public Font font;

        public FitBadge() {
            super(0, 0, FITBADGE_W, FITBADGE_H);
            tooltip = () -> {
                int pct = (int) Math.round(Mth.clamp(fit.getAsDouble(), 0, 1) * 100);
                List<Component> base = new ArrayList<>();
                base.add(Component.translatable("tooltip.fieldworks.fit.line1", pct));
                base.addAll(detail.get());
                return base;
            };
        }

        @Override
        protected void draw(GuiGraphicsExtractor g, int ax, int ay, float partial) {
            blit(g, ax, ay, FITBADGE_X, FITBADGE_Y, w, h);
            if (font == null) return;
            double f = Mth.clamp(fit.getAsDouble(), 0, 1);
            int pct = (int) Math.round(f * 100);
            int col = f > 0.8 ? COL_GOOD : f >= 0.6 ? COL_WARN : COL_BAD;
            g.text(font, Component.translatable("gui.fieldworks.fit.title"), ax + 5, ay + 9, COL_TEXT);
            g.text(font, pct + "%", ax + 45, ay + 9, col);
            // 分级色条(条槽 y=34..40 内缩)
            int bw = (int) Math.round(54 * f);
            g.fill(ax + 6, ay + 36, ax + 6 + bw, ay + 39, col);
            g.fill(ax + 6, ay + 36, ax + 6 + bw, ay + 37, 0x40000000 | (col & 0xFFFFFF));
            String env = envLine.get();
            if (env != null && !env.isEmpty()) {
                g.text(font, env, ax + 5, ay + 20, COL_MUTED);
            }
        }
    }

    // ═══════════ 页签(26×18，左/右两沿) ═══════════

    public static class Tab extends FWElement {
        public final int iconU;
        public boolean right;
        public int tint = -1;
        public Runnable action;
        public BooleanSupplier activeFlag = () -> false;

        public Tab(int y, int iconU, boolean right) {
            super(0, y, TAB_W, TAB_H);
            this.iconU = iconU;
            this.right = right;
        }

        @Override
        protected void draw(GuiGraphicsExtractor g, int ax, int ay, float partial) {
            int tintC = activeFlag.getAsBoolean() ? 0xFFBDEBE7 : tint;
            blitTint(g, ax, ay, right ? TAB_R_X : TAB_L_X, TAB_Y, TAB_W, TAB_H, tintC);
            blitTint(g, ax + 5, ay + 1, iconU, ICON_Y, ICON_S, ICON_S,
                    activeFlag.getAsBoolean() ? 0xFFFFFFFF : 0xFFC8CFD4);
        }

        @Override
        protected boolean onClick(int button) {
            if (action != null) {
                action.run();
                return true;
            }
            return false;
        }
    }
}
