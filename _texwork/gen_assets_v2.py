#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
总资产生成器 v2（UI重构/管道重构/蓝图/配方 一体）
产物：
 1. textures/guis/atlas.png (512×512)  —— AE2/Mek 式部件图集（§15）
 2. 机器 front_active 贴图 + blockstate(facing×active) + 模型（§16.1）
 3. 管道 multipart 资产（中心6×6×6+臂+法兰，§10.5）
 4. 机器合成配方（自举律 §7.5：P0 纯原版材料）+ 手册配方
 5. 巨物结构 JSON（蓝图投影用，§13）
改这里重跑，勿手改产物。
"""
import os, json, colorsys
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.abspath(__file__))
A = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "fieldworks")
D = os.path.join(ROOT, "..", "src", "main", "resources", "data", "fieldworks")

# ── 调色板（§15.3 一页纸规范） ──
C = {
    "bg":      (26, 29, 36),     # 深空石墨
    "panel":   (35, 40, 51),
    "panel2":  (30, 34, 43),
    "bevL":    (56, 64, 80),     # 亮棱（左上）
    "bevD":    (13, 15, 19),     # 暗棱（右下）
    "slotBg":  (18, 21, 26),
    "accent":  (232, 163, 61),   # 琥珀金
    "field":   (79, 195, 217),   # 冷青
    "text":    (143, 160, 181),
    "good":    (107, 203, 119),
    "warn":    (232, 197, 61),
    "bad":     (232, 133, 61),
    "input":   (79, 195, 217),
    "output":  (232, 163, 61),
    "power":   (107, 203, 119),
    "upg":     (176, 111, 224),
}

# ═══════════ 1. GUI 图集 ═══════════
def draw_panel(d, x, y, w, h, fill=None, inset=True):
    fill = fill or C["panel"]
    d.rectangle([x, y, x+w-1, y+h-1], fill=fill+(255,))
    if inset:
        d.line([x, y, x+w-1, y], fill=C["bevD"]+(255,))
        d.line([x, y, x, y+h-1], fill=C["bevD"]+(255,))
        d.line([x, y+h-1, x+w-1, y+h-1], fill=C["bevL"]+(255,))
        d.line([x+w-1, y, x+w-1, y+h-1], fill=C["bevL"]+(255,))
    else:
        d.line([x, y, x+w-1, y], fill=C["bevL"]+(255,))
        d.line([x, y, x, y+h-1], fill=C["bevL"]+(255,))
        d.line([x, y+h-1, x+w-1, y+h-1], fill=C["bevD"]+(255,))
        d.line([x+w-1, y, x+w-1, y+h-1], fill=C["bevD"]+(255,))

def build_atlas():
    im = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    d = ImageDraw.Draw(im, "RGBA")

    # 1a 标准背景 176×166 @(0,0)
    d.rectangle([0, 0, 175, 165], fill=C["bg"]+(255,))
    # 外框斜面
    d.rectangle([0, 0, 175, 2], fill=C["bevL"]+(255,))
    d.rectangle([0, 0, 2, 165], fill=C["bevL"]+(255,))
    d.rectangle([0, 163, 175, 165], fill=C["bevD"]+(255,))
    d.rectangle([173, 0, 175, 165], fill=C["bevD"]+(255,))
    # 机器区面板（上 64px）
    draw_panel(d, 5, 14, 166, 58, C["panel2"])
    # 面板内顶部强条（家族色带在代码端 tint：此处留灰）
    d.rectangle([7, 16, 169, 19], fill=(46, 53, 66, 255))
    # 背包区槽底衬（视觉分组）
    draw_panel(d, 5, 78, 166, 82, C["panel"])
    # 分隔条
    d.rectangle([6, 75, 169, 76], fill=C["bevD"]+(255,))

    # 1b 高背景 176×190 @(176,0)
    bx = 176
    d.rectangle([bx, 0, bx+175, 189], fill=C["bg"]+(255,))
    d.rectangle([bx, 0, bx+175, 2], fill=C["bevL"]+(255,))
    d.rectangle([bx, 0, bx+2, 189], fill=C["bevL"]+(255,))
    d.rectangle([bx, 187, bx+175, 189], fill=C["bevD"]+(255,))
    d.rectangle([bx+173, 0, bx+175, 189], fill=C["bevD"]+(255,))
    draw_panel(d, bx+5, 14, 166, 96, C["panel2"])
    d.rectangle([bx+7, 16, bx+169, 19], fill=(46, 53, 66, 255))
    draw_panel(d, bx+5, 114, 166, 72, C["panel"])
    d.rectangle([bx+6, 111, bx+169, 112], fill=C["bevD"]+(255,))

    # 1c 槽框 18×18 ×6 @(0,166..) normal/input/output/power/upgrade/tank
    def slot(x, y, col):
        d.rectangle([x, y, x+17, y+17], fill=C["slotBg"]+(255,))
        d.rectangle([x, y, x+17, y], fill=(col[0]//2, col[1]//2, col[2]//2, 255))
        d.rectangle([x, y, x, y+17], fill=(col[0]//2, col[1]//2, col[2]//2, 255))
        d.rectangle([x, y+17, x+17, y+17], fill=C["bevL"]+(255,))
        d.rectangle([x+17, y, x+17, y+17], fill=C["bevL"]+(255,))
        d.rectangle([x+1, y+1, x+16, y+1], fill=(col[0], col[1], col[2], 90))
        d.rectangle([x+1, y+1, x+1, y+16], fill=(col[0], col[1], col[2], 90))
    slot(0, 166, (120, 128, 140))    # normal 灰
    slot(19, 166, C["input"])        # input 青
    slot(38, 166, C["output"])       # output 琥珀
    slot(57, 166, C["power"])        # power 绿
    slot(76, 166, C["upg"])          # upgrade 紫
    slot(95, 166, (140, 150, 165))   # tank 端口银

    # 1d 槽 overlay 18×18 @(114,166..)：闪电/入/出/锁
    def bolt(x, y, col):
        d.polygon([(x+10, y+2), (x+4, y+10), (x+8, y+10), (x+6, y+16), (x+13, y+7), (x+9, y+7)], fill=col+(230,))
    def arrow_r(x, y, col):
        d.polygon([(x+3, y+6), (x+11, y+6), (x+11, y+3), (x+16, y+9), (x+11, y+15), (x+11, y+12), (x+3, y+12)], fill=col+(200,))
    def arrow_l(x, y, col):
        d.polygon([(x+15, y+6), (x+7, y+6), (x+7, y+3), (x+2, y+9), (x+7, y+15), (x+7, y+12), (x+15, y+12)], fill=col+(200,))
    bolt(114, 166, (255, 235, 120))
    arrow_r(133, 166, C["input"])
    arrow_l(152, 166, C["output"])
    # 高亮框（JEI/悬浮用）18×18 @(171,166)→ 放不下(171+18=189>176? 可以 171..189 越过bg区域但同图集) → 用 (190,166)
    d.rectangle([190, 166, 207, 183], outline=(255, 255, 255, 110), width=2)

    # 1e 进度族 @(0,188)
    def arrow_prog(x, y, filled, col):
        # 24×16 箭头底/进度
        body, head = (x+1, y+4, x+16, y+12), (x+16, y+1, x+22, y+15)
        if filled:
            d.polygon([(body[0], body[1]), (body[2], body[1]), (body[2], y+3), (x+22, y+8), (x+22, y+9), (x+16, y+14), (body[2], body[3]), (body[0], body[3])],
                      fill=col+(255,))
        else:
            d.polygon([(body[0], body[1]), (body[2], body[1]), (body[2], y+3), (x+22, y+8), (x+22, y+9), (x+16, y+14), (body[2], body[3]), (body[0], body[3])],
                      outline=(90, 98, 112, 255))
    arrow_prog(0, 188, False, None)
    arrow_prog(25, 188, True, C["field"])
    # BI 双向 16×6 @(50,188) 底 @(66,188) 亮
    d.polygon([(56, 191), (62, 191), (62, 189), (66, 194), (62, 199), (62, 197), (56, 197)], outline=(90, 98, 112, 255))
    d.polygon([(50, 191), (62, 191), (62, 189), (66, 194), (62, 199), (62, 197), (50, 197)], fill=C["field"]+(120,))
    d.polygon([(76, 191), (70, 191), (70, 189), (66, 194), (70, 199), (70, 197), (76, 197)], fill=C["field"]+(255,))
    # 火焰 13×13 底/亮
    def flame(x, y, hot):
        pts = [(x+6, y), (x+9, y+4), (x+11, y+7), (x+11, y+10), (x+6, y+13), (x+1, y+10), (x+1, y+7), (x+3, y+4)]
        if hot:
            d.polygon(pts, fill=(240, 150, 40, 255))
            d.polygon([(x+6, y+4), (x+8, y+7), (x+8, y+9), (x+6, y+11), (x+4, y+9), (x+4, y+7)], fill=(255, 220, 120, 255))
        else:
            d.polygon(pts, outline=(90, 98, 112, 255))
    flame(64, 188, False); flame(78, 188, True)
    # DOWN 8×20
    d.polygon([(72, 204), (72, 220), (68, 216), (68, 204)], outline=(90, 98, 112, 255))
    d.polygon([(82, 204), (82, 220), (78, 216), (78, 204)], fill=C["field"]+(255,))

    # 1f 计量槽 gauge @(0,230) standard 18×62（边框+玻璃）
    def gauge(x, y, w, h):
        d.rectangle([x, y, x+w-1, y+h-1], fill=(20, 23, 29, 255))
        d.rectangle([x, y, x+w-1, y], fill=C["bevD"]+(255,))
        d.rectangle([x, y, x, y+h-1], fill=C["bevD"]+(255,))
        d.rectangle([x, y+h-1, x+w-1, y+h-1], fill=C["bevL"]+(255,))
        d.rectangle([x+w-1, y, x+w-1, y+h-1], fill=C["bevL"]+(255,))
        # 玻璃斜光
        d.line([x+2, y+h-4, x+w-4, y+2], fill=(255, 255, 255, 26), width=2)
    gauge(0, 230, 18, 62)
    gauge(20, 230, 18, 34)   # small
    gauge(40, 230, 18, 100)  # tall(高机)
    # gauge 内容填充示例条（代码端 tint）：白色阶梯
    for i in range(28):
        d.line([42, 228+98-i*3, 56, 228+98-i*3], fill=(255, 255, 255, 24))

    # 1g 能量条 @(62,230) 8×56 外框 + 填充
    gauge(62, 230, 8, 56)
    for i in range(52):
        t = i/51
        col = (int(232-100*t), int(163+60*t), 61)
        d.line([64, 284-i, 68, 284-i], fill=col+(255,))

    # 1h Fit 面板底 @(74,230) 60×26
    draw_panel(d, 74, 230, 60, 26, C["panel2"])
    d.rectangle([78, 244, 130, 250], fill=(16, 19, 24, 255))
    d.rectangle([78, 244, 130, 244], fill=C["bevD"]+(255,))
    d.rectangle([78, 250, 130, 250], fill=C["bevL"]+(255,))
    # fit 填充（代码 tint）：白
    d.rectangle([79, 245, 129, 249], fill=(255, 255, 255, 30))

    # 1i 页签 26×18 底(左/右两用) @(0,300..)
    def tab(x, y):
        d.rectangle([x, y, x+25, y+17], fill=C["panel"]+(255,))
        d.rectangle([x, y, x+25, y], fill=C["bevL"]+(255,))
        d.rectangle([x, y, x, y+17], fill=C["bevL"]+(255,))
        d.rectangle([x+25, y, x+25, y+17], fill=C["bevD"]+(255,))
        d.rectangle([x, y+17, x+25, y+17], fill=C["bevD"]+(255,))
    for i in range(2):
        tab(i*27, 300)

    # 图标 18×18 网格 @(0,294)：12 格 (0,18,36,...,198)
    IY = 294
    def icell(i):
        return i * 18
    # 0 upgrade: 双箭头上(升级)
    d.polygon([(icell(0)+9, 0+IY+2), (icell(0)+3, IY+9), (icell(0)+8, IY+9)], fill=C["upg"]+(255,))
    d.polygon([(icell(0)+9, IY+2), (icell(0)+15, IY+9), (icell(0)+10, IY+9)], fill=(C["upg"][0]//2, C["upg"][1]//2, C["upg"][2]//2, 255))
    d.polygon([(icell(0)+9, IY+9), (icell(0)+3, IY+16), (icell(0)+15, IY+16)], fill=C["upg"]+(160,))
    # 1 redstone: 圆点+边
    d.ellipse([icell(1)+2, IY+2, icell(1)+15, IY+15], outline=(220, 60, 60, 255), width=2)
    d.ellipse([icell(1)+7, IY+7, icell(1)+10, IY+10], fill=(220, 60, 60, 255))
    # 2 IO cube
    d.polygon([(icell(2)+9, IY+2), (icell(2)+16, IY+6), (icell(2)+9, IY+10), (icell(2)+2, IY+6)], fill=C["input"]+(220,))
    d.polygon([(icell(2)+2, IY+6), (icell(2)+9, IY+10), (icell(2)+9, IY+16), (icell(2)+2, IY+12)], fill=(60, 140, 160, 230))
    d.polygon([(icell(2)+16, IY+6), (icell(2)+9, IY+10), (icell(2)+9, IY+16), (icell(2)+16, IY+12)], fill=(40, 100, 120, 230))
    # 3 energy bolt
    d.polygon([(icell(3)+11, IY+1), (icell(3)+4, IY+9), (icell(3)+9, IY+9), (icell(3)+6, IY+17), (icell(3)+14, IY+7), (icell(3)+9, IY+7)], fill=(255, 220, 90, 235))
    # 4 warning triangle
    d.polygon([(icell(4)+9, IY+2), (icell(4)+16, IY+15), (icell(4)+2, IY+15)], outline=(232, 197, 61, 255), width=2)
    d.rectangle([icell(4)+8, IY+7, icell(4)+10, IY+11], fill=(232, 197, 61, 255))
    d.rectangle([icell(4)+8, IY+13, icell(4)+10, IY+14], fill=(232, 197, 61, 255))
    # 5 info
    d.ellipse([icell(5)+2, IY+2, icell(5)+15, IY+15], outline=(120, 170, 220, 255), width=2)
    d.rectangle([icell(5)+8, IY+7, icell(5)+10, IY+12], fill=(120, 170, 220, 255))
    d.rectangle([icell(5)+8, IY+13, icell(5)+10, IY+14], fill=(120, 170, 220, 255))
    # 6 transfer: 左入右出双箭
    d.polygon([(icell(6)+2, IY+5), (icell(6)+8, IY+5), (icell(6)+8, IY+2), (icell(6)+13, IY+8), (icell(6)+8, IY+14), (icell(6)+8, IY+11), (icell(6)+2, IY+11)], fill=C["input"]+(220,))
    d.polygon([(icell(6)+16, IY+5), (icell(6)+10, IY+5), (icell(6)+10, IY+2), (icell(6)+5, IY+8), (icell(6)+10, IY+14), (icell(6)+10, IY+11), (icell(6)+16, IY+11)], fill=C["output"]+(180,))
    # 7 close X
    d.line([icell(7)+4, IY+4, icell(7)+13, IY+13], fill=(220, 90, 90, 255), width=3)
    d.line([icell(7)+13, IY+4, icell(7)+4, IY+13], fill=(220, 90, 90, 255), width=3)
    # 8 flame 小图标(能量页签用)
    d.polygon([(icell(8)+9, IY+1), (icell(8)+13, IY+7), (icell(8)+13, IY+11), (icell(8)+9, IY+16), (icell(8)+5, IY+11), (icell(8)+5, IY+7)], fill=(240, 150, 40, 235))
    d.polygon([(icell(8)+9, IY+7), (icell(8)+11, IY+10), (icell(8)+9, IY+13), (icell(8)+7, IY+10)], fill=(255, 220, 120, 255))
    # 9 探测/场谱(六边形)
    d.polygon([(icell(9)+9, IY+1), (icell(9)+16, IY+5), (icell(9)+16, IY+13), (icell(9)+9, IY+17), (icell(9)+2, IY+13), (icell(9)+2, IY+5)], outline=C["field"]+(255,), width=2)
    d.ellipse([icell(9)+7, IY+7, icell(9)+11, IY+11], fill=C["field"]+(255,))
    # 10 手册(书)
    d.rectangle([icell(10)+3, IY+2, icell(10)+14, IY+15], fill=(150, 60, 50, 255))
    d.rectangle([icell(10)+3, IY+2, icell(10)+14, IY+4], fill=(100, 40, 34, 255))
    d.line([icell(10)+8, IY+2, icell(10)+8, IY+15], fill=(90, 30, 26, 255), width=1)
    # 11 蓝图表(方格纸)
    d.rectangle([icell(11)+2, IY+3, icell(11)+15, IY+14], fill=(46, 90, 110, 255), outline=(200, 240, 250, 255))
    for gy in range(6, 14, 3):
        d.line([icell(11)+2, IY+gy, icell(11)+15, IY+gy], fill=(140, 200, 220, 120))
    for gx in range(5, 15, 3):
        d.line([icell(11)+gx, IY+3, icell(11)+gx, IY+14], fill=(140, 200, 220, 120))

    # 1j 侧配置浮窗 156×135 @(232,184)
    wx, wy = 232, 184
    d.rectangle([wx, wy, wx+155, wy+134], fill=C["bg"]+(255,))
    d.rectangle([wx, wy, wx+155, wy+2], fill=C["bevL"]+(255,))
    d.rectangle([wx, wy, wx+2, wy+134], fill=C["bevL"]+(255,))
    d.rectangle([wx, wy+132, wx+155, wy+134], fill=C["bevD"]+(255,))
    d.rectangle([wx+153, wy, wx+155, wy+134], fill=C["bevD"]+(255,))
    # 立方体展开图 6 面 22×22（位置：top(68,46) front(68,69) bottom(68,92) left(45,69) back(45,92) right(91,69) 相对窗）+ 窗口拖条
    d.rectangle([wx+4, wy+4, wx+151, wy+16], fill=C["panel2"]+(255,))

    # 立方面按钮底 22×22 ×6 @(0,330..)
    def facebtn(x, y):
        d.rectangle([x, y, x+21, y+21], fill=C["slotBg"]+(255,))
        d.rectangle([x, y, x+21, y], fill=C["bevL"]+(255,))
        d.rectangle([x, y, x, y+21], fill=C["bevL"]+(255,))
        d.rectangle([x, y+21, x+21, y+21], fill=C["bevD"]+(255,))
        d.rectangle([x+21, y, x+21, y+21], fill=C["bevD"]+(255,))
        d.line([x+3, y+18, x+18, y+3], fill=(255, 255, 255, 22), width=2)
    for i in range(6):
        facebtn(i*23, 330)
    # config 色覆 22×22 ×4（none灰/in青/out琥珀/both紫）@(0,354..)
    for i, col in enumerate([(120, 128, 140), C["input"], C["output"], C["upg"]]):
        d.rectangle([i*23+3, 354+3, i*23+18, 354+18], fill=(col[0], col[1], col[2], 70))
        d.rectangle([i*23+3, 354+3, i*23+18, 354+3], fill=(col[0], col[1], col[2], 200))
        d.rectangle([i*23+3, 354+18, i*23+18, 354+18], fill=(col[0], col[1], col[2], 120))

    im.save(os.path.join(A, "textures", "guis", "atlas.png"))
    print("✓ atlas.png (512×512)")

# ═══════════ 2. 机器 ACTIVE 态 ═══════════
FAMILY = {
    "ENERGY": (232, 163, 61),
    "MATERIAL": (201, 208, 216),
    "FIELD": (79, 195, 217),
    "ENVIRONMENT": (107, 203, 119),
    "CONVENIENCE": (176, 111, 224),
}
MACHINES = {
    "steam_generator": "ENERGY", "part_bench": "MATERIAL", "infinite_water_pump": "ENVIRONMENT",
    "electrolyzer_1": "FIELD", "blast_furnace": "ENERGY",
    "compressor": "MATERIAL", "distillation_tower": "MATERIAL", "gas_tank_1": "CONVENIENCE", "gas_tank_4": "CONVENIENCE",
    "gas_tank_16": "CONVENIENCE", "blower": "ENVIRONMENT", "hydrogen_shaft_furnace": "ENERGY", "steam_turbine": "ENERGY",
    "assembler": "MATERIAL", "condenser": "ENVIRONMENT", "recycler": "MATERIAL",
    "magnetizer": "FIELD", "magnetic_separator": "FIELD", "gravity_separator": "FIELD", "deep_drill": "MATERIAL",
    "negative_pressure_catcher": "FIELD", "irradiation_chamber": "FIELD", "heat_exchanger": "ENERGY",
    "carbon_capturer": "ENVIRONMENT", "corrosion_trap": "ENVIRONMENT",
    "electrolyzer_2": "FIELD", "electrolyzer_3": "FIELD", "plasma_electrolysis_matrix": "FIELD",
}

def build_active():
    texdir = os.path.join(A, "textures", "block")
    for m, fam in MACHINES.items():
        front = os.path.join(texdir, m + "_front.png")
        if not os.path.exists(front):
            print("! missing front:", m)
            continue
        im = Image.open(front).convert("RGBA")
        col = FAMILY[fam]
        out = im.copy()
        # 提亮 + 家族色发光（保 alpha）
        px, po = im.load(), out.load()
        for y in range(im.height):
            for x in range(im.width):
                r, g, b, a = px[x, y]
                if a == 0:
                    continue
                lum = (r*299+g*587+b*114)//1000
                w = 0.55 * max(0, lum-60)/195
                po[x, y] = (min(255, int(r*(1-w)+col[0]*w+30)), min(255, int(g*(1-w)+col[1]*w+30)),
                            min(255, int(b*(1-w)+col[2]*w+30)), a)
        out.save(os.path.join(texdir, m + "_front_active.png"))

        # 模型
        model_dir = os.path.join(A, "models", "block")
        base = json.load(open(os.path.join(model_dir, m + ".json"), encoding="utf-8"))
        act = json.loads(json.dumps(base))
        act["textures"]["north"] = f"fieldworks:block/{m}_front_active"
        json.dump(act, open(os.path.join(model_dir, m + "_active.json"), "w", encoding="utf-8"), indent=1)

        # blockstate：facing×active 8 变体
        bs = {"variants": {}}
        for f, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
            for act_v in ("false", "true"):
                bs["variants"][f"facing={f},active={act_v}"] = {
                    "model": f"fieldworks:block/{m}_active" if act_v == "true" else f"fieldworks:block/{m}",
                    "y": y}
        json.dump(bs, open(os.path.join(A, "blockstates", m + ".json"), "w", encoding="utf-8"), indent=1)
    print("✓ active faces/models/blockstates ×", len(MACHINES))

# ═══════════ 3. 管道资产 ═══════════
PIPE_FAMILIES = {
    # 261014 T2 三族：气液合并为流体管(蓝) + 物品管(琥珀橙) + 能量管(青)
    "fluid_pipe": (58, 110, 190),
    "item_pipe": (232, 163, 61),
    "energy_pipe": (30, 158, 150),
}

def pipe_tex(name, col):
    """16×16：中心端面(法兰圈+内芯) + 同图用作臂侧面"""
    im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(im, "RGBA")
    dark = (col[0]//2, col[1]//2, col[2]//2)
    # 外圈金属
    d.ellipse([2, 2, 13, 13], fill=(70, 76, 86, 255))
    d.ellipse([2, 2, 13, 13], outline=(46, 50, 58, 255), width=1)
    # 法兰高光
    d.arc([2, 2, 13, 13], 200, 320, fill=(120, 128, 140, 255), width=2)
    # 家族色环
    d.ellipse([4, 4, 11, 11], outline=col+(255,), width=2)
    # 内芯（透明感：半透家族色）
    d.ellipse([6, 6, 9, 9], fill=(col[0], col[1], col[2], 190))
    d.ellipse([6, 6, 7, 7], fill=(255, 255, 255, 120))
    im.save(os.path.join(A, "textures", "block", name + ".png"))

def pipe_models():
    texdir = os.path.join(A, "textures", "block")
    mdir = os.path.join(A, "models", "block")
    for fam, col in PIPE_FAMILIES.items():
        pipe_tex(fam + "_center", col)
        # 臂侧面（沿轴向拉伸观感：竖条纹）
        arm = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(arm, "RGBA")
        d.rectangle([0, 0, 15, 15], fill=(70, 76, 86, 255))
        for i, x in enumerate(range(0, 16, 4)):
            shade = (58, 63, 72) if i % 2 == 0 else (82, 89, 100)
            d.rectangle([x, 0, x+3, 15], fill=shade+(255,))
        d.line([7, 0, 7, 15], fill=col+(200,), width=2)
        arm.save(os.path.join(texdir, fam + "_arm.png"))
        # 法兰贴图（连接机器端）
        fl = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(fl, "RGBA")
        d.rectangle([0, 0, 15, 15], fill=(84, 91, 102, 255))
        d.rectangle([0, 0, 15, 1], fill=(120, 128, 140, 255))
        d.rectangle([0, 14, 15, 15], fill=(52, 56, 64, 255))
        d.ellipse([5, 5, 10, 10], fill=col+(230,))
        fl.save(os.path.join(texdir, fam + "_flange.png"))

        # 中心模型 6×6×6
        center = {
            "parent": "minecraft:block/block",
            "textures": {"particle": f"fieldworks:block/{fam}_center", "0": f"fieldworks:block/{fam}_center"},
            "elements": [{"from": [5, 5, 5], "to": [11, 11, 11],
                          "faces": {sd: {"texture": "0", "uv": [5, 5, 11, 11]} for sd in
                                    ("north", "south", "east", "west", "up", "down")}}]}
        json.dump(center, open(os.path.join(mdir, fam + "_center.json"), "w", encoding="utf-8"), indent=1)

        # 六向臂模型（元素直写到边）
        def arm_model(axis, neg):
            # axis: 'x'/'y'/'z'; neg: 指向负轴
            t = f"fieldworks:block/{fam}_arm"
            faces = {"north": [0, 0, 16, 16], "south": [0, 0, 16, 16], "east": [0, 0, 16, 16],
                     "west": [0, 0, 16, 16], "up": [0, 0, 16, 16], "down": [0, 0, 16, 16]}
            if axis == "z":
                frm, to = ([5, 5, 0], [11, 11, 5]) if neg else ([5, 5, 11], [11, 11, 16])
            elif axis == "y":
                frm, to = ([5, 0, 5], [11, 5, 11]) if neg else ([5, 11, 5], [11, 16, 11])
            else:
                frm, to = ([0, 5, 5], [5, 11, 11]) if neg else ([11, 5, 5], [16, 11, 11])
            return {"parent": "minecraft:block/block",
                    "textures": {"particle": t, "0": t},
                    "elements": [{"from": frm, "to": to,
                                  "faces": {sd: {"texture": "0", "uv": uv} for sd, uv in faces.items()}}]}

        for dirname, (axis, neg) in {"north": ("z", True), "south": ("z", False), "west": ("x", True),
                                      "east": ("x", False), "down": ("y", True), "up": ("y", False)}.items():
            json.dump(arm_model(axis, neg), open(os.path.join(mdir, f"{fam}_arm_{dirname}.json"), "w", encoding="utf-8"), indent=1)

        # 法兰模型（贴边的 8×8×3 环）
        def flange_model(dirname):
            t = f"fieldworks:block/{fam}_flange"
            rng = {"north": ([4, 4, 0], [12, 12, 2]), "south": ([4, 4, 14], [12, 12, 16]),
                   "west": ([0, 4, 4], [2, 12, 12]), "east": ([14, 4, 4], [16, 12, 12]),
                   "down": ([4, 0, 4], [12, 2, 12]), "up": ([4, 14, 4], [12, 16, 12])}[dirname]
            faces = {sd: {"texture": "0", "uv": [4, 4, 12, 12]} for sd in ("north", "south", "east", "west", "up", "down")}
            return {"parent": "minecraft:block/block",
                    "textures": {"particle": t, "0": t},
                    "elements": [{"from": rng[0], "to": rng[1], "faces": faces}]}

        for dirname in ("north", "south", "west", "east", "down", "up"):
            json.dump(flange_model(dirname), open(os.path.join(mdir, f"{fam}_flange_{dirname}.json"), "w", encoding="utf-8"), indent=1)

        # blockstate multipart：NONE/PIPE/MACHINE 三态
        bs = {"multipart": [{"apply": {"model": f"fieldworks:block/{fam}_center"}}]}
        for dirname in ("north", "south", "west", "east", "down", "up"):
            bs["multipart"].append({"when": {dirname: "pipe"},
                                    "apply": {"model": f"fieldworks:block/{fam}_arm_{dirname}"}})
            bs["multipart"].append({"when": {dirname: "machine"},
                                    "apply": [{"model": f"fieldworks:block/{fam}_arm_{dirname}"},
                                              {"model": f"fieldworks:block/{fam}_flange_{dirname}"}]})
        json.dump(bs, open(os.path.join(A, "blockstates", fam + ".json"), "w", encoding="utf-8"), indent=1)

        # 261013 T6：物品栏直段模型(中心 6×6 + 南北两臂 = 世界直管真实形态)
        tc = f"fieldworks:block/{fam}_center"
        ta = f"fieldworks:block/{fam}_arm"
        seg = {"parent": "minecraft:block/block",
               "textures": {"particle": ta, "0": tc, "1": ta},
               "elements": [
                   {"from": [5, 5, 5], "to": [11, 11, 11],
                    "faces": {sd: {"texture": "0", "uv": [5, 5, 11, 11]} for sd in
                              ("north", "south", "east", "west", "up", "down")}},
                   {"from": [5, 5, 0], "to": [11, 11, 5],
                    "faces": {"north": {"texture": "1", "uv": [0, 0, 16, 16]},
                              "east": {"texture": "1", "uv": [0, 0, 16, 16]},
                              "west": {"texture": "1", "uv": [0, 0, 16, 16]},
                              "up": {"texture": "1", "uv": [0, 0, 16, 16]},
                              "down": {"texture": "1", "uv": [0, 0, 16, 16]}}},
                   {"from": [5, 5, 11], "to": [11, 11, 16],
                    "faces": {"south": {"texture": "1", "uv": [0, 0, 16, 16]},
                              "east": {"texture": "1", "uv": [0, 0, 16, 16]},
                              "west": {"texture": "1", "uv": [0, 0, 16, 16]},
                              "up": {"texture": "1", "uv": [0, 0, 16, 16]},
                              "down": {"texture": "1", "uv": [0, 0, 16, 16]}}},
               ]}
        json.dump(seg, open(os.path.join(mdir, f"{fam}_item.json"), "w", encoding="utf-8"), indent=1)
        # 物品模型 + 26.1 物品定义均指向直段模型(gen_data SIMPLE_BLOCKS 已不含管道——此处为唯一事实源)
        idir = os.path.join(A, "models", "item")
        json.dump({"parent": f"fieldworks:block/{fam}_item"},
                  open(os.path.join(idir, f"{fam}.json"), "w", encoding="utf-8"), indent=1)
        json.dump({"model": {"type": "minecraft:model", "model": f"fieldworks:block/{fam}_item"}},
                  open(os.path.join(A, "items", f"{fam}.json"), "w", encoding="utf-8"), indent=1)
    print("✓ pipe assets ×", len(PIPE_FAMILIES))

# ═══════════ 4. 配方表（自举律 §7.5：P0 纯原版） ═══════════
def build_recipes():
    R = os.path.join(D, "recipe")

    def shaped(name, pattern, keys, result, count=1, comps=None):
        r = {"type": "minecraft:crafting_shaped",
             "pattern": [pattern[i:i+1] for i in range(len(pattern))] if isinstance(pattern, str) else list(pattern),
             "key": dict(keys),
             "result": {"id": result, "count": count}}
        if comps:
            r["result"]["components"] = comps
        json.dump(r, open(os.path.join(R, name + ".json"), "w", encoding="utf-8"), indent=1)

    def tag(t):
        return "#" + t

    # ── 手册（patchouli:guide_book + components） ──
    shaped("book_manual", ["III", "IBI", "PPP"],
           {"I": tag("minecraft:planks"), "B": "minecraft:book", "P": "minecraft:paper"},
           "patchouli:guide_book", 1, {"patchouli:book": "fieldworks:manual"})

    # ── P0 机器（纯原版，自举律） ──
    shaped("m_steam_generator", ["III", "WFW", "CCC"],
           {"I": "minecraft:iron_ingot", "W": "minecraft:water_bucket", "F": "minecraft:furnace", "C": "minecraft:cobblestone"},
           "fieldworks:steam_generator")
    shaped("m_part_bench", ["III", "ICI", "PDP"],
           {"I": "minecraft:iron_ingot", "C": "minecraft:crafting_table", "P": tag("minecraft:planks"), "D": "minecraft:dried_kelp"},  # D=绝缘垫(原版)
           "fieldworks:part_bench")
    # 电解器Ⅰ：铁壳+玻璃管+红石
    shaped("m_electrolyzer_1", ["IGI", "GRG", "ICI"],
           {"I": "minecraft:iron_ingot", "G": "minecraft:glass", "R": "minecraft:redstone", "C": "minecraft:copper_ingot"},
           "fieldworks:electrolyzer_1")
    shaped("m_blast_furnace", ["I I", "IFI", "CCC"],
           {"I": "minecraft:iron_ingot", "F": "minecraft:furnace", "C": "minecraft:cobblestone"},
           "fieldworks:blast_furnace")

    # ── P1 机器（部件+钢：机器产物合法，非自举） ──
    shaped("m_compressor", ["SIS", "IPP", "ISI"],
           {"S": "fieldworks:steel_ingot", "I": "minecraft:iron_ingot", "P": "fieldworks:piston_frame"},
           "fieldworks:compressor")
    shaped("m_distillation_tower", ["STS", "TGT", "SPS"],
           {"S": "fieldworks:steel_ingot", "T": "fieldworks:distillation_tray", "G": "minecraft:glass", "P": "fieldworks:precision_bearing"},
           "fieldworks:distillation_tower")
    shaped("m_gas_tank_1", "IGI", {"I": "minecraft:iron_ingot", "G": "minecraft:glass"}, "fieldworks:gas_tank_1")
    shaped("m_gas_tank_4", ["I I", "GTG", "III"],
           {"I": "fieldworks:steel_ingot", "G": "minecraft:glass", "T": "fieldworks:gas_tank_1"}, "fieldworks:gas_tank_4")
    shaped("m_gas_tank_16", ["I I", "GTG", "III"],
           {"I": "fieldworks:invar_ingot", "G": "minecraft:glass", "T": "fieldworks:gas_tank_4"}, "fieldworks:gas_tank_16")
    shaped("m_blower", ["I I", "IFI", "IRI"],
           {"I": "fieldworks:steel_ingot", "F": "fieldworks:nozzle_assembly", "R": "minecraft:redstone"},
           "fieldworks:blower")
    shaped("m_hydrogen_shaft_furnace", ["SFS", "FGF", "SCS"],
           {"S": "fieldworks:steel_ingot", "F": "minecraft:furnace", "G": "fieldworks:copper_coil", "C": "minecraft:copper_block"},
           "fieldworks:hydrogen_shaft_furnace")
    shaped("m_steam_turbine", ["IBI", "BGB", "IBI"],
           {"I": "fieldworks:steel_ingot", "B": "fieldworks:turbine_blade", "G": "fieldworks:steel_gear"},
           "fieldworks:steam_turbine")
    shaped("m_assembler", ["SGS", "RLR", "SSS"],
           {"S": "fieldworks:steel_ingot", "G": "fieldworks:steel_gear", "R": "minecraft:redstone", "L": "fieldworks:logic_circuit"},
           "fieldworks:assembler")
    shaped("m_condenser", ["IGI", "GCG", "I I"],
           {"I": "fieldworks:steel_ingot", "G": "minecraft:glass", "C": "minecraft:copper_ingot"},
           "fieldworks:condenser")
    shaped("m_recycler", ["I I", "IFI", "III"],
           {"I": "fieldworks:steel_ingot", "F": "minecraft:furnace"}, "fieldworks:recycler")

    # ── P2 机器 ──
    shaped("m_magnetizer", ["EIE", "ICI", "EPE"],
           {"E": "fieldworks:electromagnet_core", "I": "fieldworks:steel_ingot", "C": "fieldworks:copper_coil", "P": "fieldworks:magnetized_core"},
           "fieldworks:magnetizer")
    shaped("m_magnetic_separator", ["IEI", "GSG", "IBI"],
           {"I": "fieldworks:steel_ingot", "E": "fieldworks:electromagnet_core", "G": "minecraft:glass", "S": "fieldworks:steel_gear", "B": "fieldworks:precision_bearing"},
           "fieldworks:magnetic_separator")
    shaped("m_gravity_separator", ["IGI", "SBS", "IPI"],
           {"I": "fieldworks:invar_ingot", "G": "minecraft:glass", "S": "fieldworks:steel_gear", "B": "fieldworks:precision_bearing", "P": "fieldworks:piston_frame"},
           "fieldworks:gravity_separator")
    shaped("m_deep_drill", ["DBD", "BSB", "DSD"],
           {"D": "minecraft:diamond", "B": "fieldworks:aluminum_brass_ingot", "S": "fieldworks:steel_ingot"},
           "fieldworks:deep_drill")
    shaped("m_negative_pressure_catcher", ["IGI", "GVG", "INI"],
           {"I": "fieldworks:invar_ingot", "G": "minecraft:glass", "V": "fieldworks:void_condensate", "N": "fieldworks:nozzle_assembly"},
           "fieldworks:negative_pressure_catcher")
    shaped("m_irradiation_chamber", ["LIL", "ITI", "LIL"],
           {"L": "fieldworks:lead_sheet" if False else "minecraft:iron_ingot", "I": "fieldworks:steel_ingot", "T": "fieldworks:thorium_pellet"},
           "fieldworks:irradiation_chamber")
    shaped("m_heat_exchanger", ["ICI", "CVC", "ICI"],
           {"I": "fieldworks:steel_ingot", "C": "fieldworks:copper_coil", "V": "minecraft:copper_block"},
           "fieldworks:heat_exchanger")

    # ── P3 机器 ──
    shaped("m_carbon_capturer", ["SGS", "GCG", "SGS"],
           {"S": "fieldworks:stainless_steel_ingot", "G": "minecraft:glass", "C": "fieldworks:carbon_black"},
           "fieldworks:carbon_capturer")
    shaped("m_corrosion_trap", ["MGM", "GCG", "MMM"],
           {"M": "fieldworks:monel_ingot", "G": "minecraft:glass", "C": "fieldworks:corrosion_salt"},
           "fieldworks:corrosion_trap")
    shaped("m_electrolyzer_2", ["SGS", "GRG", "SCS"],
           {"S": "fieldworks:steel_ingot", "G": "minecraft:glass", "R": "minecraft:redstone", "C": "fieldworks:copper_coil"},
           "fieldworks:electrolyzer_2")
    shaped("m_electrolyzer_3", ["PEP", "GRG", "PMP"],
           {"P": "fieldworks:permalloy_ingot", "E": "fieldworks:electromagnet_core", "G": "minecraft:glass", "R": "minecraft:redstone", "M": "fieldworks:magnetized_core"},
           "fieldworks:electrolyzer_3")
    shaped("m_plasma_electrolysis_matrix", ["SUS", "URU", "SUS"],
           {"S": "fieldworks:superconductor_alloy", "U": "fieldworks:superconductor_ring", "R": "fieldworks:resonant_crystal_ultra"},
           "fieldworks:plasma_electrolysis_matrix")

    # ── 巨物蓝图(§13)：4纸+变体物+巨物核心 → 蓝图(components 带变体) ──
    BLUEPRINTS = {
        "resonance_tower": "fieldworks:resonance_fuel",
        "deep_electrolysis_array": "fieldworks:copper_coil",
        "eco_dome": "minecraft:oak_sapling",
        "warp_anchor": "minecraft:ender_pearl",
        "harmonic_cathedral": "minecraft:amethyst_shard",
        "planetary_array": "minecraft:echo_shard",
    }
    for variant, mat in BLUEPRINTS.items():
        shaped(f"blueprint_{variant}", ["P P", "PXG", "P P"],
               {"P": "minecraft:paper", "X": mat, "G": "fieldworks:giant_core"},
               "fieldworks:giant_blueprint", 1,
               {"fieldworks:giant_variant": f"fieldworks:{variant}"})
    # 巨物控制器直造(手动路线)：桁架+外壳+核心
    for variant in BLUEPRINTS:
        shaped(f"m_{variant}", ["TST", "SGS", "TST"],
               {"T": "fieldworks:reinforced_truss", "S": "fieldworks:steel_shell", "G": "fieldworks:giant_core"},
               f"fieldworks:{variant}")

    # ── 管道 ──
    shaped("m_gas_pipe", ["GCG"], {"G": "minecraft:glass", "C": "minecraft:copper_ingot"}, "fieldworks:gas_pipe", 8)
    shaped("m_fluid_pipe", ["GCG"], {"G": "minecraft:glass", "C": "fieldworks:steel_ingot"}, "fieldworks:fluid_pipe", 8)
    shaped("m_pump", ["IRI", "RBR", "IRI"],
           {"I": "fieldworks:steel_ingot", "R": "minecraft:redstone", "B": "fieldworks:piston_frame"},
           "fieldworks:pump")
    print("✓ crafting recipes")

# ═══════════ 5. 巨物结构 JSON ═══════════
GIANTS = {
    "resonance_tower": {"name": "谐振聚能塔", "size": [5, 9, 5], "type": "tower"},
    "deep_electrolysis_array": {"name": "深井电解阵列", "size": [7, 5, 7], "type": "array"},
    "eco_dome": {"name": "生态穹顶", "size": [9, 7, 9], "type": "dome"},
    "warp_anchor": {"name": "曲率锚", "size": [5, 5, 5], "type": "anchor"},
    "harmonic_cathedral": {"name": "谐律大教堂", "size": [7, 9, 9], "type": "church"},
    "planetary_array": {"name": "行星调谐阵列", "size": [11, 5, 11], "type": "ring"},
}

def build_structures():
    SD = os.path.join(D, "giant_structures")
    os.makedirs(SD, exist_ok=True)
    import random
    rng = random.Random(42)

    def block(x, y, z, b):
        return {"x": x, "y": y, "z": z, "block": b}

    for gid, info in GIANTS.items():
        w, h, d = info["size"]
        blocks = []
        t = info["type"]
        cx, cz = w // 2, d // 2
        for y in range(h):
            for x in range(w):
                for z in range(d):
                    edge = x in (0, w-1) or z in (0, d-1)
                    if t == "tower":
                        # 塔：四角桁架柱+玻璃幕墙+顶部平台+底部外壳
                        corner = (x in (0, w-1)) and (z in (0, d-1))
                        if corner and y < h - 1:
                            blocks.append(block(x, y, z, "fieldworks:reinforced_truss"))
                        elif edge and y == h - 1:
                            blocks.append(block(x, y, z, "fieldworks:steel_shell"))
                        elif edge and 0 < y < h - 2 and y % 2 == 1:
                            blocks.append(block(x, y, z, "fieldworks:observation_glass"))
                        elif y == 0 and edge:
                            blocks.append(block(x, y, z, "fieldworks:steel_shell"))
                    elif t == "array":
                        # 阵列：底部满外壳+顶桁架格
                        if y == 0:
                            blocks.append(block(x, y, z, "fieldworks:steel_shell"))
                        elif y == h - 1 and (x + z) % 2 == 0:
                            blocks.append(block(x, y, z, "fieldworks:reinforced_truss"))
                        elif edge and y == 1:
                            blocks.append(block(x, y, z, "fieldworks:observation_glass"))
                    elif t == "dome":
                        # 穹顶：半球
                        rr = ((x - cx) ** 2 + (z - cz) ** 2 + (y - h + 1) ** 2) ** 0.5
                        rr2 = ((x - cx) ** 2 + (z - cz) ** 2 + max(0, y - h + 2) ** 2) ** 0.5
                        if y == 0:
                            blocks.append(block(x, y, z, "fieldworks:steel_shell"))
                        elif abs(rr - (w / 2)) < 0.9 and y >= h - 3:
                            blocks.append(block(x, y, z, "fieldworks:observation_glass"))
                        elif rr < w / 2 and y in (1, h - 4) and edge:
                            blocks.append(block(x, y, z, "fieldworks:reinforced_truss"))
                    elif t == "anchor":
                        # 锚：立方桁架笼
                        frame = x in (0, w-1) and z in (0, d-1) or y in (0, h-1) and edge or y in (0, h-1)
                        shell = (x in (0, w-1) or z in (0, d-1)) and y == h // 2
                        if (x in (0, w-1) and z in (0, d-1)) or (y in (0, h-1) and edge) or (y in (0, h-1)):
                            if y in (0, h-1) and not edge:
                                continue
                            blocks.append(block(x, y, z, "fieldworks:reinforced_truss" if y != h // 2 else "fieldworks:steel_shell"))
                        elif shell:
                            blocks.append(block(x, y, z, "fieldworks:observation_glass"))
                    elif t == "church":
                        # 教堂：石廊+高塔
                        if y == 0:
                            blocks.append(block(x, y, z, "fieldworks:steel_shell"))
                        elif edge and y < 4 and y % 2 == 0:
                            blocks.append(block(x, y, z, "fieldworks:observation_glass"))
                        elif x == cx and z == cz and y < h - 1:
                            blocks.append(block(x, y, z, "fieldworks:reinforced_truss"))
                        elif y == h - 1:
                            blocks.append(block(x, y, z, "fieldworks:reinforced_truss" if (x + z) % 2 == 0 else "fieldworks:amethyst_plate"))
                    elif t == "ring":
                        # 环形阵列
                        rr = ((x - cx) ** 2 + (z - cz) ** 2) ** 0.5
                        if y == 0 and abs(rr - w / 2 + 1) < 1.1:
                            blocks.append(block(x, y, z, "fieldworks:reinforced_truss"))
                        elif y == 1 and abs(rr - w / 2 + 1) < 1.1 and (x + z) % 2 == 0:
                            blocks.append(block(x, y, z, "fieldworks:amethyst_plate"))
        # 控制器与核心
        blocks.append(block(cx, 0, cz, f"fieldworks:{gid}"))
        data = {"size": [w, h, d], "origin": "bottom_center", "blocks": blocks}
        json.dump(data, open(os.path.join(SD, gid + ".json"), "w", encoding="utf-8"), indent=1)
        print(f"✓ structure {gid}: {len(blocks)} blocks")

def build_projection_assets():
    # blockstate：单变体，不可见模型
    json.dump({"variants": {"": {"model": "fieldworks:block/projection"}}},
              open(os.path.join(A, "blockstates", "projection.json"), "w", encoding="utf-8"))
    # 261013：空 faces 元素在 26.1.2 非法(“Expected between 1 and 6 unique faces”)——
    # 空 elements 表=合法全隐形模型(方块 RenderShape.INVISIBLE，模型仅需可解析)。
    json.dump({"parent": "minecraft:block/block",
               "textures": {"particle": "fieldworks:block/fluid_pipe_center"},
               "elements": []},
              open(os.path.join(A, "models", "block", "projection.json"), "w", encoding="utf-8"))
    json.dump({"model": {"type": "minecraft:model", "model": "fieldworks:block/projection"}},
              open(os.path.join(A, "items", "projection.json"), "w", encoding="utf-8"))
    print("✓ projection assets")

if __name__ == "__main__":
    # 261013 归属修订(继 261012 管道/机器 blockstate 事故后的根治)：
    # 图集→gen_atlas_v3 / 配方→gen_data / 巨物结构→gen_giants_v3。
    # 本文件仅保留：机器 active 变体、管道模型+物品模型、投影模型资产。
    build_active()
    pipe_models()
    build_projection_assets()
    print("ALL DONE")
