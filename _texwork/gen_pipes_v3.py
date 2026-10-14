#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
管道贴图 v3 —— 261012规划 T6：三层明暗金属(受光/固有色/背光) + 青色系点缀。
产物: textures/block/{gas,fluid}_pipe_{center,arm,flange}.png (16×16, 文件名不变→模型零改动)
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
TEX = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "fieldworks", "textures", "block")

FAMILIES = {
    # 261014 T2 三族：气液合并为流体管(蓝) + 物品管(琥珀橙) + 能量管(青)
    "fluid_pipe": (58, 110, 190),
    "item_pipe": (232, 163, 61),
    "energy_pipe": (30, 158, 150),
}

def shell_shade(d, x0, y0, x1, y1, horizontal=True):
    """金属壳横向圆柱明暗(受光左/固有色/背光右 + 顶部高光线)。"""
    w = x1 - x0
    cols = []
    for x in range(w):
        t = x / max(1, w - 1)              # 0..1
        f = 0.62 + 0.55 * math_like(t)     # 亮→暗
        cols.append(f)
    for x in range(w):
        f = cols[x]
        base = int(96 * f) if not horizontal else int(96 * f)
        r, g, b = int(base * 1.02), int(base * 1.04), int(base * 1.10)
        d.line([(x0 + x, y0), (x0 + x, y1)], fill=(r, g, b, 255))
    # 高光线(左 1/4 处)与暗缘
    hx = x0 + max(1, w // 4)
    d.line([(hx, y0), (hx, y1)], fill=(190, 200, 210, 235), width=1)
    d.line([(x1 - 1, y0), (x1 - 1, y1)], fill=(38, 42, 50, 255), width=1)
    d.line([(x0, y0), (x1 - 1, y0)], fill=(140, 148, 160, 255))
    d.line([(x0, y1), (x1 - 1, y1)], fill=(40, 44, 52, 255))

def math_like(t):
    """圆柱明暗曲线: 中央偏左最亮。"""
    return (1 - (2 * t - 0.7) ** 2) ** 0.5 if abs(2 * t - 0.7) < 1 else 0.15

def build():
    for fam, col in FAMILIES.items():
        # ── center: 端面(圆角方截面 + 家族色环 + 深孔) ──
        im = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(im, "RGBA")
        d.rounded_rectangle([3, 3, 12, 12], 3, fill=(92, 99, 110, 255), outline=(44, 48, 56, 255))
        d.rounded_rectangle([4, 4, 11, 11], 2, outline=(150, 158, 170, 255))
        # 底缘阴影
        d.line([(4, 11), (11, 11)], fill=(52, 57, 66, 255))
        # 家族色环 + 内孔
        d.ellipse([5, 5, 10, 10], outline=col + (255,), width=1)
        d.ellipse([6, 6, 9, 9], fill=(col[0] // 3, col[1] // 3, col[2] // 3, 255))
        d.point((7, 7), fill=(min(255, col[0] + 90), min(255, col[1] + 90), min(255, col[2] + 90), 230))
        im.save(os.path.join(TEX, fam + "_center.png"))

        # ── arm: 侧面(横圆柱明暗 + 家族色轴向条纹) ──
        arm = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(arm, "RGBA")
        shell_shade(d, 5, 0, 11, 16)
        d.line([(8, 0), (8, 15)], fill=col + (215,), width=1)
        d.line([(6, 0), (6, 15)], fill=(120, 128, 140, 160), width=1)
        arm.save(os.path.join(TEX, fam + "_arm.png"))

        # ── flange: 法兰端板(加厚 + 螺栓点 + 家族色圈) ──
        fl = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        d = ImageDraw.Draw(fl, "RGBA")
        d.rectangle([0, 0, 15, 15], fill=(88, 95, 106, 255))
        d.rectangle([0, 0, 15, 1], fill=(152, 160, 172, 255))
        d.rectangle([0, 1, 15, 2], fill=(120, 128, 140, 255))
        d.rectangle([0, 13, 15, 15], fill=(46, 50, 58, 255))
        d.rectangle([0, 12, 15, 12], fill=(66, 71, 80, 255))
        d.rectangle([0, 0, 1, 15], fill=(126, 134, 146, 255))
        d.rectangle([14, 0, 15, 15], fill=(58, 63, 72, 255))
        # 四角螺栓
        for bx, by in [(2, 2), (12, 2), (2, 12), (12, 12)]:
            d.ellipse([bx, by, bx + 2, by + 2], fill=(58, 63, 72, 255), outline=(160, 168, 180, 255))
        # 中心家族色 + 内孔
        d.ellipse([4, 4, 11, 11], outline=col + (255,))
        d.ellipse([6, 6, 9, 9], fill=(col[0] // 3, col[1] // 3, col[2] // 3, 255))
        d.point((7, 7), fill=(min(255, col[0] + 90), min(255, col[1] + 90), min(255, col[2] + 90), 230))
        fl.save(os.path.join(TEX, fam + "_flange.png"))
    print(f"✓ 管道贴图 v3 (center/arm/flange × {len(FAMILIES)} 族)")

if __name__ == "__main__":
    build()
