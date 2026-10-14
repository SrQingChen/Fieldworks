#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GUI 图集生成器 v3 —— 261012开发规划 T3：
浅色原版底(#C6C6C8) + 青色(#1E9E96) 点缀（对标 mekUI.png / 原版 crafting_table 风格）。
产物：
  1. assets/fieldworks/textures/guis/atlas.png (512×512)
  2. assets/fieldworks/textures/guis/machine.png (176×166 兼容样式层)
  3. _texwork/atlas_coords.java.frag（FWWidgets 常量镜像，QA 比对用）
所有矩形经 disjoint 断言，杜绝部件重叠。
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "fieldworks")

# ── 浅色调色板（261012 规划 §T3.2）──
PANEL     = (198, 198, 200)   # 底板主体 #C6C6C8
PANEL_LT  = (232, 232, 234)   # 内亮
PANEL_HI  = (255, 255, 255)   # 顶棱高光
PANEL_LO  = (154, 154, 158)   # 内暗
EDGE      = (79, 84, 88)      # 外描边 #4F5458
WELL      = (139, 139, 139)   # 槽井 #8B8B8B
WELL_DK   = (55, 55, 55)      # 槽井上/左暗棱
TEAL      = (30, 158, 150)    # 主青 #1E9E96
TEAL_DK   = (12, 107, 102)
TEAL_LT   = (127, 216, 208)
AMBER     = (232, 163, 61)    # 能量(唯一保留的旧强调色)
GREEN     = (47, 168, 79)
YELLOW    = (217, 167, 40)
RED       = (192, 57, 43)
PURPLE    = (142, 111, 201)
TEXT_DK   = (63, 63, 70)
MUTED     = (110, 123, 130)

# ── 图集坐标表（唯一事实源；同时导出给 FWWidgets）──
R = {}  # name -> (x, y, w, h)
def put(name, x, y, w, h):
    R[name] = (x, y, w, h)

put("BG_STD",        0,   0, 176, 166)
put("BG_TALL",     176,   0, 176, 190)
put("JEI_BG",      356,   0, 146,  64)
put("JEI_BG_TALL", 356,  64, 146,  88)   # 261013 T3: 流体族类目底板(双计量槽布局)
put("SLOT_NORMAL",   0, 166,  18,  18)
put("SLOT_INPUT",   19, 166,  18,  18)
put("SLOT_OUTPUT",  38, 166,  18,  18)
put("SLOT_POWER",   57, 166,  18,  18)
put("SLOT_UPGRADE", 76, 166,  18,  18)
put("SLOT_TANKPORT",95, 166,  18,  18)
put("OV_IN",       114, 166,  18,  18)
put("OV_OUT",      133, 166,  18,  18)
put("OV_BOLT",     152, 166,  18,  18)
put("PROG_ARROW_BG",  0, 210,  24,  16)
put("PROG_ARROW_FILL",25, 210,  24,  16)
put("PROG_BI_BG",    50, 210,  16,   6)
put("PROG_BI_FILL",  66, 210,  16,   6)
put("PROG_FLAME_BG", 84, 210,  13,  13)
put("PROG_FLAME_FILL",97,210,  13,  13)
put("PROG_DOWN_BG", 112, 210,   8,  20)
put("PROG_DOWN_FILL",120,210,   8,  20)
put("GAUGE_STD",      0, 240,  18,  62)
put("GAUGE_SMALL",   20, 240,  18,  34)
put("GAUGE_TALL",    40, 240,  18, 100)
put("ENERGY_WELL",   60, 240,   8,  56)
put("ENERGY_FILL",   70, 240,   8,  56)
put("FITBADGE",      82, 240,  66,  48)
put("UPGPANEL",     150, 240,  22,  90)
put("TAB_L",          0, 346,  26,  18)
put("TAB_R",         28, 346,  26,  18)
put("ICON_UPGRADE",   0, 366,  18,  18)
put("ICON_REDSTONE", 18, 366,  18,  18)
put("ICON_IO",       36, 366,  18,  18)
put("ICON_ENERGY",   54, 366,  18,  18)
put("ICON_WARNING",  72, 366,  18,  18)
put("ICON_INFO",     90, 366,  18,  18)
put("ICON_TRANSFER",108, 366,  18,  18)
put("ICON_CLOSE",   126, 366,  18,  18)
put("ICON_FLAME",   144, 366,  18,  18)
put("ICON_FIELD",   162, 366,  18,  18)
put("ICON_BOOK",    180, 366,  18,  18)
put("ICON_BLUEPRINT",198,366,  18,  18)
put("SIDEWIN",     352, 166, 156, 135)
put("FACEBTN",     352, 306,  22,  22)   # 6 连排 x+=23
put("CFGOV",       352, 330,  22,  22)   # 4 连排 x+=23

