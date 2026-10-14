# -*- coding: utf-8 -*-
"""
场域工艺 贴图生成管线(§16.2 资产管线：基础贴图 + 程序化变体批量生成)。
运行：python _texwork/gen_textures.py
产出：src/main/resources/assets/fieldworks/textures/**
风格(§16.1)：深空石墨底 + 五线发光色(能源琥珀/材料银白/场学冷青/环境翠绿/便利紫) + 等级皮递增。
"""
import os
from PIL import Image, ImageDraw

ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources', 'assets', 'fieldworks', 'textures')

# ── 调色板(§15.3) ──
GRAPHITE = (26, 29, 36)
GRAPHITE_L = (38, 42, 52)
GRAPHITE_D = (18, 20, 26)
STEEL = (140, 148, 160)
STEEL_D = (96, 104, 118)
AMBER = (232, 163, 61)      # 能源
SILVER = (216, 222, 233)    # 材料
CYAN = (79, 195, 217)       # 场学
GREEN = (107, 203, 119)     # 环境
PURPLE = (177, 140, 255)    # 便利
GOLD = (232, 163, 61)

FAMILY_COLOR = {
    'energy': AMBER, 'material': SILVER, 'field': CYAN,
    'environment': GREEN, 'convenience': PURPLE,
}


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def shade(c, f):
    return tuple(max(0, min(255, int(v * f))) for v in c)


# ═══════════ 机器方块贴图(五面：front/top/side；bottom 通用) ═══════════

def machine_texture(family, tier, glow=True, variant='front'):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    accent = FAMILY_COLOR[family]
    # 机身底
    d.rectangle([0, 0, 15, 15], fill=GRAPHITE)
    for i in range(16):
        px(img, i, 0, GRAPHITE_L)
        px(img, i, 15, GRAPHITE_D)
        px(img, 0, i, GRAPHITE_L)
        px(img, 15, i, GRAPHITE_D)
    # 铆钉四角
    for cx, cy in [(2, 2), (13, 2), (2, 13), (13, 13)]:
        px(img, cx, cy, STEEL_D)
    if variant == 'top':
        # 排热格栅
        for y in range(3, 13, 2):
            d.rectangle([3, y, 12, y], fill=GRAPHITE_L)
        for x in range(3, 13, 3):
            d.rectangle([x, 3, x, 12], fill=GRAPHITE_D)
        return img
    if variant == 'side':
        d.rectangle([3, 3, 12, 12], fill=GRAPHITE_L)
        d.rectangle([3, 3, 12, 3], fill=shade(GRAPHITE_L, 1.2))
        return img
    # front：主面板
    d.rectangle([3, 3, 12, 12], fill=(GRAPHITE_L))
    # 发光体(§16.1 每机一处主发光体；等级皮：数量/面积随级)
    glow_count = min(tier, 5)
    if glow:
        band_y = 5
        d.rectangle([4, band_y, 4 + 3 + glow_count, band_y + 1], fill=accent)
        for i in range(glow_count + 1):
            px(img, 4 + i, band_y - 1, shade(accent, 0.7))
        # 品牌铭牌
        d.rectangle([4, 9, 11, 11], fill=shade(GRAPHITE, 0.8))
        d.rectangle([4, 9, 4, 11], fill=accent)
        # 指示灯
        px(img, 11, 4, GREEN)
    return img


def pipe_texture(gas=True):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    base = CYAN if gas else (120, 160, 220)
    # 中心十字管体
    for a, b in [((6, 0), (9, 15)), ((0, 6), (15, 9))]:
        d.rectangle([a[0], a[1], b[0], b[1]], fill=GRAPHITE_L)
    d.rectangle([6, 6, 9, 9], fill=STEEL_D)
    d.rectangle([6, 6, 6, 6], fill=base)
    d.rectangle([9, 6, 9, 6], fill=shade(base, 0.6))
    return img


