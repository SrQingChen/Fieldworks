#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
巨物方块贴图 v3 —— 261014 T9：
学习机器方块贴图语言(§16.1 五面语言)，为 6 台巨物控制器 + 巨物核心重制
多面立体贴图(front=主控面板+族色屏+指示灯+铭牌；side=装甲板+铆钉+族色条；
top=散热格栅+族色环)。32×32 超采样渲染 → 16×16 输出(抗锯齿)。
产物: textures/block/{name}_{front,side,top}.png
模型引用见 gen_data.py CONTROLLER_BLOCKS 段。
"""
import os
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "fieldworks", "textures", "block")

SS = 2          # 超采样
S = 16 * SS     # 32

VARIANTS = {
    # name: 族色 RGB + 副色
    "resonance_tower":        ((232, 163, 61), (255, 220, 140)),
    "deep_electrolysis_array": ((30, 158, 150), (127, 216, 208)),
    "eco_dome":               ((76, 190, 110), (160, 240, 180)),
    "warp_anchor":            ((168, 96, 232), (208, 168, 255)),
    "harmonic_cathedral":     ((200, 130, 255), (236, 205, 255)),
    "planetary_array":        ((240, 200, 90), (255, 238, 170)),
    "giant_core":             ((240, 200, 90), (127, 240, 228)),
}

BODY = (74, 62, 44)        # 古铜机身(巨物比石墨铁更厚重偏青铜)
BODY_L = (108, 94, 66)
BODY_D = (48, 38, 26)
EDGE = (28, 22, 14)
PLATE = (96, 82, 58)


def base_panel(d, light=(0, 0, 0)):
    """机身底 + 斜向受光渐变 + 1px 倒角。"""
    for y in range(S):
        f = 0.82 + 0.38 * (1 - y / (S - 1))
        for x in range(S):
            fx = 0.94 + 0.10 * (1 - x / (S - 1))
            c = tuple(min(255, int(v * f * fx)) for v in BODY)
            d.point((x, y), fill=c + (255,))
    # 倒角
    d.line([(0, 0), (S - 1, 0)], fill=tuple(min(255, v + 56) for v in BODY) + (255,))
    d.line([(0, 0), (0, S - 1)], fill=tuple(min(255, v + 40) for v in BODY) + (255,))
    d.line([(0, S - 1), (S - 1, S - 1)], fill=EDGE + (255,))
    d.line([(S - 1, 0), (S - 1, S - 1)], fill=EDGE + (255,))


def rivet(d, x, y, r=1):
    rr = r * SS
    d.ellipse([x - rr, y - rr, x + rr, y + rr], fill=(38, 30, 20, 255))
    d.ellipse([x - rr, y - rr, x + rr - SS, y + rr - SS], fill=(152, 138, 104, 255))


def glow_color(c, f=1.0):
    return tuple(min(255, int(v * f)) for v in c)


def tex_front(col, hi):
    im = Image.new("RGBA", (S, S))
    d = ImageDraw.Draw(im)
    base_panel(d)
    # 主控屏(居中大屏)：族色辉光渐变 + 扫描线
    x0, y0, x1, y1 = 4 * SS, 4 * SS, 12 * SS, 11 * SS
    for yy in range(y0, y1):
        t = (yy - y0) / max(1, y1 - y0 - 1)
        c = tuple(int(a + (b - a) * t) for a, b in zip(col, hi))
        d.line([(x0, yy), (x1 - 1, yy)], fill=c + (255,))
    for yy in range(y0 + SS, y1, 2 * SS):     # 扫描线
        d.line([(x0, yy), (x1 - 1, yy)], fill=(0, 0, 0, 70))
    d.rectangle([x0, y0, x1 - 1, y1 - 1], outline=EDGE + (255,))
    d.line([(x0 + 1, y0 + 1), (x1 - 2, y0 + 1)], fill=glow_color(hi, 1.25) + (255,))
    # 屏内指示条
    d.rectangle([x0 + SS, y1 - 2 * SS, x0 + 5 * SS, y1 - SS], fill=glow_color(hi, 1.1) + (255,))
    # 铭牌
    d.rectangle([4 * SS, 12 * SS, 12 * SS, 14 * SS], fill=PLATE + (255,))
    d.rectangle([4 * SS, 12 * SS, 12 * SS, 12 * SS + SS], fill=(140, 124, 92, 255))
    d.line([(5 * SS, 13 * SS), (11 * SS, 13 * SS)], fill=glow_color(col, 0.95) + (255,), width=SS)
    # 状态灯 ×2
    d.ellipse([12 * SS + SS, 2 * SS, 12 * SS + 2 * SS + SS, 3 * SS + SS], fill=(70, 200, 90, 255))
    d.ellipse([10 * SS + SS, 2 * SS, 10 * SS + 2 * SS + SS, 3 * SS + SS], fill=(230, 180, 60, 255))
    # 四角铆钉
    for cx, cy in [(2 * SS, 2 * SS), (14 * SS, 2 * SS), (2 * SS, 14 * SS), (14 * SS, 14 * SS)]:
        rivet(d, cx, cy)
    return im


def tex_side(col, hi):
    im = Image.new("RGBA", (S, S))
    d = ImageDraw.Draw(im)
    base_panel(d)
    # 装甲内板
    d.rectangle([3 * SS, 3 * SS, 13 * SS, 13 * SS], fill=tuple(v + 14 for v in BODY) + (255,))
    d.rectangle([3 * SS, 3 * SS, 13 * SS, 13 * SS], outline=(40, 32, 22, 255))
    d.line([(3 * SS + SS, 13 * SS - SS), (13 * SS - SS, 13 * SS - SS)], fill=(30, 24, 16, 255))
    # 族色立条(左)
    d.rectangle([4 * SS, 4 * SS, 5 * SS, 12 * SS], fill=col + (255,))
    d.rectangle([4 * SS, 4 * SS, 4 * SS, 12 * SS], fill=glow_color(hi, 1.1) + (255,))
    # 散热缝
    for yy in (6, 8, 10):
        d.rectangle([7 * SS, yy * SS, 12 * SS, yy * SS + SS - 1], fill=(34, 28, 18, 255))
    for cx, cy in [(2 * SS, 2 * SS), (14 * SS, 2 * SS), (2 * SS, 14 * SS), (14 * SS, 14 * SS)]:
        rivet(d, cx, cy)
    return im


def tex_top(col, hi):
    im = Image.new("RGBA", (S, S))
    d = ImageDraw.Draw(im)
    base_panel(d)
    # 排热格栅(井字)
    for yy in range(3 * SS, 13 * SS, 2 * SS):
        d.line([(3 * SS, yy), (13 * SS - 1, yy)], fill=(44, 36, 24, 255))
    for xx in range(3 * SS, 13 * SS, 3 * SS):
        d.line([(xx, 3 * SS), (xx, 13 * SS - 1)], fill=(52, 44, 30, 255))
    # 中央族色谐振环
    cx = cy = 8 * SS
    r = 4 * SS
    d.ellipse([cx - r, cy - r, cx + r, cy + r], outline=col + (255,), width=SS)
    d.ellipse([cx - r + SS, cy - r + SS, cx + r - SS, cy + r - SS], outline=glow_color(hi, 1.15) + (255,))
    d.ellipse([cx - SS, cy - SS, cx + SS, cy + SS], fill=glow_color(hi, 1.3) + (255,))
    for cxx, cyy in [(2 * SS, 2 * SS), (14 * SS, 2 * SS), (2 * SS, 14 * SS), (14 * SS, 14 * SS)]:
        rivet(d, cxx, cyy)
    return im


def tex_core_front(col, hi):
    """巨物核心：金壳 + 中央谐振晶体窗。"""
    im = tex_front(col, hi)
    d = ImageDraw.Draw(im)
    # 覆盖中央为晶体
    x0, y0, x1, y1 = 5 * SS, 4 * SS, 11 * SS, 12 * SS
    d.polygon([(8 * SS, y0), (x1, 7 * SS), (8 * SS, y1), (x0, 7 * SS)], fill=hi + (255,))
    d.polygon([(8 * SS, y0 + SS), (x1 - SS, 7 * SS), (8 * SS, y1 - SS), (x0 + SS, 7 * SS)],
              outline=glow_color(hi, 0.7) + (255,))
    d.line([(8 * SS - 2 * SS, 7 * SS - SS), (8 * SS, y0 + SS)], fill=(255, 255, 255, 200))
    return im


def save16(im, name):
    im = im.resize((16, 16), Image.LANCZOS)
    im.save(os.path.join(TEX, name + ".png"))


def build():
    os.makedirs(TEX, exist_ok=True)
    n = 0
    for name, (col, hi) in VARIANTS.items():
        if name == "giant_core":
            save16(tex_core_front(col, hi), f"{name}_front"); n += 1
            save16(tex_side(col, hi), f"{name}_side"); n += 1
            save16(tex_top(col, hi), f"{name}_top"); n += 1
        else:
            save16(tex_front(col, hi), f"{name}_front"); n += 1
            save16(tex_side(col, hi), f"{name}_side"); n += 1
            save16(tex_top(col, hi), f"{name}_top"); n += 1
        # 清理旧单贴图(避免陈旧资产滞留 jar)
        old = os.path.join(TEX, f"{name}.png")
        if os.path.exists(old):
            os.remove(old)
    print(f"✓ 巨物方块贴图 v3 ×{n} (6 控制器 + 巨物核心, 三面/个)")


if __name__ == "__main__":
    build()