def assert_disjoint():
    items = list(R.items())
    for i in range(len(items)):
        for j in range(i + 1, len(items)):
            n1, (x1, y1, w1, h1) = items[i]
            n2, (x2, y2, w2, h2) = items[j]
            # 连排元素(FACEBTN/CFGOV/SLOT/ICON)本身允许平铺不重叠；矩形相交检测对单一 cell 生效
            ox = max(0, min(x1 + w1, x2 + w2) - max(x1, x2))
            oy = max(0, min(y1 + h1, y2 + h2) - max(y1, y2))
            if ox > 0 and oy > 0:
                raise SystemExit(f"图集矩形重叠: {n1}{R[n1]} × {n2}{R[n2]}")
    for n, (x, y, w, h) in R.items():
        assert 0 <= x and x + w <= 512 and 0 <= y and y + h <= 512, f"越界 {n}"

# ── 基础绘制 ──
def panel(d, x, y, w, h, base=PANEL):
    """原版式浅色面板：外1px深边 + 左上2px高光 + 右下2px内影。"""
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=base + (255,))
    d.rectangle([x, y, x + w - 1, y + h - 1], outline=EDGE + (255,))
    d.line([x + 1, y + 1, x + w - 2, y + 1], fill=PANEL_HI + (255,))
    d.line([x + 1, y + 1, x + 1, y + h - 2], fill=PANEL_HI + (255,))
    d.line([x + 1, y + 2, x + w - 2, y + 2], fill=PANEL_LT + (255,))
    d.line([x + 2, y + 2, x + 2, y + h - 3], fill=PANEL_LT + (255,))
    d.line([x + 2, y + h - 2, x + w - 2, y + h - 2], fill=PANEL_LO + (255,))
    d.line([x + w - 2, y + 2, x + w - 2, y + h - 2], fill=PANEL_LO + (255,))
    d.line([x + 3, y + h - 3, x + w - 3, y + h - 3], fill=(176, 176, 180) + (255,))
    d.line([x + w - 3, y + 3, x + w - 3, y + h - 3], fill=(176, 176, 180) + (255,))

def well(d, x, y):
    """18×18 原版槽井：暗上左 + 亮下右(内凹)。"""
    d.rectangle([x, y, x + 17, y + 17], fill=WELL + (255,))
    d.line([x, y, x + 17, y], fill=WELL_DK + (255,))
    d.line([x, y, x, y + 17], fill=WELL_DK + (255,))
    d.line([x, y + 17, x + 17, y + 17], fill=PANEL_HI + (255,))
    d.line([x + 17, y, x + 17, y + 17], fill=PANEL_HI + (255,))
    d.point([x, y + 17], fill=WELL_DK + (255,))
    d.point([x + 17, y], fill=WELL_DK + (255,))

def brackets(d, x, y, col, inset=1, len_=3):
    """四角 L 形角标（语义槽）。"""
    for cx, cy, dx, dy in [(x+inset, y+inset, 1, 1), (x+17-inset, y+inset, -1, 1),
                           (x+inset, y+17-inset, 1, -1), (x+17-inset, y+17-inset, -1, -1)]:
        d.line([cx, cy, cx + dx*(len_-1), cy], fill=col + (255,))
        d.line([cx, cy, cx, cy + dy*(len_-1)], fill=col + (255,))