def frame_texture(kind):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    colors = {'frame': (60, 80, 200), 'injector': (60, 120, 255), 'coupler': (160, 100, 255),
              'constrainer': (100, 220, 130), 'stabilizer': (200, 205, 215)}
    c = colors[kind]
    d.rectangle([0, 0, 15, 15], fill=None)
    # 桁架网
    for i in range(0, 16, 5):
        d.rectangle([i, 0, i + 1, 15], fill=shade(GRAPHITE_L, 1.1))
        d.rectangle([0, i, 15, i + 1], fill=shade(GRAPHITE_L, 1.1))
    d.rectangle([1, 1, 2, 2], fill=c)
    d.rectangle([13, 1, 14, 2], fill=c)
    d.rectangle([1, 13, 2, 14], fill=c)
    d.rectangle([13, 13, 14, 14], fill=c)
    if kind == 'injector':
        d.rectangle([6, 6, 9, 9], fill=c)
    if kind == 'coupler':
        d.rectangle([4, 7, 11, 8], fill=c)
    if kind == 'constrainer':
        d.rectangle([6, 3, 9, 12], outline=c)
    if kind == 'stabilizer':
        d.ellipse([4, 4, 11, 11], outline=c)
    return img


def simple_block_texture(base, pattern='plain', accent=None):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, 15, 15], fill=base)
    for i in range(16):
        px(img, i, 0, shade(base, 1.25))
        px(img, 0, i, shade(base, 1.25))
        px(img, i, 15, shade(base, 0.7))
        px(img, 15, i, shade(base, 0.7))
    if pattern == 'truss':   # 强化桁架
        for i in range(0, 16, 4):
            d.line([(i, 0), (15, i)], fill=shade(base, 1.3))
            d.line([(0, i), (i, 15)], fill=shade(base, 1.3))
    elif pattern == 'shell':  # 钢外壳板
        d.rectangle([2, 2, 13, 13], fill=shade(base, 1.1))
        px(img, 3, 3, shade(base, 1.35))
    elif pattern == 'glass':  # 观测玻璃
        d.rectangle([0, 0, 15, 15], fill=(180, 210, 235, 110))
        d.rectangle([0, 0, 15, 1], fill=(220, 235, 250, 180))
    elif pattern == 'ore':    # 矿石
        for spots in [(3, 3), (9, 4), (5, 9), (11, 10), (7, 12)]:
            x, y = spots
            d.rectangle([x, y, x + 2, y + 2], fill=accent)
            px(img, x, y, shade(accent, 1.4))
    elif pattern == 'crystal':
        for spots in [(4, 4), (10, 3), (6, 10), (11, 11)]:
            x, y = spots
            d.line([(x, y + 3), (x + 1, y), (x + 2, y + 3)], fill=accent)
    elif pattern == 'plate':  # 铭牌
        d.rectangle([3, 5, 12, 10], fill=shade(base, 0.75))
        d.rectangle([3, 5, 3, 10], fill=accent or AMBER)
    return img


# ═══════════ 物品图标(§16.3 32×32 品阶框思路) ═══════════

def item_ingot(color, size=16):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    s = size / 16.0
    d.polygon([(3, 10), (12, 6), (13, 9), (4, 13)], fill=shade(color, 1.15))
    d.polygon([(4, 13), (13, 9), (13, 12), (4, 16)], fill=shade(color, 0.8))
    d.polygon([(3, 10), (4, 13), (13, 12), (12, 9)], fill=color)
    return img


def item_dust(color, size=16):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for spots in [(4, 9), (7, 11), (10, 8), (6, 6), (9, 12), (11, 11), (5, 12)]:
        d.rectangle([spots[0], spots[1], spots[0] + 1, spots[1] + 1], fill=color)
        px(img, spots[0], spots[1], shade(color, 1.4))
    return img


