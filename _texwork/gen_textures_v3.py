#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
物品贴图生成器 v3 —— 261012开发规划 T5。
- 矿物×2 + 锭类×10：保留既有 Faithful 变色基底，叠加"优美花纹"(拉丝/铭刻涡纹/棱线/晶簇闪点)。
- 其余 51 张：**全新程序化绘制**(numpy 高度场 Lambert 光照 + 4x 超采样)，不复用任何 Faithful 像素。
产物: assets/fieldworks/textures/item/*.png (64×64) + preview_items_v3.png
"""
import os, math
import numpy as np
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "fieldworks", "textures", "item")

SS = 4
S = 64 * SS                      # 256 渲染画布
Y, X = np.mgrid[0:S, 0:S].astype(np.float32)

LIGHT = np.array([-0.45, -0.62, 0.65], dtype=np.float32)
LIGHT /= np.linalg.norm(LIGHT)

def hex2rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i+2], 16) for i in (0, 2, 4)], dtype=np.float32)

def shade(height, base, ambient=0.42, diffuse=0.75, spec=0.0, spec_pow=16, light=LIGHT):
    """高度场→法线→Lambert+高光。height: HxW (0=空, >0 高度)。返回 RGB float。"""
    gy, gx = np.gradient(height)
    n = np.stack([-gx * 8, -gy * 8, np.ones_like(height)], axis=-1)
    n /= (np.linalg.norm(n, axis=-1, keepdims=True) + 1e-6)
    ndl = np.clip(n @ light, 0, 1)
    rgb = base[None, None, :] * (ambient + diffuse * ndl[..., None])
    if spec > 0:
        h = n @ light
        s = np.clip(h - 0.92, 0, 1) ** spec_pow
        rgb = rgb + s[..., None] * spec * 255.0
    return rgb

def comp(layers):
    """合成 [(mask,rgb,alpha_mult)] → RGBA uint8 (256) → 64px。"""
    out = np.zeros((S, S, 4), dtype=np.float32)
    for mask, rgb, am in layers:
        m = (mask & (rgb[..., :1] if rgb.shape[-1] == 4 else 1) > -1) if False else mask
        a = np.clip((rgb[..., 3] if rgb.shape[-1] == 4 else 255.0) * am, 0, 255) if rgb.shape[-1] == 4 else np.full(mask.shape, 255.0 * am)
        a = np.where(mask, a, 0)
        al = a / 255.0
        out[..., :3] = out[..., :3] * (1 - al[..., None]) + rgb[..., :3] * al[..., None]
        out[..., 3] = out[..., 3] * (1 - al) + a
    img = np.clip(out, 0, 255).astype(np.uint8)
    return Image.fromarray(img, "RGBA").resize((64, 64), Image.LANCZOS)

def solid(rgb_or_hex):
    c = hex2rgb(rgb_or_hex) if isinstance(rgb_or_hex, str) else np.asarray(rgb_or_hex, dtype=np.float32)
    arr = np.zeros((S, S, 3), dtype=np.float32)
    arr[:, :] = c
    return arr

def dist(cx, cy):
    return np.hypot(X - cx, Y - cy)

def save(img, name):
    """保存 + 明暗对比保障(保色相, 只拉伸亮度: 阴影更深/高光更亮 → 立体感)。"""
    a = np.array(img).astype(np.float32)
    m = a[..., 3] > 40
    if m.any():
        luma = a[..., :3].mean(axis=2)
        vis = luma[m]
        p10, p90 = np.percentile(vis, 10), np.percentile(vis, 90)
        if p90 - p10 < 62:
            tgt_hi = min(236.0, p90 + 82)
            tgt_lo = max(8.0, p10 * 0.58)
            scale = (tgt_hi - tgt_lo) / max(1e-3, p90 - p10)
            l = tgt_lo + (luma - p10) * scale
            f = np.clip(l / np.maximum(luma, 1.0), 0.18, 3.6)
            a[..., :3] = np.clip(a[..., :3] * f[..., None], 0, 255)
            img = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    img.save(os.path.join(OUT, name + ".png"))

# ═══════════════ 引擎级形状(高度场) ═══════════════

def h_gear(cx, cy, r_out, r_hub, bore, teeth, tooth_h):
    h = np.zeros((S, S), dtype=np.float32)
    ring = (dist(cx, cy) < r_out) & (dist(cx, cy) > r_hub)
    h[ring] = 1.0
    ang = np.arctan2(Y - cy, X - cx)
    seg = (ang / (2 * math.pi) * teeth) % 1.0
    tooth_band = dist(cx, cy) < r_out + tooth_h
    on_tooth = tooth_band & (np.abs(seg - 0.5) < 0.24) & (dist(cx, cy) > r_out - tooth_h * 0.4)
    h[on_tooth] = 1.35
    hub = dist(cx, cy) < r_hub
    h[hub] = 1.5
    h[dist(cx, cy) < bore] = 0.0
    # 减重孔(4 个)
    for a in [math.pi / 4, 3 * math.pi / 4, 5 * math.pi / 4, 7 * math.pi / 4]:
        hx, hy = cx + math.cos(a) * (r_hub + r_out) / 2, cy + math.sin(a) * (r_hub + r_out) / 2
        h[dist(hx, hy) < (r_out - r_hub) * 0.16] = 0.0
    return h

def h_cyl(cx, cy, rx, ry, y0, y1, top=1.4, side=1.0):
    """竖圆柱: 侧面椭圆拉伸。"""
    h = np.zeros((S, S), dtype=np.float32)
    inside = ((X - cx) / rx) ** 2 + 0 < 1
    yy = np.clip(Y, y0, y1)
    d = ((X - cx) / rx) ** 2 + ((Y - yy) / 10) ** 2
    side_m = ((X - cx) / rx) ** 2 <= 1
    h[side_m & (Y >= y0) & (Y <= y1)] = side
    top_m = ((X - cx) / rx) ** 2 + ((Y - y0) / ry) ** 2 <= 1
    h[top_m] = top
    bot_m = ((X - cx) / rx) ** 2 + ((Y - y1) / ry) ** 2 <= 1
    h[bot_m] = side * 0.9
    return h

def h_box(x0, y0, x1, y1, hh=1.2, r=6):
    h = np.zeros((S, S), dtype=np.float32)
    m = (X >= x0) & (X <= x1) & (Y >= y0) & (Y <= y1)
    h[m] = hh
    # 圆角
    for cx_, cy_ in [(x0 + r, y0 + r), (x1 - r, y0 + r), (x0 + r, y1 - r), (x1 - r, y1 - r)]:
        corner = (X < x0 + r) | (X > x1 - r) if False else None
    rr = np.maximum(np.maximum(x0 + r - X, X - (x1 - r)), 0) ** 2 + np.maximum(np.maximum(y0 + r - Y, Y - (y1 - r)), 0) ** 2
    h[rr > r * r] = 0
    return h

def h_dome(cx, cy, rx, ry, hh=1.2):
    d = ((X - cx) / rx) ** 2 + ((Y - cy) / ry) ** 2
    return np.clip(1 - d, 0, 1) * hh * (d < 1)

def h_torus(cx, cy, r_out, r_in, hh=1.2):
    d = dist(cx, cy)
    m = (d < r_out) & (d > r_in)
    h = np.zeros((S, S), dtype=np.float32)
    h[m] = hh
    # 环面圆度: 中心高
    mid = (r_out + r_in) / 2
    h[m] = hh * (1.0 - 0.45 * np.abs(d[m] - mid) / ((r_out - r_in) / 2))
    return h

def ring_glow(cx, cy, r, w, color, alpha=110):
    """发光环(装饰层, 直接 RGB+alpha)。"""
    d = np.abs(dist(cx, cy) - r)
    m = d < w
    arr = np.zeros((S, S, 4), dtype=np.float32)
    arr[..., :3] = color
    arr[..., 3] = np.where(m, alpha * (1 - d / w), 0)
    return m, arr, 1.0

def blob_noise(rng, cx, cy, n, r_lo, r_hi):
    m = np.zeros((S, S), dtype=bool)
    for _ in range(n):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(0, 1) ** 0.5
        bx, by = cx + math.cos(a) * rr * r_hi * 1.5, cy + math.sin(a) * rr * r_hi * 1.2
        m |= dist(bx, by) < rng.uniform(r_lo, r_hi)
    return m

def pile(rng, cx, cy, rx, ry, color, n=14, spec=0.5):
    """粉末/碎晶堆: 若干球堆叠 + 底部大椭圆。"""
    h = h_dome(cx, cy, rx, ry, 1.0)
    for _ in range(n):
        a = rng.uniform(0, 2 * math.pi)
        rr = rng.uniform(0, 1) ** 0.6
        bx, by = cx + math.cos(a) * rr * rx * 0.7, cy + math.sin(a) * rr * ry * 0.7
        h = np.maximum(h, h_dome(bx, by, rng.uniform(10, 22), rng.uniform(8, 18), rng.uniform(0.9, 1.5)))
    return shade(h, hex2rgb(color), spec=spec), h > 0.02

def crystal_spikes(cx, cy, base_col, spikes, rng, glow_col=None, glow_a=60):
    """晶簇: 每晶一个多边形面组(亮/中/暗三面)。"""
    layers = []
    for (ang, ln, wd, hh) in spikes:
        for k, (off, col_f) in enumerate([(0, 1.18), (0.5, 1.0), (1.0, 0.72)]):
            pts_mask = np.zeros((S, S), dtype=bool)
            tipx, tipy = cx + math.cos(ang) * ln, cy + math.sin(ang) * ln
            perp = ang + math.pi / 2
            p1 = (cx + math.cos(perp) * wd, cy + math.sin(perp) * wd)
            p2 = (cx - math.cos(perp) * wd, cy - math.sin(perp) * wd)
            p3 = (tipx - math.cos(perp) * wd * off, tipy - math.sin(perp) * wd * off)
            img = Image.new("L", (S, S), 0)
            dr = ImageDraw.Draw(img)
            dr.polygon([p1, p2, p3], fill=255)
            m = np.array(img) > 128
            h = np.zeros((S, S), dtype=np.float32)
            h[m] = hh
            col = hex2rgb(base_col) * col_f
            layers.append((m, shade(h, col, ambient=0.5, diffuse=0.7, spec=0.4), 1.0))
    if glow_col is not None:
        g = np.zeros((S, S, 4), dtype=np.float32)
        d = dist(cx, cy)
        g[..., :3] = hex2rgb(glow_col)
        g[..., 3] = np.clip(1 - d / 110, 0, 1) * glow_a
        layers.append((np.ones((S, S), dtype=bool), g, 1.0))
    return layers

# ═══════════════ A. 矿物/锭类: 既有基底 + 花纹 ═══════════════

def ingot_pattern(name, base_hex, emblem=True):
    im = Image.open(os.path.join(OUT, name + ".png")).convert("RGBA")
    if im.size != (64, 64):
        im = im.resize((64, 64), Image.LANCZOS)   # v1 基底为 16×16，统一升到 64
    a = np.array(im).astype(np.float32)
    mask = a[..., 3] > 40
    lum = a[..., :3].mean(axis=2)
    # 1) 斜向拉丝(±8 亮度, 3px 周期)
    stripe = ((X[::1, ::1] * 0 + (np.arange(64)[:, None] * 0)) if False else None)
    yy = np.arange(64)
    s64 = np.sin(yy[:, None] * 0 + np.arange(64)[None, :] * 0.55 + 1.2)  # 64×64 拉丝
    s64 = ((s64 > 0.55) & (s64 < 0.75)).astype(np.float32) * 16 - 8
    for c in range(3):
        a[..., c] = np.where(mask, np.clip(a[..., c] + s64, 0, 255), a[..., c])
    # 2) 顶缘高光/底缘阴影(每行首末可见像素)
    for row in range(64):
        cols = np.where(mask[row])[0]
        if len(cols) == 0:
            continue
        for cc, f in [(cols[0], 1.16), (cols[-1], 0.84)]:
            a[row, cc, :3] = np.clip(a[row, cc, :3] * f, 0, 255)
        a[row, cols[0], :3] = np.clip(a[row, cols[0], :3] * 1.06, 0, 255)
    # 3) 中央铭刻: 菱形纹 + 涡纹角饰(深色描线, 64 网格)
    y64, x64 = np.mgrid[0:64, 0:64].astype(np.float32)
    dist64 = lambda cx, cy: np.hypot(x64 - cx, y64 - cy)
    dark = hex2rgb(base_hex) * 0.55
    em = np.zeros((64, 64, 4), dtype=np.float32)
    em[..., :3] = dark
    dia = (np.abs(x64 - 32) + np.abs(y64 - 30) < 7) & (np.abs(x64 - 32) + np.abs(y64 - 30) > 4)
    em[..., 3] = np.where(dia, 200, 0)
    # 涡纹角饰(左上/右下两道弧)
    ang1 = np.abs(np.arctan2(y64 - 12, x64 - 14) - 0.6) < 0.12
    r1 = np.abs(dist64(14, 12) - 7) < 1.1
    ang2 = np.abs(np.arctan2(y64 - 52, x64 - 50) - 0.6 + math.pi) < 0.12
    r2 = np.abs(dist64(50, 52) - 7) < 1.1
    swirl = (ang1 & r1) | (ang2 & r2)
    em[..., 3] = np.where(swirl, 170, em[..., 3])
    al = em[..., 3:4] / 255.0 * mask[..., None]
    a[..., :3] = a[..., :3] * (1 - al) + em[..., :3] * al
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")

def ore_glints(name, glint=(255, 255, 255)):
    im = Image.open(os.path.join(OUT, name + ".png")).convert("RGBA")
    if im.size != (64, 64):
        im = im.resize((64, 64), Image.LANCZOS)   # v1 基底为 16×16，统一升到 64
    a = np.array(im).astype(np.float32)
    mask = a[..., 3] > 40
    rng = np.random.default_rng(7)
    for _ in range(9):
        bx, by = rng.integers(14, 50), rng.integers(14, 50)
        if not mask[by, bx]:
            continue
        r = int(rng.integers(1, 3))
        yy0, yy1 = max(0, by - r - 1), min(64, by + r + 2)
        xx0, xx1 = max(0, bx - r - 1), min(64, bx + r + 2)
        reg = a[yy0:yy1, xx0:xx1]
        m = mask[yy0:yy1, xx0:xx1]
        reg[..., :3] = np.where(m[..., None], np.clip(reg[..., :3] * 1.7 + 40, 0, 255), reg[..., :3])
    # 暗部包络(底部 1/3 压暗)
    shade_v = np.clip((np.arange(64)[:, None] - 40) / 24, 0, 1) * 0.28
    a[..., :3] = np.where(mask[..., None], a[..., :3] * (1 - shade_v), a[..., :3])
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")

# ═══════════════ B. 全新绘制(51 张) ═══════════════

STEEL = "#9AA5AD"
DARK_STEEL = "#5A636B"
COPPER = "#C87533"
TEAL = "#1E9E96"

def t_gear():
    h = h_gear(128, 128, 88, 34, 14, 8, 20)
    m = h > 0
    rgb = shade(h, hex2rgb(STEEL), ambient=0.38, diffuse=0.78, spec=0.5)
    # 拉丝: 切向条纹
    ang = np.arctan2(Y - 128, X - 128)
    brush = (np.sin(ang * 40) * 6)[..., None]
    rgb = rgb + brush * m[..., None]
    layers = [(m, rgb, 1.0)]
    # 中轴黄铜衬套
    hub = dist(128, 128) < 34
    bore = dist(128, 128) < 14
    bush = hub & ~bore
    rgb2 = shade(np.where(bush, 1.6, 0), hex2rgb("#B08A3E"), ambient=0.4, diffuse=0.75, spec=0.55)
    layers.append((bush, rgb2, 1.0))
    save(comp(layers), "steel_gear")

def t_coil():
    h = h_torus(128, 132, 86, 44, 1.2)
    # 绕线纹理: 径向波纹
    d = dist(128, 132)
    h = h + np.where(h > 0, np.sin(d * 1.5) * 0.24, 0)
    m = h > 0
    rgb = shade(h, hex2rgb(COPPER), ambient=0.4, diffuse=0.72, spec=0.65)
    save(comp([(m, rgb, 1.0)]), "copper_coil")

def t_field_wire():
    layers = []
    h = h_torus(128, 136, 82, 40, 1.1) + np.where(h_torus(128, 136, 82, 40, 1.1) > 0,
                                                  np.sin(dist(128, 136) * 1.7) * 0.12, 0)
    m = h > 0
    rgb = shade(h, hex2rgb("#3E464E"), ambient=0.42, diffuse=0.7)
    layers.append((m, rgb, 1.0))
    # 青色绝缘条纹(环向 4 道)
    angm = np.abs(np.arctan2(Y - 136, X - 128) % (math.pi / 2) - math.pi / 4) < 0.1
    stripe = m & angm & (dist(128, 136) > 44)
    sm = np.zeros((S, S), dtype=np.float32)
    sm[stripe] = 1.3
    layers.append((stripe, shade(sm, hex2rgb(TEAL), ambient=0.5, diffuse=0.7, spec=0.4), 1.0))
    save(comp(layers), "field_wire")

def t_superconductor_ring():
    h = h_torus(128, 128, 80, 50, 1.3)
    m = h > 0
    rgb = shade(h, hex2rgb(DARK_STEEL), ambient=0.4, diffuse=0.7, spec=0.4)
    layers = [(m, rgb, 1.0), ring_glow(128, 128, 65, 7, hex2rgb("#7FD8D0"), 150)]
    save(comp(layers), "superconductor_ring")

def t_turbine_blade():
    layers = []
    # 翼型: 两条弧夹成叶片, 上端窄
    for (x0, y0, ln, wd, col, hh) in [(128, 190, 128, 44, "#B9C4CC", 1.2)]:
        img = Image.new("L", (S, S), 0)
        dr = ImageDraw.Draw(img)
        dr.polygon([(x0 - wd, y0), (x0 + wd * 0.7, y0), (x0 + 8, y0 - ln * 0.72), (x0 - 4, y0 - ln)], fill=255)
        m = np.array(img) > 128
        # 翼面弧高
        t_ = np.clip((y0 - Y) / ln, 0, 1)
        hh_map = np.where(m, 1.0 + 0.5 * np.sin(t_ * math.pi) - 0.4 * np.clip((X - x0) / wd, -1, 1), 0)
        rgb = shade(hh_map, hex2rgb(col), ambient=0.4, diffuse=0.75, spec=0.7)
        layers.append((m, rgb, 1.0))
    # 叶根榫接(深色梯形)
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    dr.polygon([(128 - 40, 236), (128 + 40, 236), (128 + 30, 210), (128 - 30, 210)], fill=255)
    m2 = np.array(img) > 128
    hh2 = np.where(m2, 1.5, 0)
    layers.append((m2, shade(hh2, hex2rgb(DARK_STEEL), ambient=0.4, diffuse=0.7), 1.0))
    save(comp(layers), "turbine_blade")

def t_nozzle():
    layers = []
    # 锥形喷口(上宽下窄) + 法兰盘
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    dr.polygon([(128 - 52, 60), (128 + 52, 60), (128 + 22, 196), (128 - 22, 196)], fill=255)
    m = np.array(img) > 128
    t_ = np.clip((Y - 60) / 136, 0, 1)
    cx_col = 128 + (X - 128) * 0  # noqa
    hh = np.where(m, 1.4 - np.abs((X - 128) / (52 - 30 * t_)) * 0.5, 0)
    rgb = shade(hh, hex2rgb(STEEL), ambient=0.4, diffuse=0.75, spec=0.5)
    layers.append((m, rgb, 1.0))
    fl = h_box(60, 44, 196, 70, 1.8, 8)
    layers.append((fl > 0, shade(fl, hex2rgb(DARK_STEEL), ambient=0.42, diffuse=0.7), 1.0))
    thr = h_cyl(128, 196, 20, 8, 196, 226)
    layers.append((thr > 0, shade(thr, hex2rgb("#424A52"), ambient=0.4, diffuse=0.65), 1.0))
    save(comp(layers), "nozzle_assembly")

def t_piston_frame():
    h = h_box(44, 52, 212, 204, 1.3, 18)
    bore = dist(128, 128) < 46
    h[bore] = 0
    m = h > 0
    rgb = shade(h, hex2rgb(STEEL), ambient=0.42, diffuse=0.72, spec=0.35)
    layers = [(m, rgb, 1.0)]
    # 四角螺栓
    for bx, by in [(66, 74), (190, 74), (66, 182), (190, 182)]:
        b = h_dome(bx, by, 12, 12, 1.7)
        layers.append((b > 0.1, shade(b, hex2rgb("#77828B"), ambient=0.45, diffuse=0.7, spec=0.5), 1.0))
    # 缸壁内缘高光
    inner = (dist(128, 128) < 52) & (dist(128, 128) > 44)
    hh2 = np.zeros((S, S), dtype=np.float32)
    hh2[inner] = 1.6
    layers.append((inner, shade(hh2, hex2rgb("#C7D0D6"), ambient=0.5, diffuse=0.6, spec=0.6), 1.0))
    save(comp(layers), "piston_frame")

def t_bearing():
    layers = []
    outer = h_torus(128, 128, 84, 60, 1.5)
    layers.append((outer > 0, shade(outer, hex2rgb("#8E989F"), ambient=0.4, diffuse=0.75, spec=0.5), 1.0))
    inner = h_torus(128, 128, 34, 16, 1.7)
    layers.append((inner > 0, shade(inner, hex2rgb(STEEL), ambient=0.42, diffuse=0.75, spec=0.55), 1.0))
    # 滚珠 ×6
    for k in range(6):
        a = k * math.pi / 3 + 0.3
        bx, by = 128 + math.cos(a) * 47, 128 + math.sin(a) * 47
        ball = h_dome(bx, by, 12, 12, 1.9)
        layers.append((ball > 0.1, shade(ball, hex2rgb("#E8EDF0"), ambient=0.55, diffuse=0.6, spec=0.85, spec_pow=6), 1.0))
    save(comp(layers), "precision_bearing")

def t_logic_circuit():
    h = h_box(40, 76, 216, 182, 1.1, 10)
    layers = [(h > 0, shade(h, hex2rgb("#1E5A46"), ambient=0.5, diffuse=0.6), 1.0)]
    chip = h_box(96, 108, 160, 150, 1.7, 6)
    layers.append((chip > 0, shade(chip, hex2rgb("#20242B"), ambient=0.45, diffuse=0.7, spec=0.3), 1.0))
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    for y_ in range(84, 176, 12):
        dr.line([(46, y_), (96, y_)], fill=255, width=4)
        dr.line([(160, y_ + 6), (210, y_ + 6)], fill=255, width=4)
    tr = np.array(img) > 0
    trm = np.zeros((S, S), dtype=np.float32)
    trm[tr] = 1.5
    layers.append((tr, shade(trm, hex2rgb("#D9A728"), ambient=0.55, diffuse=0.5, spec=0.4), 1.0))
    # 焊点
    rng = np.random.default_rng(3)
    for _ in range(8):
        bx, by = rng.integers(50, 206), rng.integers(84, 174)
        dd = h_dome(bx, by, 5, 5, 1.8)
        layers.append((dd > 0.2, shade(dd, hex2rgb("#F2D06B"), ambient=0.6, diffuse=0.5, spec=0.8, spec_pow=4), 1.0))
    save(comp(layers), "logic_circuit")

def t_silicon_wafer():
    h = h_dome(128, 128, 88, 88, 1.3)
    rgb = shade(h, hex2rgb("#7E8CA6"), ambient=0.48, diffuse=0.62, spec=0.75, spec_pow=8)
    # 虹彩: 角度渐变色偏移
    ang = np.arctan2(Y - 128, X - 128)
    hue_shift = np.stack([np.sin(ang) * 30, np.sin(ang + 2) * 30, np.sin(ang + 4) * 30], axis=-1)
    rgb = rgb + hue_shift * (h > 0)[..., None]
    # 裸片网格
    gx = ((X - 44) // 22 * 22 + 44)
    grid = (np.abs(X - gx) < 1.4) | (np.abs(Y - ((Y - 44) // 22 * 22 + 44)) < 1.4)
    rgb = np.where((grid & (h > 0))[..., None], rgb * 0.75, rgb)
    save(comp([(h > 0, rgb, 1.0)]), "silicon_wafer")

def t_sensor_core():
    layers = []
    base = h_dome(128, 140, 74, 66, 1.2)
    layers.append((base > 0, shade(base, hex2rgb("#6E7980"), ambient=0.42, diffuse=0.72), 1.0))
    dome = h_dome(128, 128, 50, 48, 1.8)
    layers.append((dome > 0, shade(dome, hex2rgb(TEAL), ambient=0.5, diffuse=0.65, spec=0.7), 1.0))
    layers.append(ring_glow(128, 128, 60, 4, hex2rgb("#7FD8D0"), 120))
    # 三个触脚
    for a in [math.pi * 0.75, math.pi * 1.0, math.pi * 1.25]:
        bx, by = 128 + math.cos(a) * 74, 140 + math.sin(a) * 66
        p = h_box(bx - 7, by - 14, bx + 7, by + 14, 1.0, 4)
        layers.append((p > 0, shade(p, hex2rgb("#B7A24A"), ambient=0.5, diffuse=0.6, spec=0.4), 1.0))
    save(comp(layers), "sensor_core")

def t_electromagnet_core():
    layers = []
    # E 形铁芯
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    dr.rectangle([52, 60, 204, 80], fill=255)
    dr.rectangle([52, 60, 72, 200], fill=255)
    dr.rectangle([128 - 10, 60, 128 + 10, 200], fill=255)
    dr.rectangle([204 - 20, 60, 204, 200], fill=255)
    m = np.array(img) > 0
    h = np.where(m, 1.5, 0).astype(np.float32)
    layers.append((m, shade(h, hex2rgb("#8A8F96"), ambient=0.42, diffuse=0.72, spec=0.35), 1.0))
    # 铜绕组(中柱两段)
    for y0, y1 in [(100, 140), (150, 190)]:
        w = h_cyl(128, y0, 26, 10, y0, y1)
        wv = w + np.where(w > 0, np.sin(Y * 1.2) * 0.14, 0)
        layers.append((wv > 0, shade(wv, hex2rgb(COPPER), ambient=0.42, diffuse=0.7, spec=0.6), 1.0))
    save(comp(layers), "electromagnet_core")

def t_magnetized(name, strong=False):
    layers = []
    h = h_box(56, 100, 200, 168, 1.5, 14)
    m = h > 0
    rgb = shade(h, hex2rgb("#5E6870"), ambient=0.42, diffuse=0.72, spec=0.3)
    layers.append((m, rgb, 1.0))
    # N/S 极色端
    north = m & (X < 88)
    south = m & (X > 168)
    hn = np.where(north, 1.8, 0).astype(np.float32)
    hs = np.where(south, 1.8, 0).astype(np.float32)
    layers.append((north, shade(hn, hex2rgb("#C0392B"), ambient=0.5, diffuse=0.6), 1.0))
    layers.append((south, shade(hs, hex2rgb("#2E6BD6"), ambient=0.5, diffuse=0.6), 1.0))
    if strong:
        layers.append(ring_glow(128, 134, 78, 9, hex2rgb("#7FD8D0"), 130))
    save(comp(layers), name)

def t_distillation_tray():
    layers = []
    tray = h_cyl(128, 200, 84, 14, 176, 216)
    layers.append((tray > 0, shade(tray, hex2rgb(STEEL), ambient=0.42, diffuse=0.72), 1.0))
    # 泡罩 ×3
    for bx in [80, 128, 176]:
        cap = h_cyl(bx, 150, 22, 8, 120, 176)
        layers.append((cap > 0, shade(cap, hex2rgb("#77828B"), ambient=0.42, diffuse=0.7, spec=0.45), 1.0))
        dome = h_dome(bx, 120, 20, 10, 1.6)
        layers.append((dome > 0, shade(dome, hex2rgb("#98A3AB"), ambient=0.45, diffuse=0.7, spec=0.4), 1.0))
    save(comp(layers), "distillation_tray")

def t_seal_flange():
    layers = []
    fl = h_dome(128, 128, 84, 84, 1.2)
    bore = dist(128, 128) < 34
    fl[bore] = 0
    layers.append((fl > 0, shade(fl, hex2rgb("#8E989F"), ambient=0.42, diffuse=0.72, spec=0.4), 1.0))
    for k in range(8):
        a = k * math.pi / 4
        bx, by = 128 + math.cos(a) * 62, 128 + math.sin(a) * 62
        b = h_dome(bx, by, 9, 9, 1.8)
        layers.append((b > 0.1, shade(b, hex2rgb(DARK_STEEL), ambient=0.45, diffuse=0.7, spec=0.5), 1.0))
    g = h_torus(128, 128, 44, 36, 1.5)
    layers.append((g > 0, shade(g, hex2rgb("#3A4046"), ambient=0.45, diffuse=0.6), 1.0))
    save(comp(layers), "seal_flange")

def t_resonator_shell():
    layers = []
    shell = h_dome(128, 120, 82, 78, 1.3)
    inner = dist(128, 130) < 46 & np.zeros_like(dist(128, 128), dtype=bool) if False else (dist(128, 132) < 44)
    hh = shell.copy()
    hh[inner] = 0.3
    layers.append((shell > 0, shade(hh, hex2rgb("#9AA5AD"), ambient=0.45, diffuse=0.7, spec=0.5), 1.0))
    layers.append(ring_glow(128, 128, 100, 8, hex2rgb(TEAL), 90))
    save(comp(layers), "resonator_shell")

def t_amethyst_resonator():
    layers = crystal_spikes(128, 140, "#9C6BD6",
                            [(-math.pi / 2, 92, 26, 1.8), (-math.pi / 2 + 0.7, 62, 18, 1.4),
                             (-math.pi / 2 - 0.7, 58, 18, 1.4)], None, glow_col="#C9A7E3", glow_a=70)
    ring = h_torus(128, 140, 84, 70, 1.6)
    layers.append((ring > 0, shade(ring, hex2rgb("#B08A3E"), ambient=0.45, diffuse=0.7, spec=0.5), 1.0))
    save(comp(layers), "amethyst_resonator")

def t_spectrum_module():
    layers = []
    frame = h_box(48, 88, 208, 176, 1.4, 12)
    layers.append((frame > 0, shade(frame, hex2rgb(DARK_STEEL), ambient=0.42, diffuse=0.7), 1.0))
    # 三棱镜(三角分光)
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    dr.polygon([(128 - 44, 160), (128 + 44, 160), (128, 84)], fill=255)
    m = np.array(img) > 128
    hh = np.where(m, 1.6, 0).astype(np.float32)
    layers.append((m, shade(hh, hex2rgb("#B9C4CC"), ambient=0.5, diffuse=0.65, spec=0.8, spec_pow=6), 0.92))
    # 光谱条
    for i, col in enumerate(["#E2574C", "#E29C4B", "#E2D84B", "#6BD65E", "#4B9FE2", "#8A6BE2"]):
        y0 = 96 + i * 11
        bar = h_box(64, y0, 196, y0 + 7, 1.2, 3)
        layers.append((bar > 0, shade(bar, hex2rgb(col), ambient=0.6, diffuse=0.5), 0.85))
    save(comp(layers), "spectrum_module")

def t_lens(name, col, glow):
    h = h_dome(128, 128, 84, 84, 1.5)
    rgb = shade(h, hex2rgb(col), ambient=0.52, diffuse=0.58, spec=0.85, spec_pow=5)
    d = dist(128, 128)
    rgb = rgb + (np.clip(1 - d / 84, 0, 1) * 60)[..., None] * hex2rgb(glow)[None, None, :] / 255.0
    layers = [(h > 0, rgb, 0.95), ring_glow(128, 128, 86, 5, hex2rgb(glow), 110)]
    save(comp(layers), name)

def t_crystal(name, col, glow, big, halo_a=80):
    rng = np.random.default_rng(11)
    spikes = [(-math.pi / 2 + 0.0, 92 if big else 74, 24 if big else 18, 1.8),
              (-math.pi / 2 + 0.75, 66 if big else 54, 17, 1.4),
              (-math.pi / 2 - 0.75, 62 if big else 52, 16, 1.4)]
    if big:
        spikes.append((-math.pi / 2 + 0.35, 76, 14, 1.5))
    layers = crystal_spikes(128, 148, col, spikes, rng, glow_col=glow, glow_a=halo_a)
    base = h_dome(128, 156, 40 if big else 32, 16, 1.2)
    layers.append((base > 0, shade(base, hex2rgb("#5E6870"), ambient=0.45, diffuse=0.65), 1.0))
    save(comp(layers), name)

def t_void_drop(name, col, swirl):
    h = h_dome(128, 128, 56, 68, 1.7)
    m = h > 0
    rgb = shade(h, hex2rgb(col), ambient=0.45, diffuse=0.65, spec=0.7)
    ang = np.arctan2(Y - 128, X - 128)
    sw = np.sin(ang * 3 + dist(128, 128) * 0.08) * 26
    rgb = rgb + sw[..., None] * hex2rgb(swirl)[None, None, :] / 255.0 * m[..., None]
    layers = [(m, rgb, 0.97), ring_glow(128, 128, 74, 10, hex2rgb(swirl), 100)]
    save(comp(layers), name)

def t_pellets():
    layers = []
    for i, (cx, cy) in enumerate([(100, 190), (156, 190), (128, 148)]):
        p = h_cyl(cx, cy, 26, 9, cy - 16, cy + 16)
        ptop = h_dome(cx, cy - 16, 26, 9, 1.8)
        hh = np.maximum(p, ptop)
        col = "#8E989F" if i < 2 else "#A8B2B8"
        layers.append((hh > 0, shade(hh, hex2rgb(col), ambient=0.45, diffuse=0.68, spec=0.45), 1.0))
        layers.append(ring_glow(cx, cy - 16, 27, 3, hex2rgb("#7FD830"), 60))
    save(comp(layers), "thorium_pellet")

def t_canister(name, col, glow):
    layers = []
    body = h_cyl(128, 120, 56, 12, 86, 186)
    layers.append((body > 0, shade(body, hex2rgb(col), ambient=0.42, diffuse=0.72, spec=0.4), 1.0))
    band = h_cyl(128, 128, 58, 12, 118, 154)
    layers.append((band > 0, shade(band, hex2rgb(TEAL), ambient=0.5, diffuse=0.6), 1.0))
    neck = h_cyl(128, 128, 16, 5, 58, 88)
    layers.append((neck > 0, shade(neck, hex2rgb(DARK_STEEL), ambient=0.42, diffuse=0.7), 1.0))
    layers.append(ring_glow(128, 136, 86, 6, hex2rgb(glow), 90))
    save(comp(layers), name)

def t_wrench():
    layers = []
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    dr.rectangle([118, 92, 138, 210], fill=255)               # 柄
    dr.ellipse([92, 44, 164, 116], fill=255)                  # 开口头
    dr.ellipse([112, 64, 144, 96], fill=0)                    # 开口孔
    dr.rectangle([104, 60, 152, 68], fill=0)                  # 开口槽
    dr.ellipse([100, 186, 156, 242], fill=255)                # 环尾
    dr.ellipse([116, 202, 140, 226], fill=0)
    m = np.array(img) > 128
    h = np.where(m, 1.5, 0).astype(np.float32)
    layers.append((m, shade(h, hex2rgb("#96A1A9"), ambient=0.4, diffuse=0.75, spec=0.55), 1.0))
    grip = m & (Y > 140) & (Y < 200)
    hg = np.where(grip, 1.2, 0).astype(np.float32)
    layers.append((grip, shade(hg, hex2rgb(TEAL), ambient=0.48, diffuse=0.65), 1.0))
    save(comp(layers), "wrench")

def t_probe_staff():
    layers = []
    rod = h_cyl(140, 160, 9, 4, 60, 216)
    layers.append((rod > 0, shade(rod, hex2rgb("#7A5A38"), ambient=0.45, diffuse=0.65), 1.0))
    # 顶部晶球
    orb = h_dome(140, 62, 26, 26, 1.8)
    layers.append((orb > 0, shade(orb, hex2rgb("#2FA8A0"), ambient=0.5, diffuse=0.65, spec=0.85, spec_pow=5), 1.0))
    layers.append(ring_glow(140, 62, 34, 7, hex2rgb("#7FD8D0"), 140))
    # 包箍
    band = h_cyl(140, 96, 12, 4, 88, 100)
    layers.append((band > 0, shade(band, hex2rgb("#B08A3E"), ambient=0.45, diffuse=0.7, spec=0.4), 1.0))
    save(comp(layers), "probe_staff")

def t_tidal_calendar():
    layers = []
    disc = h_dome(128, 128, 86, 86, 1.3)
    layers.append((disc > 0, shade(disc, hex2rgb("#B08A3E"), ambient=0.45, diffuse=0.68, spec=0.5), 1.0))
    for k in range(8):
        a = k * math.pi / 4
        bx, by = 128 + math.cos(a) * 62, 128 + math.sin(a) * 62
        moon = h_dome(bx, by, 11, 11, 1.7)
        frac = (k / 8) % 1
        col = "#F2EDD8" if frac < 0.5 else "#4A5058"
        layers.append((moon > 0.1, shade(moon, hex2rgb(col), ambient=0.55, diffuse=0.55, spec=0.4), 1.0))
    ctr = h_dome(128, 128, 26, 26, 1.8)
    layers.append((ctr > 0.1, shade(ctr, hex2rgb("#2E6BD6"), ambient=0.5, diffuse=0.62, spec=0.7), 1.0))
    save(comp(layers), "tidal_calendar")

def t_terminal():
    layers = []
    body = h_box(44, 60, 212, 200, 1.5, 16)
    layers.append((body > 0, shade(body, hex2rgb("#3E464E"), ambient=0.45, diffuse=0.68), 1.0))
    screen = h_box(64, 84, 192, 140, 1.2, 6)
    layers.append((screen > 0, shade(screen, hex2rgb("#123B38"), ambient=0.6, diffuse=0.45), 1.0))
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    dr.line([(76, 112), (110, 112)], fill=255, width=5)
    dr.line([(76, 126), (150, 126)], fill=255, width=5)
    dr.line([(76, 140), (130, 140)], fill=255, width=5)
    wl = np.array(img) > 0
    hw = np.where(wl, 1.5, 0).astype(np.float32)
    layers.append((wl, shade(hw, hex2rgb("#7FD8D0"), ambient=0.7, diffuse=0.4), 1.0))
    for k in range(3):
        btn = h_box(76 + k * 34, 158, 96 + k * 34, 174, 1.6, 4)
        col = [TEAL, "#D9A728", "#C0392B"][k]
        layers.append((btn > 0, shade(btn, hex2rgb(col), ambient=0.55, diffuse=0.55), 1.0))
    ant = h_cyl(196, 70, 5, 3, 36, 64)
    layers.append((ant > 0, shade(ant, hex2rgb("#96A1A9"), ambient=0.45, diffuse=0.7), 1.0))
    save(comp(layers), "fieldpack_terminal")

def t_blueprint():
    layers = []
    roll = h_cyl(128, 122, 62, 12, 74, 176)
    layers.append((roll > 0, shade(roll, hex2rgb("#2E5F75"), ambient=0.48, diffuse=0.62), 1.0))
    # 端面卷纸
    ends = h_dome(128, 74, 62, 13, 1.6)
    layers.append((ends > 0.05, shade(ends, hex2rgb("#D8E8EC"), ambient=0.55, diffuse=0.55), 1.0))
    inner = dist(128, 74) < 30
    ei = np.where(inner, 1.2, 0).astype(np.float32)
    layers.append((inner, shade(ei, hex2rgb("#1C4453"), ambient=0.5, diffuse=0.55), 1.0))
    # 展开图纸(斜角)
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    dr.polygon([(36, 170), (150, 140), (220, 176), (110, 210)], fill=255)
    m = np.array(img) > 128
    hh = np.where(m, 1.3, 0).astype(np.float32)
    layers.append((m, shade(hh, hex2rgb("#BFDCF0"), ambient=0.55, diffuse=0.5), 1.0))
    gl = np.zeros((S, S, 4), dtype=np.float32)
    gl[..., :3] = hex2rgb("#1E9E96")
    xm = (X > 60) & (X < 200) & (np.abs((X - 60) % 24) < 2) & m
    ym = (Y > 145) & (Y < 205) & (np.abs((Y - 145) % 20) < 2) & m
    gl[..., 3] = np.where(xm | ym, 150, 0)
    layers.append((np.ones((S, S), dtype=bool), gl, 1.0))
    save(comp(layers), "giant_blueprint")

def t_tuning_fork():
    layers = []
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    dr.rectangle([88, 60, 108, 150], fill=255)
    dr.rectangle([148, 60, 168, 150], fill=255)
    dr.rounded_rectangle([88, 140, 168, 210], 18, fill=255)
    m = np.array(img) > 128
    h = np.where(m, 1.5, 0).astype(np.float32)
    layers.append((m, shade(h, hex2rgb("#B9C4CC"), ambient=0.42, diffuse=0.75, spec=0.75), 1.0))
    layers.append(ring_glow(128, 105, 60, 6, hex2rgb("#7FD8D0"), 90))
    save(comp(layers), "tuning_fork")

def t_rhythmic_book():
    layers = []
    cover = h_box(48, 56, 210, 204, 1.5, 10)
    layers.append((cover > 0, shade(cover, hex2rgb("#274B52"), ambient=0.45, diffuse=0.65), 1.0))
    spine = h_box(48, 56, 68, 204, 1.8, 6)
    layers.append((spine > 0, shade(spine, hex2rgb("#1B383E"), ambient=0.45, diffuse=0.65), 1.0))
    # 页缘
    pages = h_box(204, 64, 210, 196, 1.1, 4)
    layers.append((pages > 0, shade(pages, hex2rgb("#E8E2CC"), ambient=0.55, diffuse=0.5), 1.0))
    # 封面律动纹(同心弧)
    ring = np.abs(dist(128, 130) - 46) < 2.4
    hh = np.where(ring & (X > 80), 1.8, 0).astype(np.float32)
    layers.append((ring & (X > 80), shade(hh, hex2rgb(TEAL), ambient=0.55, diffuse=0.6, spec=0.4), 1.0))
    gem = h_dome(128, 130, 16, 16, 1.9)
    layers.append((gem > 0, shade(gem, hex2rgb("#2FA8A0"), ambient=0.5, diffuse=0.65, spec=0.8, spec_pow=5), 1.0))
    layers.append(ring_glow(128, 130, 22, 5, hex2rgb("#7FD8D0"), 110))
    save(comp(layers), "rhythmic_book")

def t_chip(name, gem_col, tier_dots):
    layers = []
    sub = h_box(52, 84, 204, 180, 1.2, 8)
    layers.append((sub > 0, shade(sub, hex2rgb("#20554A"), ambient=0.5, diffuse=0.6), 1.0))
    chip = h_box(88, 112, 168, 156, 1.7, 6)
    layers.append((chip > 0, shade(chip, hex2rgb("#20242B"), ambient=0.45, diffuse=0.7, spec=0.35), 1.0))
    # 引脚(上下)
    for k in range(5):
        px = 96 + k * 16
        for py, py2 in [(64, 84), (180, 200)]:
            pin = h_box(px - 4, py, px + 4, py2, 1.0, 2)
            layers.append((pin > 0, shade(pin, hex2rgb("#D9A728"), ambient=0.55, diffuse=0.55), 1.0))
    gem = h_dome(128, 134, 20, 20, 1.9)
    layers.append((gem > 0, shade(gem, hex2rgb(gem_col), ambient=0.5, diffuse=0.65, spec=0.85, spec_pow=5), 1.0))
    for k in range(tier_dots):
        dx = 128 + (k - (tier_dots - 1) / 2) * 14
        dot = h_dome(dx, 102, 4, 4, 1.8)
        layers.append((dot > 0.2, shade(dot, hex2rgb("#F2EDD8"), ambient=0.6, diffuse=0.5), 1.0))
    save(comp(layers), name)

def t_fuel_cell():
    layers = []
    body = h_cyl(128, 130, 48, 10, 84, 184)
    layers.append((body > 0, shade(body, hex2rgb("#5A4A72"), ambient=0.45, diffuse=0.65), 1.0))
    core = h_cyl(128, 130, 26, 7, 100, 170)
    layers.append((core > 0, shade(core, hex2rgb("#8A5FD6"), ambient=0.55, diffuse=0.6, spec=0.5), 0.95))
    cap = h_cyl(128, 130, 54, 10, 70, 90)
    layers.append((cap > 0, shade(cap, hex2rgb(DARK_STEEL), ambient=0.42, diffuse=0.7), 1.0))
    layers.append(ring_glow(128, 130, 92, 10, hex2rgb("#8A5FD6"), 130))
    save(comp(layers), "resonance_fuel")

def t_tidal_resonator():
    layers = []
    shell = h_torus(128, 128, 84, 56, 1.4)
    layers.append((shell > 0, shade(shell, hex2rgb("#8FA7B8"), ambient=0.45, diffuse=0.7, spec=0.5), 1.0))
    wave = h_dome(128, 128, 44, 44, 1.6)
    rgb = shade(wave, hex2rgb("#2E6BD6"), ambient=0.5, diffuse=0.62, spec=0.7)
    ang = np.arctan2(Y - 128, X - 128)
    rgb = rgb + (np.sin(ang * 2 + dist(128, 128) * 0.1) * 22)[..., None]
    layers.append((wave > 0, rgb, 0.96))
    for k in range(4):
        a = k * math.pi / 2 + math.pi / 4
        bx, by = 128 + math.cos(a) * 70, 128 + math.sin(a) * 70
        b = h_dome(bx, by, 10, 10, 1.8)
        layers.append((b > 0.1, shade(b, hex2rgb("#B08A3E"), ambient=0.5, diffuse=0.65, spec=0.5), 1.0))
    save(comp(layers), "tidal_resonator")

def t_dry_ice():
    rng = np.random.default_rng(5)
    layers = []
    for cx, cy, r_ in [(96, 168, 44), (156, 176, 38), (126, 120, 46)]:
        h = h_box(cx - r_, cy - r_ * 0.8, cx + r_, cy + r_ * 0.8, 1.5, 8)
        layers.append((h > 0, shade(h, hex2rgb("#DCE8EE"), ambient=0.6, diffuse=0.5, spec=0.4), 1.0))
    mist = np.zeros((S, S, 4), dtype=np.float32)
    mist[..., :3] = hex2rgb("#FFFFFF")
    md = dist(128, 90)
    mist[..., 3] = np.clip(1 - md / 90, 0, 1) * 60
    layers.append((np.ones((S, S), dtype=bool), mist, 1.0))
    save(comp(layers), "dry_ice")

def t_mercury_fulminate():
    rng = np.random.default_rng(9)
    rgb, m = pile(rng, 128, 148, 74, 46, "#D8CFC0", n=16, spec=0.9)
    layers = [(m, rgb, 1.0)]
    for _ in range(5):
        bx, by = rng.integers(90, 166), rng.integers(120, 176)
        gl = h_dome(bx, by, 6, 6, 1.5)
        layers.append((gl > 0.3, shade(gl, hex2rgb("#E88A7A"), ambient=0.6, diffuse=0.5, spec=0.6), 0.8))
    save(comp(layers), "mercury_fulminate")

def t_powder(name, col, spec):
    rng = np.random.default_rng(abs(hash(name)) % 10000)
    rgb, m = pile(rng, 128, 148, 76, 48, col, n=18, spec=spec)
    save(comp([(m, rgb, 1.0)]), name)

def t_sulfur():
    rng = np.random.default_rng(4)
    layers = []
    base_rgb, base_m = pile(rng, 128, 150, 72, 46, "#D9C22E", n=12, spec=0.6)
    layers.append((base_m, base_rgb, 1.0))
    for _ in range(6):
        a = rng.uniform(0, 2 * math.pi)
        bx, by = 128 + math.cos(a) * 40, 150 + math.sin(a) * 26
        cube = h_box(bx - 12, by - 12, bx + 12, by + 10, 1.6, 4)
        layers.append((cube > 0, shade(cube, hex2rgb("#E8D44A"), ambient=0.55, diffuse=0.55, spec=0.6), 1.0))
    save(comp(layers), "sulfur")

def t_corrosion_salt():
    rng = np.random.default_rng(6)
    layers = []
    rgb, m = pile(rng, 128, 148, 76, 48, "#7FA8A0", n=15, spec=0.75)
    layers.append((m, rgb, 1.0))
    for _ in range(6):
        bx, by = rng.integers(88, 168), rng.integers(118, 178)
        sp = crystal_spikes(bx, by, "#9FD0C6", [(-math.pi / 2 + rng.uniform(-0.6, 0.6), 26, 8, 1.6)], rng)
        layers += sp
    save(comp(layers), "corrosion_salt")



# ═══════════════ 261014 T10 场学饰品(7) ═══════════════

def t_resonant_band():
    RNG = np.random.default_rng(21)
    h = h_torus(128, 132, 82, 56, 1.3)
    m = h > 0
    rgb = shade(h, hex2rgb("#C8A24E"), ambient=0.38, diffuse=0.72, spec=0.55)
    layers = [(m, rgb, 1.0), ring_glow(128, 132, 69, 5, hex2rgb("#7FD8D0"), 120)]
    # 四枚谐振晶钉
    for a in [0, math.pi / 2, math.pi, 3 * math.pi / 4 + math.pi / 2]:
        cx = 128 + math.cos(a) * 69
        cy = 132 + math.sin(a) * 69
        layers += crystal_spikes(cx, cy, "#17D4C8", [(a, 20, 7, 1.5)], RNG, glow_col="#B0FFF6", glow_a=70)
    save(comp(layers), "resonant_band")

def t_magnetic_lens():
    h = h_torus(128, 128, 84, 54, 1.25)
    m = h > 0
    ring = shade(h, hex2rgb("#9AA5A8"), ambient=0.4, diffuse=0.7, spec=0.6)
    lens = h_dome(128, 128, 52, 52, 1.0)
    lm = lens > 0.05
    lrgb = shade(lens, hex2rgb("#AED6E8"), ambient=0.6, diffuse=0.5, spec=1.1)
    layers = [(lm, lrgb, 0.92), (m, ring, 1.0)]
    # 透镜十字准线 + 磁极标记
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    dr.line([(128 - 40, 128), (128 + 40, 128)], fill=255, width=3)
    dr.line([(128, 128 - 40), (128, 128 + 40)], fill=255, width=3)
    dr.ellipse([128 - 12, 128 - 12, 128 + 12, 128 + 12], outline=255, width=3)
    cm = np.array(img) > 128
    layers.append((cm, solid("#3A6B8A"), 0.8))
    save(comp(layers), "magnetic_lens")

def t_fieldline_amulet():
    layers = []
    # 挂环
    ring_h = h_torus(128, 52, 26, 14, 1.1)
    layers.append((ring_h > 0, shade(ring_h, hex2rgb("#C8A24E"), ambient=0.4, diffuse=0.7, spec=0.5), 1.0))
    # 六角坠牌(261014 放大主体保覆盖率)
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    hexpts = [(128 + 78 * math.cos(math.pi / 6 * i + math.pi / 6), 152 + 78 * math.sin(math.pi / 6 * i + math.pi / 6)) for i in range(6)]
    dr.polygon(hexpts, fill=255)
    m = np.array(img) > 128
    hh = np.where(m, 1.25 - 0.5 * np.abs(X - 128) / 78 - 0.3 * np.abs(Y - 152) / 78, 0)
    layers.append((m, shade(hh, hex2rgb("#2E6E78"), ambient=0.42, diffuse=0.75, spec=0.5), 1.0))
    # 场线波纹
    gl = Image.new("L", (S, S), 0)
    dg = ImageDraw.Draw(gl)
    for k, w in [(0, 30), (1, 18), (2, 8)]:
        y0 = 128 + k * 18
        pts = [(x, y0 + 7 * math.sin(x / 14 + k)) for x in range(66, 191, 6)]
        dg.line(pts, fill=255, width=3)
    gm = np.array(gl) > 128
    layers.append((gm & m, solid("#7FD8D0"), 0.95))
    save(comp(layers), "fieldline_amulet")

def t_flux_belt():
    layers = []
    # 皮带(左右两条暗革)
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    dr.rectangle([8, 104, 88, 152], fill=255)
    dr.rectangle([168, 104, 248, 152], fill=255)
    m = np.array(img) > 128
    hh = np.where(m, 1.05, 0)
    layers.append((m, shade(hh, hex2rgb("#5A3E2A"), ambient=0.35, diffuse=0.8), 1.0))
    # 缝线
    gl = Image.new("L", (S, S), 0)
    dg = ImageDraw.Draw(gl)
    for x0 in (14, 168):
        dg.line([(x0, 112), (x0 + 70, 112)], fill=255, width=2)
        dg.line([(x0, 144), (x0 + 70, 144)], fill=255, width=2)
    gm = np.array(gl) > 128
    layers.append((gm, solid("#8A6A48"), 0.9))
    # 琥珀扣(圆角方板 + 内环)
    plate = h_box(84, 86, 172, 170, 1.3, 8)
    layers.append((plate > 0, shade(plate, hex2rgb("#C8A24E"), ambient=0.4, diffuse=0.72, spec=0.6), 1.0))
    tor = h_torus(128, 128, 34, 20, 1.1)
    layers.append((tor > 0, shade(tor, hex2rgb("#8A6A20"), ambient=0.4, diffuse=0.7, spec=0.4), 1.0))
    layers.append(ring_glow(128, 128, 27, 6, hex2rgb("#FFD873"), 150))
    save(comp(layers), "flux_belt")

def t_tidal_charm():
    layers = []
    # 贝壳螺旋(海蓝→ Prismarine 绿渐层扇贝)
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    for i in range(9):
        r = 82 - i * 8
        a0, a1 = -100 - i * 4, 100 + i * 4
        dr.pieslice([128 - r, 140 - r, 128 + r, 140 + r], a0, a1, fill=255 if i % 2 == 0 else 0)
    m0 = np.array(img) > 128
    hh = np.where(m0, 1.15 - 0.3 * dist(128, 140) / 82, 0)
    layers.append((m0, shade(hh, hex2rgb("#4E9E8A"), ambient=0.42, diffuse=0.72, spec=0.55), 1.0))
    # 中央珍珠
    pearl = h_dome(128, 128, 26, 26, 1.5)
    layers.append((pearl > 0, shade(pearl, hex2rgb("#DCE8EC"), ambient=0.55, diffuse=0.5, spec=1.2), 1.0))
    # 顶部挂环
    ring = h_torus(128, 56, 16, 9, 1.0)
    layers.append((ring > 0, shade(ring, hex2rgb("#C8A24E"), ambient=0.4, diffuse=0.7, spec=0.5), 1.0))
    layers.append(ring_glow(128, 128, 30, 5, hex2rgb("#9FE8D8"), 90))
    save(comp(layers), "tidal_charm")

def t_void_step_ring():
    RNG = np.random.default_rng(22)
    h = h_torus(128, 128, 80, 52, 1.35)
    m = h > 0
    rgb = shade(h, hex2rgb("#3A3244"), ambient=0.32, diffuse=0.78, spec=0.7)
    layers = [(m, rgb, 1.0), ring_glow(128, 128, 66, 8, hex2rgb("#A98FE8"), 160)]
    # 环上悬浮虚影碎晶
    for a in [0.4, 2.2, 4.0, 5.4]:
        cx = 128 + math.cos(a) * 66
        cy = 128 + math.sin(a) * 66
        layers += crystal_spikes(cx, cy, "#8A5FD6", [(a, 16, 6, 1.4)], RNG, glow_col="#C9A7E3", glow_a=80)
    save(comp(layers), "void_step_ring")

def t_superconductor_core():
    layers = []
    # 中央六角核心(超导白青)
    img = Image.new("L", (S, S), 0)
    dr = ImageDraw.Draw(img)
    hexpts = [(128 + 52 * math.cos(math.pi / 3 * i), 128 + 52 * math.sin(math.pi / 3 * i)) for i in range(6)]
    dr.polygon(hexpts, fill=255)
    m = np.array(img) > 128
    hh = np.where(m, 1.6 - 0.9 * dist(128, 128) / 52, 0)
    layers.append((m, shade(hh, hex2rgb("#D8F4F0"), ambient=0.6, diffuse=0.5, spec=1.0), 1.0))
    # 外支架环(深钢) + 上下钳口
    tor = h_torus(128, 128, 78, 60, 1.15)
    tm = tor > 0
    mm = tm & ~m
    layers.append((mm, shade(tor, hex2rgb("#5A6878"), ambient=0.38, diffuse=0.75, spec=0.6), 1.0))
    # 能量轨道环 ×2(旋转对称的两段弧光)
    for ph in (0, math.pi):
        gl = Image.new("L", (S, S), 0)
        dg = ImageDraw.Draw(gl)
        dg.arc([128 - 88, 128 - 88, 128 + 88, 128 + 88], math.degrees(ph) + 18, math.degrees(ph) + 162, fill=255, width=6)
        gm = np.array(gl) > 128
        layers.append((gm, solid("#35D8CE"), 0.95))
    # 核心辉光 + 星芒
    layers.append(ring_glow(128, 128, 30, 14, hex2rgb("#B0FFF6"), 110))
    sp = Image.new("L", (S, S), 0)
    dsp = ImageDraw.Draw(sp)
    dsp.line([(128 - 60, 128), (128 + 60, 128)], fill=255, width=3)
    dsp.line([(128, 128 - 60), (128, 128 + 60)], fill=255, width=3)
    spm = np.array(sp) > 128
    layers.append((spm & (dist(128, 128) < 62), solid("#FFFFFF"), 0.85))
    save(comp(layers), "superconductor_core")

def t_accessories():
    t_resonant_band(); t_magnetic_lens(); t_fieldline_amulet(); t_flux_belt()
    t_tidal_charm(); t_void_step_ring(); t_superconductor_core()

def build():
    # ── A. 矿物/锭 花纹强化(12) ──
    ingots = {
        "aluminum_ingot": "#C8D2D8", "aluminum_brass_ingot": "#D6C48A", "flux_alloy_ingot": "#C8A24E",
        "invar_ingot": "#9AA5A8", "monel_ingot": "#B8A890", "permalloy_ingot": "#A8B0B8",
        "pure_iron_ingot": "#D8D8D8", "stainless_steel_ingot": "#C0C8D0", "steel_ingot": "#8E989F",
        "superconductor_alloy": "#5A6878",
    }
    for name, col in ingots.items():
        save(ingot_pattern(name, col), name)
    save(ore_glints("raw_bauxite"), "raw_bauxite")
    save(ore_glints("raw_monazite"), "raw_monazite")
    # 超导合金追加发光脉络
    im = Image.open(os.path.join(OUT, "superconductor_alloy.png")).convert("RGBA")
    gl = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    gd = ImageDraw.Draw(gl)
    gd.line([(14, 44), (26, 34), (22, 24), (36, 18)], fill=(127, 216, 208, 170), width=2)
    gd.line([(40, 48), (50, 38), (46, 28)], fill=(127, 216, 208, 140), width=2)
    save(Image.alpha_composite(im, gl), "superconductor_alloy")

    # ── B. 全新绘制(51) ──
    t_gear(); t_coil(); t_field_wire(); t_superconductor_ring(); t_turbine_blade()
    t_nozzle(); t_piston_frame(); t_bearing(); t_logic_circuit(); t_silicon_wafer()
    t_sensor_core(); t_electromagnet_core(); t_magnetized("magnetized_core", False)
    t_magnetized("magnetized_core2", True); t_distillation_tray(); t_seal_flange()
    t_resonator_shell(); t_amethyst_resonator(); t_spectrum_module()
    t_lens("amethyst_lens", "#9C6BD6", "#C9A7E3"); t_lens("void_lens", "#2B1E3D", "#8A5FD6")
    t_crystal("resonant_shard", "#2FA8A0", "#7FD8D0", True, 60)
    t_crystal("resonant_crystal_raw", "#2E8A84", "#5FD0C8", False, 70)
    t_crystal("resonant_crystal_refined", "#20B2AA", "#8FE8E0", True, 90)
    t_crystal("resonant_crystal_ultra", "#17D4C8", "#B0FFF6", True, 130)
    t_void_drop("void_condensate", "#241A33", "#8A5FD6")
    t_void_drop("tuned_void_essence", "#3A2A52", "#A98FE8")
    t_pellets(); t_canister("compressed_air", "#8E989F", "#B8D8E8"); t_fuel_cell()
    t_wrench(); t_probe_staff(); t_tidal_calendar(); t_terminal(); t_blueprint()
    t_tuning_fork(); t_rhythmic_book(); t_tidal_resonator(); t_dry_ice()
    t_mercury_fulminate(); t_sulfur(); t_corrosion_salt()
    t_accessories()   # 261014 T10 场学饰品
    t_powder("carbon_black", "#26262A", 0.9)
    t_powder("inert_concentrate", "#8E8E92", 0.5)
    t_chip("upgrade_efficiency_1", "#2FA84F", 1); t_chip("upgrade_efficiency_2", "#2E86D6", 2)
    t_chip("upgrade_efficiency_3", "#8A5FD6", 3); t_chip("upgrade_field_capacity", "#1E9E96", 2)
    t_chip("upgrade_isolation", "#D9A728", 1); t_chip("upgrade_rectifier", "#C0392B", 2)
    t_chip("upgrade_superconductor", "#17D4C8", 3)

    # 预览拼图
    names = sorted(os.listdir(OUT))
    names = [n[:-4] for n in names if n.endswith(".png")]
    cols = 12
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * 68, rows * 68), (40, 44, 50, 255))
    for i, n in enumerate(names):
        im = Image.open(os.path.join(OUT, n + ".png"))
        sheet.paste(im, ((i % cols) * 68 + 2, (i // cols) * 68 + 2), im)
    sheet.save(os.path.join(ROOT, "preview_items_v3.png"))
    print(f"✓ v3 物品贴图 {len(names)} 张 + preview_items_v3.png")

if __name__ == "__main__":
    build()
