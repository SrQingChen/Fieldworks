#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
README 立体徽章 logo 生成器(261014 T12)：
六边形"场徽"盾牌——青铜外环 + 青色场芯 + 双轨道环 + 中央谐振晶体 + 辉光 + 投影。
2048 渲染 → 512 输出(抗锯齿)。产物 docs/logo.png
"""
import math
import os
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT_DIR = os.path.join(ROOT, "..", "docs")
S = 2048
CX = CY = S // 2

BRONZE = (176, 138, 82)
BRONZE_L = (232, 200, 140)
BRONZE_D = (94, 70, 38)
TEAL = (30, 158, 150)
TEAL_L = (127, 216, 208)
TEAL_D = (10, 78, 74)
INK = (24, 20, 16)


def hexagon(cx, cy, r, rot=math.pi / 6):
    return [(cx + r * math.cos(rot + math.pi / 3 * i), cy + r * math.sin(rot + math.pi / 3 * i)) for i in range(6)]


def build():
    os.makedirs(OUT_DIR, exist_ok=True)
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im, "RGBA")

    # ── 投影(立体感之源：徽章悬浮) ──
    sh = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    ds = ImageDraw.Draw(sh)
    ds.polygon(hexagon(CX + 26, CY + 64, 780), fill=(8, 10, 14, 190))
    sh = sh.filter(ImageFilter.GaussianBlur(46))
    im.alpha_composite(sh)

    # ── 外环斜面(青铜渐变 + 左上受光) ──
    R_OUT, R_IN = 780, 640
    for i, rr in enumerate(range(R_OUT, R_IN, -4)):
        t = i / (R_OUT - R_IN)
        # 环向明暗 + 径向斜面：左上亮、右下暗
        col = tuple(int(a + (b - a) * t) for a, b in zip(BRONZE_L, BRONZE_D))
        d.polygon(hexagon(CX, CY, rr), fill=col + (255,))

    # 环向受光(左上弧提亮 / 右下压暗)
    light = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    dl = ImageDraw.Draw(light)
    dl.polygon(hexagon(CX - 60, CY - 60, R_OUT - 8), fill=(255, 240, 210, 90))
    dl.polygon(hexagon(CX, CY, R_IN + 26), fill=(0, 0, 0, 0))
    dark = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    dd = ImageDraw.Draw(dark)
    dd.polygon(hexagon(CX + 70, CY + 70, R_OUT - 8), fill=(20, 12, 4, 120))
    dd.polygon(hexagon(CX, CY, R_IN + 26), fill=(0, 0, 0, 0))
    im.alpha_composite(light.filter(ImageFilter.GaussianBlur(18)))
    im.alpha_composite(dark.filter(ImageFilter.GaussianBlur(18)))

    # ── 内芯(青色场域径向渐变) ──
    core = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    dc = ImageDraw.Draw(core)
    dc.polygon(hexagon(CX, CY, R_IN), fill=TEAL_D + (255,))
    for rr in range(R_IN, 0, -6):
        t = 1 - rr / R_IN
        col = tuple(int(a + (b - a) * t) for a, b in zip(TEAL_D, TEAL))
        dc.polygon(hexagon(CX, CY, rr), fill=col + (255,))
    im.alpha_composite(core)

    # 场域辉光(中心亮核 + 上部高光)
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    dg = ImageDraw.Draw(glow)
    dg.ellipse([CX - 300, CY - 300, CX + 300, CY + 300], fill=TEAL_L + (110,))
    dg.ellipse([CX - 130, CY - 190, CX + 260, CY + 190], fill=(210, 255, 250, 70))
    im.alpha_composite(glow.filter(ImageFilter.GaussianBlur(70)))

    # ── 双轨道环(能量在环内流动) ──
    ring = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    dr = ImageDraw.Draw(ring)
    r1, r2 = 560, 470
    dr.ellipse([CX - r1, CY - r1, CX + r1, CY + r1], outline=(220, 250, 246, 235), width=16)
    dr.ellipse([CX - r2, CY - r2 * 0.52, CX + r2, CY + r2 * 0.52], outline=(127, 216, 208, 210), width=12)
    # 轨道亮珠(左上受光珠 + 右下暗珠 → 旋转感)
    for ang, sz, al in [(0.8, 46, 255), (2.4, 30, 200), (4.2, 36, 230)]:
        bx, by = CX + r1 * math.cos(ang), CY + r1 * math.sin(ang)
        dr.ellipse([bx - sz, by - sz, bx + sz, by + sz], fill=(235, 255, 252, al))
    im.alpha_composite(ring.filter(ImageFilter.GaussianBlur(2)))

    # ── 中央谐振晶体(六棱锥两层) ──
    cry = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    dcr = ImageDraw.Draw(cry)
    top, bot = CY - 330, CY + 330
    for (x0, x1, y0, y1, col) in [
        (CX - 200, CX, top, bot, (170, 240, 234, 255)),       # 左面(受光)
        (CX, CX + 200, top, bot, (58, 158, 150, 255)),        # 右面(背光)
        (CX - 90, CX, top, bot - 0, (226, 255, 252, 255)),    # 左内棱(高光)
    ]:
        dcr.polygon([(x0, bot), (x1, bot), ((x0 + x1) / 2 + (x1 - x0) * 0.18, top)], fill=col)
    # 晶体棱线
    dcr.line([(CX - 200, bot), (CX - 118, top)], fill=(255, 255, 255, 170), width=8)
    dcr.line([(CX + 200, bot), (CX + 118, top)], fill=(10, 60, 56, 200), width=8)
    im.alpha_composite(cry)
    # 晶体辉光
    cg = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    dcg = ImageDraw.Draw(cg)
    dcg.polygon([(CX - 210, bot), (CX + 210, bot), (CX, top)], fill=(120, 240, 230, 90))
    im.alpha_composite(cg.filter(ImageFilter.GaussianBlur(42)))

    # ── 六角螺栓(外环六角，左上亮右下暗) ──
    d = ImageDraw.Draw(im, "RGBA")
    for i in range(6):
        ang = math.pi / 6 + math.pi / 3 * i
        bx, by = CX + (R_OUT + R_IN) / 2 * math.cos(ang), CY + (R_OUT + R_IN) / 2 * math.sin(ang)
        lit = math.cos(ang - math.pi * 1.25)  # 左上受光
        base = int(120 + 70 * lit)
        d.polygon(hexagon(bx, by, 44, rot=0), fill=(base, int(base * 0.92), int(base * 0.72), 255))
        d.polygon(hexagon(bx - 8 * lit, by - 8 * lit, 22, rot=0), fill=(min(255, base + 70),) * 2 + (int((base + 70) * 0.8), 255))

    # ── 底部铭文刻线(场学符号) ──
    d.line([(CX - 330, CY + 520), (CX + 330, CY + 520)], fill=(200, 235, 230, 140), width=10)
    for i in range(5):
        x = CX - 240 + i * 120
        d.line([(x, CY + 480), (x, CY + 560)], fill=(200, 235, 230, 110), width=8)

    out = im.resize((512, 512), Image.LANCZOS)
    out.save(os.path.join(OUT_DIR, "logo.png"))
    # 横幅版(徽章 + 右侧标题底板)
    banner = Image.new("RGBA", (1600, 512), (0, 0, 0, 0))
    banner.alpha_composite(out, (40, 0))
    banner.save(os.path.join(OUT_DIR, "logo_banner.png"))
    print("✓ docs/logo.png + logo_banner.png")


if __name__ == "__main__":
    build()