def item_crystal(color, size=16):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.polygon([(8, 1), (12, 6), (10, 14), (6, 14), (4, 6)], fill=shade(color, 0.85))
    d.polygon([(8, 1), (8, 14), (6, 14), (4, 6)], fill=color)
    d.polygon([(8, 1), (12, 6), (10, 14), (8, 14)], fill=shade(color, 1.2))
    return img


def item_gear(color, size=16):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.ellipse([3, 3, 12, 12], fill=color)
    d.ellipse([6, 6, 9, 9], fill=GRAPHITE)
    for t in range(8):
        import math
        a = t * math.pi / 4
        cx, cy = 8 + 5.2 * math.cos(a), 8 + 5.2 * math.sin(a)
        d.rectangle([cx - 1, cy - 1, cx + 1, cy + 1], fill=shade(color, 1.2))
    return img


def item_coil(color, size=16):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for y in range(3, 12, 2):
        d.arc([3, y - 2, 12, y + 4], 0, 360, fill=color)
    d.rectangle([3, 2, 4, 13], fill=shade(color, 0.6))
    d.rectangle([11, 2, 12, 13], fill=shade(color, 0.6))
    return img


def item_cell(color, size=16):
    """气体罐/压缩空气瓶(§16.3 瓶罐统一轮廓+色码)。"""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([6, 1, 9, 3], fill=STEEL_D)
    d.rounded_rectangle([5, 3, 10, 14], radius=2, fill=GRAPHITE_L)
    d.rectangle([5, 6, 10, 11], fill=color)
    d.rectangle([6, 4, 7, 5], fill=shade(color, 1.3))
    return img


def item_tool(kind, size=16):
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    if kind == 'staff':   # 探测杖
        d.rectangle([7, 3, 8, 13], fill=(110, 78, 45))
        d.ellipse([5, 1, 10, 6], outline=CYAN)
        d.rectangle([7, 2, 8, 3], fill=CYAN)
    elif kind == 'wrench':
        d.line([(4, 12), (11, 5)], fill=STEEL, width=2)
        d.ellipse([9, 2, 14, 7], outline=STEEL, width=2)
        d.ellipse([2, 10, 7, 15], outline=STEEL_D, width=2)
    elif kind == 'terminal':
        d.rectangle([2, 3, 13, 13], fill=GRAPHITE_L)
        d.rectangle([3, 4, 12, 9], fill=CYAN)
        d.rectangle([3, 10, 9, 12], fill=shade(CYAN, 0.5))
    elif kind == 'calendar':
        d.rectangle([2, 2, 13, 14], fill=(235, 230, 215))
        d.rectangle([2, 2, 13, 4], fill=STEEL_D)
        d.ellipse([6, 6, 10, 10], fill=GOLD)
        px(img, 8, 8, (250, 250, 240))
    elif kind == 'book':
        d.rectangle([3, 2, 12, 14], fill=(60, 40, 80))
        d.rectangle([4, 3, 11, 13], fill=(120, 80, 180))
        d.rectangle([4, 7, 11, 8], fill=GOLD)
    return img


def item_chip(color, size=16, tier=1):
    """升级芯(§10.4)。"""
    img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([3, 3, 12, 12], fill=GRAPHITE_L)
    d.rectangle([5, 5, 10, 10], fill=color)
    for i in range(3 + tier):
        px(img, 4 + i * 2, 2, GOLD)
        px(img, 4 + i * 2, 13, GOLD)
    return img


# ═══════════ GUI 图集(§15.3 176×166 背景 + 侧栏贴片) ═══════════

