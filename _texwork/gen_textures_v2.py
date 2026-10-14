#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Faithful 64x 素材重制管线 v2（§16.2 资产管线：改生成器后重跑，勿手改产物）
- 从 Faithful 64x Release 13 提取基础贴图，做保亮度变色(HSV)、叠加、描边、花纹处理
- 输出 64×64 RGBA 物品贴图（与 Faithful 同分辨率同精致度），透明通道完整保留
- 全量替换 _texwork 时代生成的 16x 粗糙物品贴图（机器方块贴图不在本脚本范围）
"""
import os, sys, math, colorsys
from PIL import Image, ImageDraw, ImageFilter, ImageEnhance

ROOT = os.path.dirname(os.path.abspath(__file__))
WORKSPACE = os.path.abspath(os.path.join(ROOT, "..", ".."))
FAITHFUL_ITEM = os.path.join(WORKSPACE, "_api_ref", "faithful", "assets", "minecraft", "textures", "item")
FAITHFUL_BLOCK = os.path.join(WORKSPACE, "_api_ref", "faithful", "assets", "minecraft", "textures", "block")
OUT = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "fieldworks", "textures", "item")
os.makedirs(OUT, exist_ok=True)

BLOCK_ALIASES = {"piston": "piston_side", "cake": "cake_side", "lightning_rod": "lightning_rod"}

def src(name):
    p = os.path.join(FAITHFUL_ITEM, name + ".png")
    if not os.path.exists(p):
        alt = BLOCK_ALIASES.get(name, name)
        p2 = os.path.join(FAITHFUL_BLOCK, alt + ".png")
        if os.path.exists(p2):
            p = p2
    im = Image.open(p).convert("RGBA")
    if im.size != (64, 64):
        im = im.resize((64, 64), Image.LANCZOS)
    return im

# ── 核心算法：保亮度变色（把不透明像素的色相/饱和度替换为目标，亮度保留 Faithful 光影） ──
def recolor(im, target_hex, sat_boost=1.0, val_scale=1.0, keep_alpha=True):
    r, g, b, a = im.split()
    px = im.load()
    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    po = out.load()
    tr, tg, tb = int(target_hex[0:2], 16), int(target_hex[2:4], 16), int(target_hex[4:6], 16)
    th, ts, tv = colorsys.rgb_to_hsv(tr/255, tg/255, tb/255)
    for y in range(im.height):
        for x in range(im.width):
            pr, pg, pb, pa = px[x, y]
            if pa == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(pr/255, pg/255, pb/255)
            # 平滑颜色区（低饱和）也上色：s 抬到目标饱和的一部分
            ns = min(1.0, (s * 0.25 + ts * 0.75) * sat_boost)
            nv = min(1.0, v * val_scale if val_scale <= 1 else v + (1-v)*(val_scale-1))
            nr, ng, nb = colorsys.hsv_to_rgb(th, ns, nv)
            po[x, y] = (int(nr*255), int(ng*255), int(nb*255), pa)
    return out

def tint_additive(im, hexcol, strength=0.35):
    """仅给中间亮度像素叠加色偏（保留高光阴影）"""
    tr, tg, tb = int(hexcol[0:2],16), int(hexcol[2:4],16), int(hexcol[4:6],16)
    px = im.load(); out = im.copy(); po = out.load()
    for y in range(im.height):
        for x in range(im.width):
            r,g,b,a = px[x,y]
            if a == 0: continue
            lum = (r*299+g*587+b*114)//1000
            w = strength * (1 - abs(lum-128)/128)
            po[x,y] = (int(r*(1-w)+tr*w), int(g*(1-w)+tg*w), int(b*(1-w)+tb*w), a)
    return out

def overlay(base, top, off=(0,0), mult_alpha=1.0):
    if mult_alpha < 1.0:
        a = top.split()[3].point(lambda v: int(v*mult_alpha))
        top = top.copy(); top.putalpha(a)
    base.alpha_composite(top, off)
    return base

def mirror_h(im):
    return im.transpose(Image.FLIP_LEFT_RIGHT)

def glow(im, hexcol, radius=3, strength=0.8):
    """提取亮部做外发光"""
    tr, tg, tb = int(hexcol[0:2],16), int(hexcol[2:4],16), int(hexcol[4:6],16)
    lum = Image.new("RGBA", im.size, (0,0,0,0)); pl = lum.load(); pi = im.load()
    for y in range(im.height):
        for x in range(im.width):
            r,g,b,a = pi[x,y]
            l = (r+g+b)//3
            pl[x,y] = (tr,tg,tb, int(a * max(0, l-150)/105) if l>150 else 0)
    blur = lum.filter(ImageFilter.GaussianBlur(radius))
    res = Image.new("RGBA", im.size, (0,0,0,0))
    res.alpha_composite(blur); res.alpha_composite(blur); res.alpha_composite(im)
    return res

def border_frame(im, hexcol, width=2, inset=2):
    """64x 内描边框（品阶框，新玩法卡牌语言）"""
    d = ImageDraw.Draw(im, "RGBA")
    tr, tg, tb = int(hexcol[0:2],16), int(hexcol[2:4],16), int(hexcol[4:6],16)
    for i in range(width):
        d.rectangle([inset+i, inset+i, 63-inset-i, 63-inset-i], outline=(tr,tg,tb,200))
    return im

def vgrad(im, top_hex, bot_hex, alpha=90):
    tr,tg,tb = int(top_hex[0:2],16),int(top_hex[2:4],16),int(top_hex[4:6],16)
    br,bg,bb = int(bot_hex[0:2],16),int(bot_hex[2:4],16),int(bot_hex[4:6],16)
    ov = Image.new("RGBA", im.size, (0,0,0,0)); po = ov.load()
    for y in range(64):
        t = y/63
        c = (int(tr+(br-tr)*t), int(tg+(bg-tg)*t), int(tb+(bb-tb)*t), alpha)
        for x in range(64): po[x,y] = c
    return overlay(im, ov)

def save(im, name):
    im = im.convert("RGBA")
    assert im.size == (64,64)
    im.save(os.path.join(OUT, name + ".png"))
    print("✓", name)

# ═══════════════ 分类生成 ═══════════════

GEN = {}

# ── 1. 锭族（iron_ingot 造型，逐色保亮度变色） ──
INGOTS = {
    "steel_ingot":            ("B8C4CE", 1.00, 1.00),
    "aluminum_ingot":         ("D9DEE3", 0.90, 1.06),
    "stainless_steel_ingot":  ("E6EDF2", 0.95, 1.08),
    "invar_ingot":            ("9FA8AC", 0.95, 0.97),
    "monel_ingot":            ("C9A97E", 1.00, 1.00),
    "permalloy_ingot":        ("C7B7A0", 1.00, 1.02),
    "flux_alloy_ingot":       ("8FB7D9", 1.00, 1.00),
    "pure_iron_ingot":        ("E3E8EC", 0.92, 1.10),
    "aluminum_brass_ingot":   ("E2C06A", 1.00, 1.02),
    "superconductor_alloy":   ("9AD7E8", 1.05, 1.05),
}
for name, (col, s, v) in INGOTS.items():
    GEN[name] = lambda col=col, s=s, v=v: recolor(src("iron_ingot"), col, s, v)

# ── 2. 粉/尘/粒（blaze_powder/gunpowder 造型） ──
POWDERS = {
    "sulfur":              ("blaze_powder", "E8D44D"),
    "carbon_black":        ("gunpowder",    "2E2E33"),
    "mercury_fulminate":   ("gunpowder",    "D8C9B8"),
    "inert_concentrate":   ("gunpowder",    "9FB4C7"),
    "thorium_pellet":      ("fire_charge",  "7E9B6E"),
    "corrosion_salt":      ("sugar",        "C9D6A3"),
    "dry_ice":             ("snowball",     "CDE9F2"),
    "resonance_fuel":      ("blaze_powder", "F0A83D"),
}
for name, (base, col) in POWDERS.items():
    if name == "resonance_fuel":
        GEN[name] = lambda col=col: glow(recolor(src("blaze_powder"), col, 1.1, 1.15), "FFD980", 4, 0.9)
    elif name == "dry_ice":
        GEN[name] = lambda col=col: glow(recolor(src("snowball"), col, 0.85, 1.0), "BEE8FF", 3, 0.4)
    else:
        GEN[name] = lambda b=base, col=col: recolor(src(b), col)

# ── 3. 粗矿/碎料（raw_iron 造型） ──
for name, col in {"raw_bauxite": "C79A68", "raw_monazite": "B8A24E"}.items():
    GEN[name] = lambda col=col: recolor(src("raw_iron"), col)

# ── 4. 晶体族（amethyst_shard/diamond/emerald 造型 + 品阶框） ──
CRYSTALS = {
    "resonant_crystal_raw":     ("amethyst_shard", "A88BD9", None),
    "resonant_crystal_refined": ("diamond",        "8FD3E8", "C8A85A"),
    "resonant_crystal_ultra":   ("diamond",        "5FE0D9", "4FC3D9"),
    "resonant_shard":           ("amethyst_shard", "8C6FC4", None),
    "void_condensate":          ("ender_pearl",    "1E2B3A", None),
    "tuned_void_essence":       ("ender_eye",      "3A5A7A", None),
}
for name, (base, col, frame) in CRYSTALS.items():
    if name == "tuned_void_essence":
        GEN[name] = lambda b=base, col=col: glow(recolor(src(b), col, 1.1, 1.0), "6FE0D9", 3, 0.7)
    elif frame:
        GEN[name] = lambda b=base, col=col, f=frame: border_frame(recolor(src(b), col, 1.05, 1.05), f, 2, 3)
    else:
        GEN[name] = lambda b=base, col=col: recolor(src(b), col)

# ── 5. 镜/谐振器 ──
GEN["amethyst_lens"] = lambda: recolor(src("ender_eye"), "B06FE0", 1.0, 1.0)
GEN["void_lens"] = lambda: glow(recolor(src("ender_eye"), "22324A", 0.9, 0.95), "4FC3D9", 2, 0.5)
GEN["tuning_fork"] = lambda: recolor(src("shears"), "C9A0DC", 0.9, 1.0)
GEN["tidal_resonator"] = lambda: glow(recolor(src("heart_of_the_sea"), "3E8BD9", 1.0, 1.05), "7EC8FF", 3, 0.6)
GEN["amethyst_resonator"] = lambda: glow(recolor(src("heart_of_the_sea"), "9A5FD0", 1.0, 1.05), "D0A0FF", 3, 0.6)

# ── 6. 部件族（clock/compass/piston 等变形） ──
def _gear(color, dark="6A6F76"):
    """faithful clock 表盘 → 齿轮：中心孔 + 四向齿 + 径向辐条"""
    g = recolor(src("clock_16"), color, 0.9, 0.95)
    d = ImageDraw.Draw(g, "RGBA")
    c = (255, 255, 255, 0)
    # 中心孔
    d.ellipse([27, 27, 37, 37], fill=(30, 32, 36, 255))
    d.ellipse([29, 29, 35, 35], fill=(16, 17, 20, 255))
    # 辐条
    dr, dg, db = int(dark[0:2],16), int(dark[2:4],16), int(dark[4:6],16)
    d.line([32, 12, 32, 52], fill=(dr, dg, db, 160), width=2)
    d.line([12, 32, 52, 32], fill=(dr, dg, db, 160), width=2)
    return g

GEN["steel_gear"] = lambda: _gear("9FA8B2")
GEN["copper_coil"] = lambda: overlay(
    recolor(src("lightning_rod"), "B8703A", 1.0, 0.95),
    mirror_h(recolor(src("lightning_rod"), "D08A50", 1.0, 1.05)), (18, 0))
GEN["piston_frame"] = lambda: recolor(src("piston"), "8B939C", 0.95, 0.97)
GEN["precision_bearing"] = lambda: overlay(
    recolor(src("slime_ball"), "9AA4AE", 0.6, 0.95),
    recolor(src("iron_nugget"), "C9D0D8", 1.0, 1.05), (14, 14))
GEN["logic_circuit"] = lambda: overlay(
    recolor(src("music_disc_cat"), "1E6B45", 0.9, 0.9),
    recolor(src("gold_nugget"), "E8C53D", 1.0, 1.1), (26, 8))
GEN["sensor_core"] = lambda: glow(recolor(src("ender_eye"), "4FC3D9", 1.0, 1.0), "7FE8FF", 2, 0.7)
GEN["nozzle_assembly"] = lambda: mirror_h(recolor(src("bucket"), "8B939C", 0.95, 0.95))
GEN["distillation_tray"] = lambda: recolor(src("cake"), "9AA4AE", 0.8, 0.85)
GEN["electromagnet_core"] = lambda: overlay(recolor(src("lightning_rod"), "B8703A", 1.0, 0.95),
                                            recolor(src("iron_ingot"), "E8D44D", 1.0, 1.0), (16, 34))
GEN["magnetized_core"] = lambda: glow(recolor(src("lightning_rod"), "C23B3B", 1.0, 1.0), "FF8080", 2, 0.5)
GEN["magnetized_core2"] = lambda: glow(recolor(src("lightning_rod"), "3B6BC2", 1.0, 1.0), "80A8FF", 2, 0.5)
GEN["turbine_blade"] = lambda: recolor(src("iron_axe"), "9FA8B2", 0.9, 0.95)
GEN["seal_flange"] = lambda: recolor(src("anvil"), "8B939C", 0.9, 0.92)
GEN["resonator_shell"] = lambda: recolor(src("bell"), "9A5FD0", 0.85, 0.9)
GEN["field_wire"] = lambda: overlay(recolor(src("string"), "B87A3A", 1.0, 0.95),
                                    recolor(src("string"), "4FC3D9", 1.0, 1.1), (0, 22))
GEN["superconductor_ring"] = lambda: glow(
    overlay(recolor(src("gold_nugget"), "9AD7E8", 1.0, 1.0),
            recolor(src("snowball"), "E0FAFF", 0.7, 1.1), (16, 16)), "7FE8FF", 3, 0.6)
GEN["silicon_wafer"] = lambda: overlay(recolor(src("music_disc_cat"), "5A6A7E", 0.8, 0.9),
                                       recolor(src("gold_nugget"), "4FC3D9", 1.0, 1.2), (26, 26))

# ── 7. 工具/手持 ──
def _probe_staff():
    rod = recolor(src("blaze_rod"), "8B6F4A", 0.85, 0.9)
    tip = recolor(src("amethyst_shard"), "4FC3D9", 1.1, 1.1)
    return glow(overlay(rod, tip, (24, 2)), "7FE8FF", 2, 0.7)
GEN["probe_staff"] = _probe_staff
GEN["wrench"] = lambda: overlay(recolor(src("shears"), "E8853D", 1.0, 1.0),
                                recolor(src("iron_nugget"), "8B939C", 1.0, 0.95), (8, 40))
def _fieldpack_terminal():
    b = recolor(src("book"), "3A4048", 0.8, 0.85)
    scr = Image.new("RGBA", (64, 64), (0,0,0,0))
    d = ImageDraw.Draw(scr)
    d.rectangle([22, 18, 46, 40], fill=(79, 195, 217, 235), outline=(15, 30, 38, 255), width=2)
    for i, yy in enumerate([22, 27, 32, 37]):
        d.line([25, yy, 43 - i*4, yy], fill=(10, 40, 52, 200), width=2)
    return overlay(b, scr)
GEN["fieldpack_terminal"] = _fieldpack_terminal
def _tidal_calendar():
    b = recolor(src("book"), "2E4A6E", 0.9, 0.9)
    moon = recolor(src("clock_16"), "D9E8F2", 0.5, 1.15)
    m2 = moon.copy(); m2 = ImageEnhance.Brightness(m2).enhance(0.85)
    b = overlay(b, moon, (14, 16)); b = overlay(b, mirror_h(m2), (30, 30))
    return b
GEN["tidal_calendar"] = _tidal_calendar
GEN["rhythmic_book"] = lambda: glow(recolor(src("enchanted_book"), "8A5FD0", 1.0, 1.0), "B08FFF", 3, 0.55)
def _giant_blueprint():
    b = recolor(src("map"), "2E5A6E", 0.85, 0.9)
    d = ImageDraw.Draw(b, "RGBA")
    for i in range(8, 64, 8):
        d.line([i, 10, i, 58], fill=(200, 240, 250, 120), width=1)
        d.line([8, i, 58, i], fill=(200, 240, 250, 120), width=1)
    d.rectangle([24, 22, 44, 42], outline=(240, 220, 120, 220), width=2)
    return b
GEN["giant_blueprint"] = _giant_blueprint
GEN["spectrum_module"] = lambda: glow(recolor(src("spyglass"), "3A5A7A", 1.0, 1.0), "4FC3D9", 2, 0.6)

# ── 8. 升级芯（芯片：金属底+引脚+品阶宝石） ──
UPGRADES = {
    "upgrade_efficiency_1": ("E8C53D", "amber"),
    "upgrade_efficiency_2": ("E8C53D", "gold"),
    "upgrade_efficiency_3": ("E8C53D", "diamond"),
    "upgrade_field_capacity": ("8FD3E8", "diamond"),
    "upgrade_rectifier": ("B06FE0", "amethyst"),
    "upgrade_isolation": ("6BCB77", "emerald"),
    "upgrade_superconductor": ("5FE0D9", "diamond"),
}
GEM_SRC = {"amber": "gold_nugget", "gold": "gold_ingot", "diamond": "diamond", "amethyst": "amethyst_shard", "emerald": "emerald"}
def _chip(accent, gem):
    body = recolor(src("warped_fungus_on_a_stick"), "2E3440", 0.7, 0.8)
    plate = recolor(src("music_disc_cat"), "22303A", 0.9, 0.85)
    body = overlay(body, plate, (12, 18))
    g = recolor(src(GEM_SRC[gem]), accent, 1.1, 1.15)
    body = glow(overlay(body, g, (24, 26)), accent, 2, 0.55)
    return border_frame(body, accent, 1, 13)
for name, (accent, gem) in UPGRADES.items():
    GEN[name] = lambda a=accent, gm=gem: _chip(a, gm)

# ── 9. 杂项 ──
def _compressed_air():
    bottle = recolor(src("glass_bottle"), "C9D6E0", 0.35, 1.0)
    fill = vgrad(Image.new("RGBA", (64,64),(0,0,0,0)), "9AC8E8", "4A88B8", 190)
    # 仅瓶体内部填充：以瓶子 alpha 下半部为掩码
    mask = bottle.split()[3].point(lambda v: v if v > 100 else 0)
    inner = Image.new("RGBA", (64,64), (0,0,0,0))
    inner.paste(fill, (0,0), mask)
    inner = inner.crop((0, 24, 64, 64))  # 下 2/3 才是液体
    bottle.alpha_composite(inner, (0, 24))
    return glow(bottle, "BEE8FF", 2, 0.35)
GEN["compressed_air"] = _compressed_air
GEN["inert_concentrate2"] = GEN["inert_concentrate"]  # 兼容别名（如无此条目自动跳过）

if __name__ == "__main__":
    only = sys.argv[1:] if len(sys.argv) > 1 else None
    n = 0
    for name, fn in GEN.items():
        if name == "inert_concentrate2":
            continue
        if only and name not in only:
            continue
        save(fn(), name)
        n += 1
    print(f"\n{n} textures generated at 64x -> {OUT}")