def build_atlas():
    assert_disjoint()
    im = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)

    # ── 底板：背包格烘入（vanilla crafting_table 同做法）──
    for name, (x, y, w, h) in [("BG_STD", R["BG_STD"]), ("BG_TALL", R["BG_TALL"])]:
        panel(d, x, y, w, h)
        inv_y = y + 84 if name == "BG_STD" else y + 108
        # 机器区微底纹：顶带下方 1px 分隔
        d.line([x + 7, y + 15, x + 168, y + 15], fill=PANEL_LO + (160,))
        for row in range(3):
            for col in range(9):
                well(d, x + 8 + col * 18 - 1, inv_y + row * 18 - 1)
        for col in range(9):
            well(d, x + 8 + col * 18 - 1, inv_y + 58 - 1)
        # 背包标签底衬不画(文字运行时绘制)

    # ── 槽位格 ──
    for name, col in [("SLOT_NORMAL", None), ("SLOT_INPUT", TEAL), ("SLOT_OUTPUT", GREEN),
                      ("SLOT_POWER", AMBER), ("SLOT_UPGRADE", PURPLE), ("SLOT_TANKPORT", TEAL_DK)]:
        x, y, w, h = R[name]
        well(d, x, y)
        if col:
            brackets(d, x, y, col)
    # 槽位 overlay（16×16 内容居中）
    for name, col in [("OV_IN", TEAL), ("OV_OUT", GREEN), ("OV_BOLT", AMBER)]:
        x, y, _, _ = R[name]
        cx, cy = x + 9, y + 9
        if name == "OV_IN":    # 入角标:左向三角
            d.polygon([(cx+4, cy-5), (cx-4, cy), (cx+4, cy+5)], fill=col + (230,))
        elif name == "OV_OUT":  # 出角标:右向三角
            d.polygon([(cx-4, cy-5), (cx+4, cy), (cx-4, cy+5)], fill=col + (230,))
        else:                   # 闪电
            d.polygon([(cx-1, cy-6), (cx-5, cy+1), (cx-1, cy+1), (cx-2, cy+6), (cx+4, cy-2), (cx, cy-2)],
                      fill=col + (235,))

    # ── 进度部件 ──
    x, y = R["PROG_ARROW_BG"][0], R["PROG_ARROW_BG"][1]
    d.rounded_rectangle([x, y+3, x+23, y+12], 4, fill=PANEL_LO + (255,), outline=EDGE + (255,))
    d.polygon([(x+16, y+5), (x+23, y+8), (x+16, y+11)], fill=(120,120,124)+(255,))
    fx, fy = R["PROG_ARROW_FILL"][:2]
    d.rounded_rectangle([fx, fy+3, fx+23, fy+12], 4, fill=TEAL + (255,), outline=TEAL_DK + (255,))
    d.polygon([(fx+16, fy+5), (fx+23, fy+8), (fx+16, fy+11)], fill=TEAL_LT + (255,))
    # 双向
    bx, by = R["PROG_BI_BG"][:2]
    d.rounded_rectangle([bx, by, bx+15, by+5], 2, fill=PANEL_LO + (255,), outline=EDGE + (255,))
    d.polygon([(bx+2, by+1), (bx+6, by+3), (bx+2, by+5)], fill=(120,120,124)+(255,))
    d.polygon([(bx+13, by+1), (bx+9, by+3), (bx+13, by+5)], fill=(120,120,124)+(255,))
    bx, by = R["PROG_BI_FILL"][:2]
    d.rounded_rectangle([bx, by, bx+15, by+5], 2, fill=TEAL + (255,))
    d.polygon([(bx+2, by+1), (bx+6, by+3), (bx+2, by+5)], fill=TEAL_LT + (255,))
    d.polygon([(bx+13, by+1), (bx+9, by+3), (bx+13, by+5)], fill=TEAL_LT + (255,))
    # 火焰
    def flame(fx, fy, hot):
        c1, c2 = (RED, AMBER) if not hot else (AMBER, YELLOW)
        d.polygon([(fx+6, fy), (fx+11, fy+5), (fx+9, fy+5), (fx+12, fy+9), (fx+7, fy+12),
                   (fx+1, fy+8), (fx+4, fy+6), (fx+2, fy+4), (fx+6, fy)], fill=c1 + (255,))
        d.polygon([(fx+6, fy+4), (fx+9, fy+8), (fx+7, fy+11), (fx+4, fy+8), (fx+6, fy+4)],
                  fill=c2 + (255,))
    flame(*R["PROG_FLAME_BG"][:2], False)
    flame(*R["PROG_FLAME_FILL"][:2], True)
    # 下落箭头
    dx0, dy0 = R["PROG_DOWN_BG"][:2]
    d.rectangle([dx0+1, dy0, dx0+6, dy0+13], fill=PANEL_LO + (255,))
    d.polygon([(dx0, dy0+13), (dx0+7, dy0+13), (dx0+3, dy0+19)], fill=PANEL_LO + (255,))
    dx0, dy0 = R["PROG_DOWN_FILL"][:2]
    d.rectangle([dx0+1, dy0, dx0+6, dy0+13], fill=TEAL + (255,))
    d.polygon([(dx0, dy0+13), (dx0+7, dy0+13), (dx0+3, dy0+19)], fill=TEAL + (255,))

    # ── 计量槽（透明中心 + 浅色框 + 玻璃斜光）──
    def gauge(gx, gy, gw, gh):
        d.rectangle([gx, gy, gx+gw-1, gy+gh-1], outline=EDGE + (255,))
        d.line([gx+1, gy+1, gx+gw-2, gy+1], fill=PANEL_HI + (255,))
        d.line([gx+1, gy+1, gx+1, gy+gh-2], fill=PANEL_HI + (255,))
        d.line([gx+1, gy+gh-2, gx+gw-2, gy+gh-2], fill=PANEL_LO + (255,))
        d.line([gx+gw-2, gy+1, gx+gw-2, gy+gh-2], fill=PANEL_LO + (255,))
        # 四分之一刻度
        for i in range(1, 4):
            ty = gy + 2 + (gh - 4) * i // 4
            d.line([gx+3, ty, gx+6, ty], fill=PANEL_LO + (200,))
            d.line([gx+gw-7, ty, gx+gw-4, ty], fill=PANEL_LO + (200,))
        # 玻璃斜光（左上 45° 细带, 半透明白）
        for i in range(3, gw - 3):
            d.point([gx + i, gy + 3 + i], fill=(255, 255, 255, 70))
            if gy + 3 + i < gy + min(gh - 3, 16):
                d.point([gx + i, gy + 4 + i], fill=(255, 255, 255, 45))
    for k in ["GAUGE_STD", "GAUGE_SMALL", "GAUGE_TALL"]:
        gx, gy, gw, gh = R[k]
        gauge(gx, gy, gw, gh)

    # ── 能量条（井 + 琥珀渐变填充）──
    ex, ey, ew, eh = R["ENERGY_WELL"]
    d.rectangle([ex, ey, ex+ew-1, ey+eh-1], fill=(120, 120, 124, 255), outline=EDGE + (255,))
    d.line([ex+1, ey+1, ex+ew-2, ey+1], fill=PANEL_HI + (255,))
    fx, fy, fw, fh = R["ENERGY_FILL"]
    for i in range(fh):
        t = i / (fh - 1)
        col = (int(AMBER[0] - 60*t), int(AMBER[1] - 40*t), int(AMBER[2] - 20*t))
        d.line([fx, fy+i, fx+fw-1, fy+i], fill=col + (255,))
    d.line([fx+1, fy, fx+1, fy+fh-1], fill=(255, 220, 160, 120))

    # ── Fit 徽章（主框外左上角，浮起小面板 + 右下投影）──
    bx, by, bw, bh = R["FITBADGE"]
    d.rectangle([bx+2, by+3, bx+bw-1, by+bh-1+2], fill=(0, 0, 0, 60))  # 投影
    panel(d, bx, by, bw, bh)
    d.rectangle([bx+3, by+3, bx+bw-4, by+6], fill=TEAL + (255,))       # 青色题头带
    d.rectangle([bx+5, by+34, bx+bw-5, by+40], fill=WELL + (255,),
                outline=WELL_DK + (255,))                              # 条槽
    for i in range(3, 60, 8):
        d.point([bx+i, by+37], fill=PANEL_LO + (160,))

    # ── 升级侧板（主框右外侧 22×90）──
    ux, uy, uw, uh = R["UPGPANEL"]
    panel(d, ux, uy, uw, uh)
    for i in range(4):
        well(d, ux + 2, uy + 8 + i * 18)
        brackets(d, ux + 2, uy + 8 + i * 18, PURPLE)

    # ── 页签 ──
    def tab(tx, ty, right):
        d.rectangle([tx, ty, tx+25, ty+17], fill=PANEL + (255,))
        d.line([tx, ty+17, tx+25, ty+17], fill=EDGE + (255,))
        if right:
            d.line([tx, ty, tx, ty+17], fill=EDGE + (255,))
            d.line([tx+25, ty, tx+25, ty+17], fill=EDGE + (255,))
            d.line([tx+1, ty, tx+25, ty], fill=EDGE + (255,))
            d.line([tx+1, ty+1, tx+24, ty+1], fill=PANEL_HI + (255,))
            d.line([tx+1, ty+1, tx+1, ty+16], fill=PANEL_HI + (255,))
        else:
            d.line([tx, ty, tx+25, ty], fill=EDGE + (255,))
            d.line([tx, ty, tx, ty+17], fill=EDGE + (255,))
            d.line([tx+25, ty, tx+25, ty+17], fill=EDGE + (255,))
            d.line([tx+1, ty+1, tx+24, ty+1], fill=PANEL_HI + (255,))
            d.line([tx+1, ty+1, tx+1, ty+16], fill=PANEL_HI + (255,))
        well(d, tx + 5, ty + 1)
    tab(*R["TAB_L"][:2], False)
    tab(*R["TAB_R"][:2], True)

    # ── 图标（18×18 线性青色系）──
    def icon(ix, iy, kind):
        cx, cy = ix + 9, iy + 9
        p = TEAL + (255,)
        pl = TEAL_DK + (255,)
        if kind == "gear":
            import math
            for k in range(8):
                a = k * math.pi / 4
                d.rectangle([cx-1+int(6*math.cos(a))-1, cy-1+int(6*math.sin(a))-1,
                             cx-1+int(6*math.cos(a))+1, cy-1+int(6*math.sin(a))+1], fill=p)
            d.ellipse([cx-5, cy-5, cx+4, cy+4], outline=p)
            d.ellipse([cx-2, cy-2, cx+1, cy+1], fill=pl)
        elif kind == "dust":
            for k, (ox, oy) in enumerate([(-4,-2),(-1,-4),(3,-2),(-3,2),(1,1),(4,3),(0,4),(-1,-5)]):
                d.point([cx+ox, cy+oy], fill=p)
                d.point([cx+ox+1, cy+oy], fill=pl)
        elif kind == "io":
            d.polygon([(cx-7, cy-4), (cx-1, cy-4), (cx-1, cy-6), (cx+2, cy-2), (cx-1, cy+2), (cx-1, cy), (cx-7, cy)],
                      outline=p)
            d.polygon([(cx+7, cy+4), (cx+1, cy+4), (cx+1, cy+6), (cx-2, cy+2), (cx+1, cy-2), (cx+1, cy), (cx+7, cy)],
                      outline=p)
        elif kind == "bolt":
            d.polygon([(cx-1, cy-7), (cx-6, cy+1), (cx-1, cy+1), (cx-2, cy+7), (cx+5, cy-2), (cx, cy-2)], fill=p)
        elif kind == "warn":
            d.polygon([(cx, cy-7), (cx+7, cy+6), (cx-7, cy+6)], outline=RED + (255,))
            d.line([cx, cy-3, cx, cy+1], fill=RED + (255,))
            d.point([cx, cy+4], fill=RED + (255,))
        elif kind == "info":
            d.ellipse([cx-7, cy-7, cx+7, cy+7], outline=p)
            d.point([cx, cy-4], fill=p); d.point([cx, cy-3], fill=p)
            d.line([cx, cy-1, cx, cy+4], fill=p)
        elif kind == "transfer":
            d.line([cx-7, cy-3, cx+5, cy-3], fill=p)
            d.polygon([(cx+5, cy-6), (cx+8, cy-3), (cx+5, cy)], outline=p)
            d.line([cx+7, cy+3, cx-5, cy+3], fill=p)
            d.polygon([(cx-5, cy), (cx-8, cy+3), (cx-5, cy+6)], outline=p)
        elif kind == "close":
            for k in range(-5, 6):
                d.point([cx+k, cy+k], fill=RED + (255,)); d.point([cx+k, cy-k], fill=RED + (255,))
        elif kind == "flame":
            flame(cx-6, cy-6, True)
        elif kind == "field":
            import math
            pts = [(cx + int(7*math.cos(a)), cy + int(7*math.sin(a))) for a in
                   [i*math.pi/3 + math.pi/6 for i in range(6)]]
            d.polygon(pts, outline=p)
            d.point([cx, cy], fill=p)
        elif kind == "book":
            d.rectangle([cx-6, cy-6, cx+6, cy+6], outline=p)
            d.line([cx, cy-6, cx, cy+6], fill=p)
        elif kind == "blueprint":
            d.rectangle([cx-7, cy-5, cx+7, cy+5], outline=p)
            d.line([cx-4, cy-2, cx+4, cy-2], fill=p)
            d.line([cx-4, cy+1, cx+2, cy+1], fill=p)
    for name, kind in [("ICON_UPGRADE","gear"),("ICON_REDSTONE","dust"),("ICON_IO","io"),("ICON_ENERGY","bolt"),
                       ("ICON_WARNING","warn"),("ICON_INFO","info"),("ICON_TRANSFER","transfer"),("ICON_CLOSE","close"),
                       ("ICON_FLAME","flame"),("ICON_FIELD","field"),("ICON_BOOK","book"),("ICON_BLUEPRINT","blueprint")]:
        icon(*R[name][:2], kind)

    # ── 侧面配置浮窗（156×135 浅色）──
    sx, sy, sw, sh = R["SIDEWIN"]
    panel(d, sx, sy, sw, sh)
    d.rectangle([sx+3, sy+3, sx+sw-4, sy+12], fill=TEAL + (255,))
    d.rectangle([sx+4, sy+84, sx+sw-5, sy+108], fill=PANEL_LT + (255,), outline=PANEL_LO + (255,))

    # ── 面按钮 22×22 ×6（立方体展开图用）──
    fbx, fby, _, _ = R["FACEBTN"]
    for i in range(6):
        fx = fbx + i * 23
        d.rectangle([fx, fby, fx+21, fby+21], fill=PANEL_LT + (255,), outline=EDGE + (255,))
        d.line([fx+1, fby+1, fx+20, fby+1], fill=PANEL_HI + (255,))
        d.line([fx+1, fby+21, fx+20, fby+21], fill=PANEL_LO + (255,))
    # 配置色覆 22×22 ×4 (半透明色块 + 角标)
    cgx, cgy, _, _ = R["CFGOV"]
    for i, col in enumerate([(150,150,154), TEAL, GREEN, (58,110,190)]):
        fx = cgx + i * 23
        d.rectangle([fx, cgy, fx+21, cgy+21], fill=col + (150,), outline=col + (255,))
        d.rectangle([fx+4, cgy+4, fx+17, cgy+17], outline=(255,255,255,180))

    # ── JEI 类目底板（146×64 浅色）──
    jx, jy, jw, jh = R["JEI_BG"]
    panel(d, jx, jy, jw, jh)
    d.rectangle([jx+3, jy+3, jx+jw-4, jy+6], fill=TEAL + (255,))

    # ── JEI 流体族类目底板（146×88，261013 T3：上区双计量槽+下区物品槽行）──
    tx, ty, tw, th = R["JEI_BG_TALL"]
    panel(d, tx, ty, tw, th)
    d.rectangle([tx+3, ty+3, tx+tw-4, ty+6], fill=TEAL + (255,))

    im.save(os.path.join(A, "textures", "guis", "atlas.png"))
    print("✓ atlas.png (v3 浅色+青色)")

    # 兼容样式层 machine.png（176×166 独立文件）
    m = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    dm = ImageDraw.Draw(m)
    panel(dm, 0, 0, 176, 166)
    for row in range(3):
        for col in range(9):
            well(dm, 8 + col * 18, 84 + row * 18)
    for col in range(9):
        well(dm, 8 + col * 18, 142)
    m.save(os.path.join(A, "textures", "guis", "machine.png"))
    print("✓ machine.png (浅色兼容层)")

    # 坐标镜像导出（供 FWWidgets 常量比对）
    with open(os.path.join(ROOT, "atlas_coords.java.frag"), "w", encoding="utf-8") as f:
        for n, (x, y, w, h) in R.items():
            f.write(f"{n} {x} {y} {w} {h}\n")
    print("✓ atlas_coords.java.frag")

if __name__ == "__main__":
    build_atlas()