def gui_texture():
    img = Image.new('RGBA', (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    # 背景：深空石墨
    d.rectangle([0, 0, 175, 165], fill=(26, 29, 36, 245))
    for y in range(0, 166, 4):
        d.rectangle([0, y, 175, y], fill=(28, 31, 39, 255))
    # 边框：琥珀强调 + 冷青角标(§15.3)
    d.rectangle([0, 0, 175, 165], outline=(58, 64, 78))
    d.rectangle([2, 2, 173, 163], outline=(44, 49, 61))
    for x in range(4, 172, 12):
        px(img, x, 0, AMBER)
        px(img, x, 165, AMBER)
    # 槽位(输入 ×3 @44..；输出 @116,35；升级 ×4 @152,8..；玩家背包 8..,84..)
    for slot in [(44, 35), (62, 35), (80, 35), (116, 35)]:
        d.rectangle([slot[0] - 1, slot[1] - 1, slot[0] + 16, slot[1] + 16], fill=(18, 20, 26))
        d.rectangle([slot[0], slot[1], slot[0] + 15, slot[1] + 15], fill=(36, 40, 50))
    for i in range(4):
        y = 8 + i * 18
        d.rectangle([151, y - 1, 168, y + 16], fill=(18, 20, 26))
        d.rectangle([152, y, 167, y + 15], fill=(36, 40, 50))
        d.rectangle([152, y, 152, y + 15], fill=(60, 52, 36))
    for row in range(3):
        for col in range(9):
            x, y = 8 + col * 18, 84 + row * 18
            d.rectangle([x - 1, y - 1, x + 16, y + 16], fill=(18, 20, 26))
            d.rectangle([x, y, x + 15, y + 15], fill=(34, 38, 47))
    for col in range(9):
        x, y = 8 + col * 18, 142
        d.rectangle([x - 1, y - 1, x + 16, y + 16], fill=(18, 20, 26))
        d.rectangle([x, y, x + 15, y + 15], fill=(34, 38, 47))
    # 能量条槽 @7,20 12×40
    d.rectangle([6, 19, 18, 61], fill=(18, 20, 26))
    d.rectangle([7, 20, 17, 60], fill=(24, 27, 34))
    # 进度槽 @79,35 24×4
    d.rectangle([78, 34, 103, 40], fill=(18, 20, 26))
    # 侧栏贴片(右移 176 区，供样式 srcRect 引用)
    d.rectangle([180, 0, 203, 16], fill=(CYAN[0], CYAN[1], CYAN[2], 255))       # 进度填充
    d.rectangle([180, 20, 191, 59], fill=(AMBER[0], AMBER[1], AMBER[2], 255))   # 能量填充
    d.rectangle([180, 64, 219, 68], fill=(GREEN[0], GREEN[1], GREEN[2], 255))   # Fit 好
    d.rectangle([220, 64, 259, 68], fill=(PURPLE[0], PURPLE[1], PURPLE[2], 255))
    return img


# ═══════════ 生成清单 ═══════════

def save(img, *path):
    p = os.path.join(ROOT, *path)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    return p


def main():
    count = 0
    # 机器五面贴图(spec 表：与 MachineSpecs 对齐)
    MACHINES = [
        # (name, family, tier)
        ('steam_generator', 'energy', 1), ('part_bench', 'material', 1),
        ('infinite_water_pump', 'environment', 1),
        ('electrolyzer_1', 'field', 1), ('electrolyzer_2', 'field', 2), ('electrolyzer_3', 'field', 3),
        ('plasma_electrolysis_matrix', 'field', 5),
        ('blast_furnace', 'energy', 1), ('compressor', 'material', 1),
        ('distillation_tower', 'material', 2), ('blower', 'environment', 1),
        ('hydrogen_shaft_furnace', 'energy', 2), ('steam_turbine', 'energy', 2),
        ('assembler', 'material', 2), ('condenser', 'environment', 1), ('recycler', 'material', 1),
        ('magnetizer', 'field', 2), ('magnetic_separator', 'field', 2), ('gravity_separator', 'field', 2),
        ('deep_drill', 'material', 2), ('negative_pressure_catcher', 'field', 2),
        ('irradiation_chamber', 'field', 2), ('heat_exchanger', 'energy', 2),
        ('carbon_capturer', 'environment', 2), ('corrosion_trap', 'environment', 2),
        ('gas_tank_1', 'convenience', 1), ('gas_tank_4', 'convenience', 2), ('gas_tank_16', 'convenience', 3),
    ]
    for name, family, tier in MACHINES:
        save(machine_texture(family, tier, variant='front'), 'block', f'{name}_front.png'); count += 1
        save(machine_texture(family, tier, variant='top'), 'block', f'{name}_top.png'); count += 1
        save(machine_texture(family, tier, variant='side'), 'block', f'{name}_side.png'); count += 1
    # 观测设备(场学线)
    for name in ['magnetometer', 'tidal_meter', 'carbon_gauge', 'observation_lighthouse', 'spectrometer']:
        save(machine_texture('field', 2, variant='front'), 'block', f'{name}_front.png'); count += 1
        save(machine_texture('field', 2, variant='top'), 'block', f'{name}_top.png'); count += 1
        save(machine_texture('field', 2, variant='side'), 'block', f'{name}_side.png'); count += 1
    # 特殊方块
    save(machine_texture('environment', 1), 'block', 'stone_catcher_front.png'); count += 1
    save(simple_block_texture(GRAPHITE_L, 'plain'), 'block', 'stone_catcher_top.png'); count += 1
    save(simple_block_texture(GRAPHITE_D), 'block', 'stone_catcher_side.png'); count += 1
    save(pipe_texture(True), 'block', 'gas_pipe.png'); count += 1
    save(pipe_texture(False), 'block', 'fluid_pipe.png'); count += 1
    save(pipe_texture(False), 'block', 'pump.png'); count += 1
    save(pipe_texture(True), 'block', 'vent_pipe.png'); count += 1
    for kind in ['frame', 'injector', 'coupler', 'constrainer', 'stabilizer']:
        save(frame_texture(kind), 'block', f'field_{kind}.png'); count += 1
    save(simple_block_texture((70, 76, 90), 'truss'), 'block', 'reinforced_truss.png'); count += 1
    save(simple_block_texture((120, 128, 140), 'shell'), 'block', 'steel_shell.png'); count += 1
    save(simple_block_texture((150, 180, 210), 'glass'), 'block', 'observation_glass.png'); count += 1
    save(simple_block_texture((100, 108, 120), 'shell'), 'block', 'steel_casing.png'); count += 1
    save(simple_block_texture((120, 90, 160), 'plate', PURPLE), 'block', 'amethyst_plate.png'); count += 1
    # 261014 T9：巨物控制器/巨物核心贴图改由 gen_controllers_v3.py 生成(机器同语言三面贴图)
    # 矿物
    save(simple_block_texture((140, 120, 100), 'ore', (200, 140, 80)), 'block', 'bauxite_ore.png'); count += 1
    save(simple_block_texture((90, 90, 95), 'ore', (200, 140, 80)), 'block', 'deepslate_bauxite_ore.png'); count += 1
    save(simple_block_texture((90, 90, 95), 'ore', (220, 200, 90)), 'block', 'monazite_ore.png'); count += 1
    save(simple_block_texture((60, 55, 70), 'crystal', (170, 120, 240)), 'block', 'resonant_cluster.png'); count += 1
    save(simple_block_texture((150, 150, 160)), 'block', 'sky_platform.png'); count += 1
    save(simple_block_texture((90, 70, 120), 'plate', CYAN), 'block', 'harmonic_chamber.png'); count += 1
    save(simple_block_texture((90, 70, 120), 'plate', PURPLE), 'block', 'harmonic_enchant_pod.png'); count += 1
    save(simple_block_texture((60, 90, 70), 'plate', GREEN), 'block', 'tame_generator.png'); count += 1
    save(simple_block_texture((110, 60, 110), 'plate', PURPLE), 'block', 'phase_item_bridge.png'); count += 1
    save(simple_block_texture((100, 90, 130), 'shell', PURPLE), 'block', 'resonant_drill.png'); count += 1
    save(simple_block_texture((70, 60, 90), 'crystal', CYAN), 'block', 'crystal_farm.png'); count += 1

    # ── 物品 ──
    INGOTS = [
        ('steel_ingot', STEEL), ('aluminum_ingot', (200, 205, 215)), ('stainless_steel_ingot', (188, 195, 205)),
        ('pure_iron_ingot', (225, 228, 235)), ('invar_ingot', (150, 148, 140)), ('permalloy_ingot', (190, 175, 210)),
        ('monel_ingot', (170, 175, 165)), ('aluminum_brass_ingot', (215, 185, 140)), ('flux_alloy_ingot', (120, 200, 230)),
        ('superconductor_alloy', (100, 245, 255)),
    ]
    for name, c in INGOTS:
        save(item_ingot(c), 'item', f'{name}.png'); count += 1
    save(item_dust((200, 190, 170)), 'item', 'raw_bauxite.png'); count += 1
    save(item_dust((220, 200, 90)), 'item', 'raw_monazite.png'); count += 1
    save(item_dust((235, 235, 245)), 'item', 'silicon_wafer.png'); count += 1
    save(item_dust((160, 160, 170)), 'item', 'carbon_black.png'); count += 1
    save(item_dust((230, 220, 120)), 'item', 'sulfur.png'); count += 1
    save(item_dust((180, 200, 160)), 'item', 'corrosion_salt.png'); count += 1
    save(item_dust((230, 160, 120)), 'item', 'mercury_fulminate.png'); count += 1
    CRYSTALS = [('resonant_crystal_raw', (150, 100, 220)), ('resonant_crystal_refined', (180, 130, 245)),
                ('resonant_crystal_ultra', (220, 190, 255)), ('resonant_shard', (130, 90, 190)),
                ('dry_ice', (200, 225, 240)), ('void_condensate', (60, 40, 90)),
                ('tuned_void_essence', (110, 85, 200)), ('void_lens', (80, 60, 130)),
                ('amethyst_lens', (180, 130, 230)), ('thorium_pellet', (160, 170, 90)),
                ('inert_concentrate', (200, 167, 227)), ('dry_ice2', (0, 0, 0))]
    for name, c in CRYSTALS[:11]:
        save(item_crystal(c), 'item', f'{name}.png'); count += 1
    save(item_gear(STEEL), 'item', 'steel_gear.png'); count += 1
    save(item_coil((200, 130, 70)), 'item', 'copper_coil.png'); count += 1
    save(item_coil(CYAN), 'item', 'field_wire.png'); count += 1
    save(item_crystal((100, 245, 255)), 'item', 'superconductor_ring.png'); count += 1
    save(item_ingot((235, 200, 120)), 'item', 'tuning_fork.png'); count += 1
    save(item_crystal((120, 220, 190)), 'item', 'tidal_resonator.png'); count += 1
    save(item_crystal((200, 150, 255)), 'item', 'amethyst_resonator.png'); count += 1
    save(item_ingot((120, 100, 150)), 'item', 'magnetized_core.png'); count += 1
    save(item_gear((90, 140, 200)), 'item', 'magnetized_core2.png'); count += 1
    # 部件
    PARTS = [('copper_coil', 'coil2'), ('piston_frame', 'gear2'), ('precision_bearing', 'gear3'),
             ('logic_circuit', 'chip2'), ('sensor_core', 'crystal2'), ('nozzle_assembly', 'cell2'),
             ('distillation_tray', 'cell3'), ('electromagnet_core', 'gear4'), ('turbine_blade', 'ingot2'),
             ('seal_flange', 'gear5'), ('resonator_shell', 'crystal3'), ('giant_blueprint', 'book2')]
    for name, kind in PARTS:
        if kind.startswith('coil'):
            save(item_coil((200, 140, 80)), 'item', f'{name}.png'); count += 1
        elif kind.startswith('gear'):
            save(item_gear((160, 168, 180)), 'item', f'{name}.png'); count += 1
        elif kind.startswith('chip'):
            save(item_chip(CYAN), 'item', f'{name}.png'); count += 1
        elif kind.startswith('crystal'):
            save(item_crystal((140, 190, 240)), 'item', f'{name}.png'); count += 1
        elif kind.startswith('cell'):
            save(item_cell((180, 190, 205)), 'item', f'{name}.png'); count += 1
        elif kind.startswith('ingot'):
            save(item_ingot((190, 200, 215)), 'item', f'{name}.png'); count += 1
        else:
            save(item_tool('book'), 'item', f'{name}.png'); count += 1
    # 气体罐(§16.3 瓶罐统一轮廓+色码)
    CELLS = [('compressed_air', 0xB8C9D9), ('resonance_fuel', 0xE8A33D)]
    for name, c in CELLS:
        r, g, b = (c >> 16) & 255, (c >> 8) & 255, c & 255
        save(item_cell((r, g, b)), 'item', f'{name}.png'); count += 1
    # 工具
    save(item_tool('staff'), 'item', 'probe_staff.png'); count += 1
    save(item_tool('wrench'), 'item', 'wrench.png'); count += 1
    save(item_tool('terminal'), 'item', 'fieldpack_terminal.png'); count += 1
    save(item_tool('calendar'), 'item', 'tidal_calendar.png'); count += 1
    save(item_tool('book'), 'item', 'rhythmic_book.png'); count += 1
    save(item_crystal((120, 200, 230)), 'item', 'spectrum_module.png'); count += 1
    # 升级芯(§10.4)
    save(item_chip(GREEN, tier=1), 'item', 'upgrade_efficiency_1.png'); count += 1
    save(item_chip(GREEN, tier=2), 'item', 'upgrade_efficiency_2.png'); count += 1
    save(item_chip(GREEN, tier=3), 'item', 'upgrade_efficiency_3.png'); count += 1
    save(item_chip(CYAN), 'item', 'upgrade_field_capacity.png'); count += 1
    save(item_chip(SILVER), 'item', 'upgrade_rectifier.png'); count += 1
    save(item_chip((200, 170, 120)), 'item', 'upgrade_isolation.png'); count += 1
    save(item_chip((100, 245, 255), tier=3), 'item', 'upgrade_superconductor.png'); count += 1

    # GUI
    save(gui_texture(), 'guis', 'machine.png'); count += 1

    # 流体贴图(still，12 气体)
    FLUIDS = [('compressed_air', 0xB8C9D9), ('oxygen', 0x6EC6FF), ('nitrogen', 0x9FB8AD),
              ('hydrogen', 0xE5F5A0), ('marsh_gas', 0x7FA05A), ('carbon_dioxide', 0x8A8A8A),
              ('inert_gas', 0xC9A7E3), ('sulfuric_dilute', 0xC8B55A), ('steam', 0xD7E4EC),
              ('void_essence', 0x2B1E3D), ('tuned_void_gas', 0x6D55C9), ('nether_gas', 0xB84A2B)]
    for name, c in FLUIDS:
        r, g, b = (c >> 16) & 255, (c >> 8) & 255, c & 255
        img = Image.new('RGBA', (16, 16), (r, g, b, 230))
        d = ImageDraw.Draw(img)
        for i in range(0, 16, 4):
            for j in range(0, 16, 4):
                if (i + j) % 8 == 0:
                    d.rectangle([i, j, i + 1, j + 1], fill=(min(255, r + 30), min(255, g + 30), min(255, b + 30), 235))
        # 26.x：方块图集只扫 textures/block/ —— 流体精灵必须放在 block/ 下(FluidModel Material 引用)
        save(img, 'block', f'fluid_{name}_still.png'); count += 1
        save(img, 'block', f'fluid_{name}_flow.png'); count += 1

    print(f'[gen_textures] generated {count} textures -> {os.path.abspath(ROOT)}')


if __name__ == '__main__':
    main()
