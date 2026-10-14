# -*- coding: utf-8 -*-
"""
场域工艺 数据生成器(§19.3 数据包分层)。
运行：python _texwork/gen_data.py
产出：src/main/resources/{data,assets}/fieldworks/**
  - recipes(§7 FieldProfile 全量)、advancements(§8.5)、worldgen(3 矿)、tags、
    blockstates/models、lang(zh_cn+en_us)、screens 样式、patchouli_books(§18)。
"""
import json
import os

BASE = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')


def wj(rel, data):
    p = os.path.join(BASE, rel)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    return p


def FID(path):
    return f'fieldworks:{path}'


def VID(path):
    return f'minecraft:{path}'


# ═══════════════ 方块/物品清单 ═══════════════

MACHINE_BLOCKS = [  # (id, family) — 有 facing 的 spec 机器
    'steam_generator', 'part_bench', 'infinite_water_pump', 'electrolyzer_1', 'electrolyzer_2', 'electrolyzer_3',
    'plasma_electrolysis_matrix', 'blast_furnace', 'compressor', 'distillation_tower',
    'blower', 'hydrogen_shaft_furnace', 'steam_turbine', 'assembler', 'condenser', 'recycler',
    'magnetizer', 'magnetic_separator', 'gravity_separator', 'deep_drill',
    'negative_pressure_catcher', 'irradiation_chamber', 'heat_exchanger',
    'carbon_capturer', 'corrosion_trap', 'gas_tank_1', 'gas_tank_4', 'gas_tank_16',
]
# 观测设备(DisplayBlock 无 facing 属性)：单独处理，简单 blockstate
DISPLAY_DEVICES = ['magnetometer', 'tidal_meter', 'carbon_gauge', 'observation_lighthouse', 'spectrometer']
# 261014 T9：巨物控制器与核心改多面贴图(机器同语言 front/side/top)，见 gen_controllers_v3.py
CONTROLLER_BLOCKS = ['resonance_tower', 'deep_electrolysis_array', 'eco_dome', 'warp_anchor',
                     'harmonic_cathedral', 'planetary_array', 'giant_core']
SIMPLE_BLOCKS = [  # 单贴图方块
    # 261012 归属修订: gas_pipe/fluid_pipe/item_pipe/energy_pipe 归 gen_assets_v2.pipe_models()(multipart)，在此重跑会覆盖回静态单变体!
    ('stone_catcher', 'stone_catcher_front'),
    ('field_frame', 'field_frame'), ('field_injector', 'field_injector'), ('field_coupler', 'field_coupler'),
    ('field_constrainer', 'field_constrainer'), ('field_stabilizer', 'field_stabilizer'),
    ('reinforced_truss', 'reinforced_truss'), ('steel_shell', 'steel_shell'),
    ('observation_glass', 'observation_glass'), ('steel_casing', 'steel_casing'),
    ('amethyst_plate', 'amethyst_plate'),
    ('bauxite_ore', 'bauxite_ore'), ('deepslate_bauxite_ore', 'deepslate_bauxite_ore'),
    ('monazite_ore', 'monazite_ore'), ('resonant_cluster', 'resonant_cluster'),
    ('sky_platform', 'sky_platform'), ('harmonic_chamber', 'harmonic_chamber'),
    ('harmonic_enchant_pod', 'harmonic_enchant_pod'), ('tame_generator', 'tame_generator'),
    ('phase_item_bridge', 'phase_item_bridge'), ('resonant_drill', 'resonant_drill'),
    ('crystal_farm', 'crystal_farm'),
]
ITEMS = [
    'probe_staff', 'spectrum_module', 'fieldpack_terminal', 'tidal_calendar', 'wrench',
    'steel_ingot', 'aluminum_ingot', 'silicon_wafer', 'compressed_air', 'carbon_black',
    'stainless_steel_ingot', 'pure_iron_ingot', 'invar_ingot', 'permalloy_ingot',
    'monel_ingot', 'aluminum_brass_ingot',
    'resonant_crystal_raw', 'resonant_crystal_refined', 'resonant_crystal_ultra',
    'magnetized_core', 'field_wire', 'flux_alloy_ingot', 'thorium_pellet', 'dry_ice',
    'inert_concentrate', 'amethyst_lens', 'sulfur', 'corrosion_salt',
    'void_condensate', 'tuned_void_essence', 'void_lens', 'tuning_fork',
    'tidal_resonator', 'amethyst_resonator',
    'superconductor_alloy', 'superconductor_ring',
    'copper_coil', 'steel_gear', 'piston_frame', 'precision_bearing', 'logic_circuit',
    'sensor_core', 'nozzle_assembly', 'distillation_tray', 'electromagnet_core',
    'turbine_blade', 'seal_flange', 'resonator_shell',
    'raw_bauxite', 'raw_monazite', 'resonant_shard',
    'upgrade_efficiency_1', 'upgrade_efficiency_2', 'upgrade_efficiency_3',
    'upgrade_field_capacity', 'upgrade_rectifier', 'upgrade_isolation', 'upgrade_superconductor',
    'resonance_fuel', 'mercury_fulminate', 'rhythmic_book', 'giant_blueprint',
    # 261014 T10 场学饰品
    'resonant_band', 'magnetic_lens', 'fieldline_amulet', 'flux_belt',
    'tidal_charm', 'void_step_ring', 'superconductor_core',
]


# ═══════════════ blockstates + models ═══════════════

def gen_blockstates_models():
    n = 0
    for m in MACHINE_BLOCKS:
        # 模型：cube 六面贴图(§16.1 五面语言)
        wj(f'assets/fieldworks/models/block/{m}.json', {
            'parent': 'minecraft:block/cube',
            'textures': {
                'particle': FID(f'block/{m}_front'),
                'north': FID(f'block/{m}_front'), 'south': FID(f'block/{m}_side'),
                'east': FID(f'block/{m}_side'), 'west': FID(f'block/{m}_side'),
                'up': FID(f'block/{m}_top'), 'down': FID('block/machine_bottom'),
            }})
        # 261012 归属修订: 机器 blockstate 归 gen_assets_v2.build_active()(facing×active 8 变体)，此处只写模型
        n += 1
    for m in DISPLAY_DEVICES:
        wj(f'assets/fieldworks/models/block/{m}.json', {
            'parent': 'minecraft:block/cube',
            'textures': {
                'particle': FID(f'block/{m}_front'),
                'north': FID(f'block/{m}_front'), 'south': FID(f'block/{m}_side'),
                'east': FID(f'block/{m}_side'), 'west': FID(f'block/{m}_side'),
                'up': FID(f'block/{m}_top'), 'down': FID('block/machine_bottom'),
            }})
        wj(f'assets/fieldworks/blockstates/{m}.json', {'variants': {'': {'model': FID(f'block/{m}')}}})
        n += 2
    # 共享底面
    from PIL import Image, ImageDraw
    p = os.path.join(BASE, 'assets/fieldworks/textures/block/machine_bottom.png')
    img = Image.new('RGBA', (16, 16), (18, 20, 26, 255))
    d = ImageDraw.Draw(img)
    for i in range(0, 16, 4):
        d.rectangle([i, 0, i, 15], fill=(22, 24, 31, 255))
    os.makedirs(os.path.dirname(p), exist_ok=True)
    img.save(p)
    for name, tex in SIMPLE_BLOCKS:
        wj(f'assets/fieldworks/models/block/{name}.json', {
            'parent': 'minecraft:block/cube_all',
            'textures': {'all': FID(f'block/{tex}')}})
        wj(f'assets/fieldworks/blockstates/{name}.json', {'variants': {'': {'model': FID(f'block/{name}')}}})
        n += 2
    # 261014 T9：巨物控制器/核心 五面贴图模型(机器同语言；贴图由 gen_controllers_v3.py 生成)
    for name in CONTROLLER_BLOCKS:
        wj(f'assets/fieldworks/models/block/{name}.json', {
            'parent': 'minecraft:block/cube',
            'textures': {
                'particle': FID(f'block/{name}_front'),
                'north': FID(f'block/{name}_front'), 'south': FID(f'block/{name}_side'),
                'east': FID(f'block/{name}_side'), 'west': FID(f'block/{name}_side'),
                'up': FID(f'block/{name}_top'), 'down': FID('block/machine_bottom')}})
        wj(f'assets/fieldworks/blockstates/{name}.json', {'variants': {'': {'model': FID(f'block/{name}')}}})
        n += 2
    # 物品模型
    for name in MACHINE_BLOCKS + [b[0] for b in SIMPLE_BLOCKS]:
        wj(f'assets/fieldworks/models/item/{name}.json', {'parent': FID(f'block/{name}')})
        n += 1
    for it in ITEMS:
        wj(f'assets/fieldworks/models/item/{it}.json', {
            'parent': 'minecraft:item/generated', 'textures': {'layer0': FID(f'item/{it}')}})
        n += 1
    # ── 物品定义(26.x 必须：assets/<ns>/items/<id>.json，缺失则物品渲染为紫黑棋盘) ──
    # 方块物品 → 指向方块模型(3D，同 vanilla furnace)；材料/工具 → 指向物品模型(2D)
    for name in MACHINE_BLOCKS + [b[0] for b in SIMPLE_BLOCKS]:
        wj(f'assets/fieldworks/items/{name}.json', {
            'model': {'type': 'minecraft:model', 'model': FID(f'block/{name}')}})
        n += 1
    for it in ITEMS:
        wj(f'assets/fieldworks/items/{it}.json', {
            'model': {'type': 'minecraft:model', 'model': FID(f'item/{it}')}})
        n += 1
    return n


# ═══════════════ 配方(§7/§8.3/§9) ═══════════════

# 261013 T2：机器 → 配方族镜像表(与 Java MachineRecipeFamily 严格一致；电解 4 机共族)。
FAMILIES = {
    'electrolysis': ['electrolyzer_1', 'electrolyzer_2', 'electrolyzer_3', 'plasma_electrolysis_matrix'],
    'part_crafting': ['part_bench'],
    'blast_smelting': ['blast_furnace'],
    'compressing': ['compressor'],
    'distilling': ['distillation_tower'],
    'condensing': ['condenser'],
    'assembling': ['assembler'],
    'recycling': ['recycler'],
    'magnetizing': ['magnetizer'],
    'magnetic_separating': ['magnetic_separator'],
    'gravity_separating': ['gravity_separator'],
    'deep_drilling': ['deep_drill'],
    'vacuum_catching': ['negative_pressure_catcher'],
    'irradiating': ['irradiation_chamber'],
    'heat_exchanging': ['heat_exchanger'],
    'shaft_smelting': ['hydrogen_shaft_furnace'],
    'corrosion_trapping': ['corrosion_trap'],
    'carbon_capturing': ['carbon_capturer'],
}
MACHINE_FAMILY = {m: f for f, ms in FAMILIES.items() for m in ms}


def family_of(machine):
    fam = MACHINE_FAMILY.get(machine)
    if fam is None:
        raise SystemExit(f'[gen_data] 机器 {machine} 不在任何配方族中——请同步 FAMILIES 与 MachineRecipeFamily')
    return fam


def MR(machines, inputs, outputs, energy=200, time=200, profile=None, atmo=None, fluid_in=None, fluid_out=None, chances=None):
    first = machines if isinstance(machines, str) else machines[0]
    r = {'type': FID(family_of(first)),
         'machines': [FID(m) for m in ([machines] if isinstance(machines, str) else machines)],
         'energy': energy, 'time': time}
    if inputs:
        r['inputs'] = inputs
    if outputs:
        r['outputs'] = outputs
    if chances:
        r['chances'] = chances
    if profile:
        r['field_profile'] = profile
    if atmo:
        r['atmosphere_cost'] = atmo
    if fluid_in:
        r['fluid_input'] = fluid_in
    if fluid_out:
        r['fluid_outputs'] = fluid_out
    return r


def item(path, ns='fieldworks', count=1):
    return {'id': f'{ns}:{path}', 'count': count}


def ing_item(path, ns='minecraft'):
    # 26.1.2 Ingredient 为 HolderSet codec：纯字符串
    return f'{ns}:{path}'


def ingc(path, count, ns='minecraft'):
    """261014 T6 计数配料：{"ingredient": str, "count": n}(Java InputEntry.CODEC 对象形态)。"""
    return {'ingredient': f'{ns}:{path}', 'count': count}


def ingc_fw(path, count):
    return ingc(path, count, ns='fieldworks')


def ing_tag(path, ns='fieldworks'):
    return f'#{ns}:{path}'


def FC(field, ideal=None, tolerance=40, penalty=0.5, weight=1.0, mn=None, mx=None):
    c = {'field': FID(field), 'tolerance': tolerance, 'penalty': penalty, 'weight': weight}
    if ideal is not None:
        c['ideal'] = ideal
    if mn is not None:
        c['min'] = mn
    if mx is not None:
        c['max'] = mx
    return c


def gen_recipes():
    n = 0
    R = {}

    # ── P0 原版合成(自举律 §7.5：纯原版材料) ──
    R['probe_staff'] = {'type': 'minecraft:crafting_shaped',
        'pattern': ['  n', ' s ', 's  '],
        'key': {'n': 'minecraft:iron_nugget', 's': 'minecraft:stick'},
        'result': item('probe_staff')}
    R['stone_catcher'] = {'type': 'minecraft:crafting_shaped',
        'pattern': ['cgc', 'gbg', 'cgc'],
        'key': {'c': 'minecraft:cobblestone', 'g': 'minecraft:glass',
                'b': 'minecraft:barrel'},
        'result': item('stone_catcher')}
    R['steam_generator'] = {'type': 'minecraft:crafting_shaped',
        'pattern': ['iii', 'ifi', 'ccc'],
        'key': {'i': 'minecraft:iron_ingot', 'f': 'minecraft:furnace',
                'c': 'minecraft:cobblestone'},
        'result': item('steam_generator')}
    R['part_bench'] = {'type': 'minecraft:crafting_shaped',
        'pattern': ['ici', 'ctc', 'ici'],
        'key': {'i': 'minecraft:iron_ingot', 'c': 'minecraft:copper_ingot',
                't': 'minecraft:crafting_table'},
        'result': item('part_bench')}
    R['wrench'] = {'type': 'minecraft:crafting_shaped',
        'pattern': ['i i', ' c ', 'i i'],
        'key': {'i': 'minecraft:iron_ingot', 'c': 'minecraft:copper_ingot'},
        'result': item('wrench')}
    # 管道三族(261014 T2：气液合并为流体管；物品/能量管新增)
    R['fluid_pipe'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['cgc'],
                       'key': {'c': 'minecraft:copper_ingot', 'g': 'minecraft:glass'},
                       'result': item('fluid_pipe', count=8)}
    R['item_pipe'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['igi'],
                      'key': {'i': 'minecraft:iron_ingot', 'g': 'minecraft:glass'},
                      'result': item('item_pipe', count=8)}
    R['energy_pipe'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['rgr'],
                        'key': {'r': 'minecraft:redstone', 'g': 'minecraft:gold_ingot'},
                        'result': item('energy_pipe', count=8)}
    R['sky_platform'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['sss', 's s', 'sss'],
                         'key': {'s': 'fieldworks:steel_casing'}, 'result': item('sky_platform', count=4)}
    R['steel_casing'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['sss', 's s', 'sss'],
                         'key': {'s': 'fieldworks:steel_ingot'}, 'result': item('steel_casing')}
    R['reinforced_truss'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['sss', 's s', 'sss'],
                             'key': {'s': 'fieldworks:steel_shell'}, 'result': item('reinforced_truss')}
    R['steel_shell'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['s s', 's s', 'sss'],
                        'key': {'s': 'fieldworks:steel_ingot'}, 'result': item('steel_shell', count=4)}
    R['observation_glass'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['ggg', 'gsg', 'ggg'],
                              'key': {'g': 'minecraft:glass', 's': 'fieldworks:steel_ingot'},
                              'result': item('observation_glass', count=8)}
    R['giant_core'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['rcr', 'cbc', 'rcr'],
                       'key': {'r': 'fieldworks:resonant_crystal_refined',
                               'c': 'fieldworks:steel_casing',
                               'b': 'minecraft:beacon'},
                       'result': item('giant_core')}
    # 人工场五件套(P3)
    R['field_frame'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['ttt', 't t', 'ttt'],
                        'key': {'t': 'fieldworks:reinforced_truss'}, 'result': item('field_frame', count=4)}
    R['field_injector'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['cec', 'ese', 'ccc'],
                           'key': {'c': 'fieldworks:steel_casing', 'e': 'fieldworks:electromagnet_core',
                                   's': 'fieldworks:field_wire'},
                           'result': item('field_injector')}
    R['field_coupler'] = {'type': 'minecraft:crafting_shaped', 'pattern': [' w ', 'wew', ' w '],
                          'key': {'w': 'fieldworks:field_wire', 'e': 'fieldworks:logic_circuit'},
                          'result': item('field_coupler')}
    R['field_constrainer'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['ini', 'nbn', 'ini'],
                              'key': {'i': 'fieldworks:invar_ingot', 'n': 'fieldworks:monel_ingot',
                                      'b': 'fieldworks:steel_casing'},
                              'result': item('field_constrainer')}
    R['field_stabilizer'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['isi', 'sbs', 'isi'],
                             'key': {'s': 'fieldworks:seal_flange', 'i': 'fieldworks:invar_ingot',
                                     'b': 'fieldworks:steel_casing'},
                             'result': item('field_stabilizer')}
    # 观测设备
    for dev in ['magnetometer', 'tidal_meter', 'carbon_gauge', 'spectrometer']:
        R[dev] = {'type': 'minecraft:crafting_shaped', 'pattern': ['igi', 'gcg', 'igi'],
                  'key': {'i': 'fieldworks:steel_ingot', 'g': 'minecraft:glass',
                          'c': 'fieldworks:sensor_core'},
                  'result': item(dev)}
    R['observation_lighthouse'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['glg', 'lbl', 'sss'],
                                   'key': {'g': 'minecraft:glowstone', 'l': 'fieldworks:logic_circuit',
                                           'b': 'fieldworks:steel_casing', 's': 'fieldworks:steel_shell'},
                                   'result': item('observation_lighthouse')}
    # 储气罐
    for i, cap in [(1, 16), (4, 8), (16, 4)]:
        R[f'gas_tank_{i}'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['igi', 'g g', 'igi'],
                              'key': {'i': 'minecraft:iron_ingot', 'g': 'minecraft:glass'},
                              'result': item(f'gas_tank_{i}', count=1)} if i == 1 else {
            'type': 'minecraft:crafting_shapeless',
            'ingredients': [f'fieldworks:gas_tank_{i // 4 if i != 4 else 1}', f'fieldworks:gas_tank_{i // 4 if i != 4 else 1}'],
            'result': item(f'gas_tank_{i}')}
    # 其他功能块
    R['harmonic_chamber'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['ala', 'lsl', 'asa'],
                             'key': {'a': 'minecraft:amethyst_shard', 'l': 'fieldworks:amethyst_lens',
                                     's': 'fieldworks:steel_casing'},
                             'result': item('harmonic_chamber')}
    R['harmonic_enchant_pod'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['aaa', 'acb', 'sss'],
                                 'key': {'a': 'minecraft:amethyst_shard', 'c': 'fieldworks:tuning_fork',
                                         'b': 'minecraft:bookshelf', 's': 'fieldworks:steel_ingot'},
                                 'result': item('harmonic_enchant_pod')}
    R['tame_generator'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['glg', 'lbl', 'ggg'],
                           'key': {'g': 'minecraft:glowstone_dust', 'l': 'fieldworks:logic_circuit',
                                   'b': 'fieldworks:steel_casing'},
                           'result': item('tame_generator')}
    R['phase_item_bridge'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['ese', 'scs', 'ese'],
                              'key': {'e': 'minecraft:ender_pearl', 's': 'fieldworks:steel_ingot',
                                      'c': 'fieldworks:logic_circuit'},
                              'result': item('phase_item_bridge')}
    R['resonant_drill'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['dnd', 'ncn', 'dnd'],
                           'key': {'d': 'minecraft:diamond', 'n': 'minecraft:netherite_ingot',
                                   'c': 'fieldworks:giant_core'},
                           'result': item('resonant_drill')}
    R['crystal_farm'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['asa', 'scs', 'sss'],
                         'key': {'a': 'minecraft:amethyst_block', 's': 'fieldworks:steel_casing',
                                 'c': 'fieldworks:resonant_crystal_raw'},
                         'result': item('crystal_farm')}
    # 升级芯(§10.4)
    for i in (1, 2, 3):
        R[f'upgrade_efficiency_{i}'] = {'type': 'minecraft:crafting_shapeless',
            'ingredients': (['minecraft:redstone'] * i + (['fieldworks:upgrade_efficiency_1'] if i == 2 else ['fieldworks:upgrade_efficiency_2'] if i == 3 else [])),
            'result': item(f'upgrade_efficiency_{i}')}
    R['upgrade_field_capacity'] = {'type': 'minecraft:crafting_shapeless',
        'ingredients': ['fieldworks:field_wire', 'minecraft:redstone', 'fieldworks:copper_coil'],
        'result': item('upgrade_field_capacity')}
    R['upgrade_rectifier'] = {'type': 'minecraft:crafting_shapeless',
        'ingredients': ['fieldworks:distillation_tray', 'fieldworks:logic_circuit'],
        'result': item('upgrade_rectifier')}
    R['upgrade_isolation'] = {'type': 'minecraft:crafting_shapeless',
        'ingredients': ['fieldworks:monel_ingot', 'fieldworks:seal_flange'],
        'result': item('upgrade_isolation')}
    R['upgrade_superconductor'] = {'type': 'minecraft:crafting_shapeless',
        'ingredients': ['fieldworks:superconductor_alloy', 'fieldworks:dry_ice'],
        'result': item('upgrade_superconductor')}

    # ── 部件台(电力合成；261014 T6 复合配方重设计：耗材大幅扩充+计数) ──
    R['part_copper_coil'] = MR('part_bench', [ingc('copper_ingot', 6), ingc('iron_ingot', 2)], [item('copper_coil')], 180, 140)
    R['part_piston_frame'] = MR('part_bench', [ingc('iron_ingot', 6), ingc('copper_ingot', 2), ing_tag('planks', 'minecraft')], [item('piston_frame')], 220, 160)
    R['part_sensor_core'] = MR('part_bench', [ingc('copper_ingot', 4), ingc('redstone', 4)], [item('sensor_core')], 180, 120)
    R['part_steel_gear'] = MR('part_bench', [ingc('steel_ingot', 4, 'fieldworks'), ingc('iron_ingot', 4)], [item('steel_gear')], 240, 160)
    R['part_precision_bearing'] = MR('part_bench', [ingc('steel_ingot', 3, 'fieldworks'), ingc('iron_ingot', 5)], [item('precision_bearing')], 240, 170)
    R['part_logic_circuit'] = MR('part_bench', [ingc('silicon_wafer', 2, 'fieldworks'), ingc('copper_ingot', 4), ingc('redstone', 2), ingc('carbon_black', 2, 'fieldworks')], [item('logic_circuit')], 300, 200)
    R['part_nozzle'] = MR('part_bench', [ingc('copper_ingot', 4), ingc('iron_ingot', 4)], [item('nozzle_assembly')], 220, 150)
    R['part_distillation_tray'] = MR('assembler', [ingc('steel_ingot', 4, 'fieldworks'), ingc_fw('nozzle_assembly', 2)], [item('distillation_tray')], 420, 200)
    R['part_electromagnet_core'] = MR('assembler', [ingc('pure_iron_ingot', 4, 'fieldworks'), ingc_fw('copper_coil', 2)], [item('electromagnet_core')], 520, 240)
    R['part_turbine_blade'] = MR('assembler', [ingc('aluminum_ingot', 4, 'fieldworks'), ingc_fw('precision_bearing', 1)], [item('turbine_blade')], 460, 220)
    R['part_seal_flange'] = MR('assembler', [ingc('invar_ingot', 4, 'fieldworks'), ingc_fw('piston_frame', 1)], [item('seal_flange')], 420, 200)
    R['part_resonator_shell'] = MR('assembler', [ingc('resonant_crystal_refined', 2, 'fieldworks'), ingc('steel_casing', 2, 'fieldworks')], [item('resonator_shell')], 720, 280)

    # ── P0 机器组装(261014：复合配方——机器显著更贵；铁槽位 ≥16 例证在电解器Ⅰ) ──
    R['m_electrolyzer_1'] = MR('part_bench', [ingc_fw('copper_coil', 2), ingc('iron_ingot', 16), ingc('glass', 4)], [item('electrolyzer_1')], 400, 240)
    R['m_blast_furnace'] = MR('part_bench', [ingc('cobblestone', 16), ingc('iron_ingot', 8), ingc_fw('copper_coil', 1)], [item('blast_furnace')], 380, 220)
    R['m_compressor'] = MR('part_bench', [ingc_fw('piston_frame', 2), ingc_fw('copper_coil', 2), ingc('iron_ingot', 8)], [item('compressor')], 420, 240)
    # ── 无限水泵(261014 T7：前中期量产型机械，耗材厚重) ──
    R['m_infinite_water_pump'] = MR('part_bench', [ingc('steel_casing', 2, 'fieldworks'), ingc_fw('turbine_blade', 1),
                                                   ingc_fw('seal_flange', 2), ingc_fw('copper_coil', 4),
                                                   ingc_fw('logic_circuit', 1), ingc('iron_ingot', 16)], [item('infinite_water_pump')], 800, 400)
    # ── P1 机器 ──
    R['m_distillation_tower'] = MR('assembler', [ingc_fw('distillation_tray', 4), ingc('steel_casing', 2, 'fieldworks'), ingc_fw('nozzle_assembly', 4)], [item('distillation_tower')], 800, 400)
    R['m_hydrogen_shaft_furnace'] = MR('assembler', [ingc('steel_casing', 4, 'fieldworks'), ingc_fw('nozzle_assembly', 4), ingc_fw('steel_gear', 2)], [item('hydrogen_shaft_furnace')], 800, 400)
    R['m_steam_turbine'] = MR('assembler', [ingc_fw('turbine_blade', 4), ingc_fw('precision_bearing', 2), ingc('steel_casing', 2, 'fieldworks')], [item('steam_turbine')], 800, 400)
    R['m_assembler'] = MR('part_bench', [ingc_fw('logic_circuit', 2), ingc('steel_casing', 2, 'fieldworks'),
                                         ingc_fw('piston_frame', 2), ingc_fw('steel_gear', 2), ingc_fw('copper_coil', 4)], [item('assembler')], 700, 360)
    R['m_blower'] = MR('part_bench', [ingc_fw('nozzle_assembly', 2), ingc_fw('copper_coil', 2), ingc('iron_ingot', 4)], [item('blower')], 360, 200)
    R['m_condenser'] = MR('part_bench', [ingc_fw('nozzle_assembly', 2), ingc('glass', 8), ingc('copper_ingot', 4)], [item('condenser')], 360, 200)
    R['m_recycler'] = MR('part_bench', [ingc_fw('piston_frame', 2), ingc('iron_ingot', 12), ingc_fw('logic_circuit', 1)], [item('recycler')], 380, 220)
    # ── P2 机器 ──
    R['m_magnetizer'] = MR('assembler', [ingc_fw('electromagnet_core', 2), ingc('steel_casing', 4, 'fieldworks')], [item('magnetizer')], 800, 380)
    R['m_magnetic_separator'] = MR('assembler', [ingc_fw('electromagnet_core', 2), ingc_fw('precision_bearing', 4)], [item('magnetic_separator')], 800, 380)
    R['m_gravity_separator'] = MR('assembler', [ingc_fw('turbine_blade', 2), ingc_fw('precision_bearing', 4)], [item('gravity_separator')], 800, 380)
    R['m_deep_drill'] = MR('assembler', [ingc_fw('steel_gear', 4), ingc_fw('piston_frame', 2), ingc('steel_casing', 4, 'fieldworks')], [item('deep_drill')], 900, 400)
    R['m_negative_pressure_catcher'] = MR('assembler', [ingc_fw('seal_flange', 4), ingc('glass', 8)], [item('negative_pressure_catcher')], 800, 360)
    R['m_irradiation_chamber'] = MR('assembler', [ingc_fw('thorium_pellet', 4), ingc('steel_casing', 4, 'fieldworks')], [item('irradiation_chamber')], 900, 400)
    R['m_heat_exchanger'] = MR('assembler', [ingc_fw('copper_coil', 4), ingc_fw('seal_flange', 4)], [item('heat_exchanger')], 700, 340)
    R['m_electrolyzer_2'] = MR('assembler', [ingc('steel_casing', 4, 'fieldworks'), ingc_fw('copper_coil', 4), ingc_fw('logic_circuit', 4)], [item('electrolyzer_2')], 900, 380)
    # ── P3 机器 ──
    R['m_electrolyzer_3'] = MR('assembler', [ingc_fw('electromagnet_core', 2), ingc_fw('permalloy_ingot', 4), ingc('steel_casing', 8, 'fieldworks')], [item('electrolyzer_3')], 1400, 440,
                               profile={'fields': [FC('magnetic', ideal=0.35, tolerance=0.4, penalty=0.8)]})
    R['m_plasma'] = MR('assembler', [ingc_fw('superconductor_ring', 1), ingc_fw('superconductor_alloy', 4), ingc_fw('resonator_shell', 4)], [item('plasma_electrolysis_matrix')], 2600, 620)
    R['m_carbon_capturer'] = MR('assembler', [ingc_fw('sensor_core', 2), ingc_fw('logic_circuit', 4), ingc('steel_casing', 4, 'fieldworks')], [item('carbon_capturer')], 1000, 400)
    R['m_corrosion_trap'] = MR('assembler', [ingc_fw('monel_ingot', 4), ingc('steel_casing', 4, 'fieldworks'), ingc_fw('seal_flange', 2)], [item('corrosion_trap')], 1000, 400)
    # ── 场学饰品(261014 T10：强人工场材料系) ──
    R['acc_resonant_band'] = MR('assembler', [ingc_fw('field_wire', 4), ingc_fw('resonant_crystal_refined', 2), ingc('gold_ingot', 2)], [item('resonant_band')], 700, 320)
    R['acc_magnetic_lens'] = MR('assembler', [ingc_fw('amethyst_lens', 1), ingc_fw('pure_iron_ingot', 4), ingc('copper_ingot', 4)], [item('magnetic_lens')], 700, 320)
    R['acc_fieldline_amulet'] = MR('assembler', [ingc_fw('field_wire', 4), ingc_fw('seal_flange', 2), ingc_fw('invar_ingot', 2)], [item('fieldline_amulet')], 750, 340)
    R['acc_flux_belt'] = MR('assembler', [ingc_fw('flux_alloy_ingot', 4), ingc_fw('logic_circuit', 2), ingc('gold_ingot', 4)], [item('flux_belt')], 850, 360)
    R['acc_tidal_charm'] = MR('assembler', [ingc_fw('tidal_resonator', 1), ingc('prismarine_shard', 8), ingc_fw('invar_ingot', 4)], [item('tidal_charm')], 950, 400)
    R['acc_void_step_ring'] = MR('assembler', [ingc_fw('void_lens', 1), ingc_fw('void_condensate', 4), ingc('ender_pearl', 4)], [item('void_step_ring')], 1200, 440)
    R['acc_superconductor_core'] = MR('assembler', [ingc_fw('superconductor_ring', 1), ingc_fw('superconductor_alloy', 4), ingc_fw('resonant_crystal_ultra', 8)], [item('superconductor_core')], 3000, 620)
    R['fieldpack_terminal'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['gcg', 'csc', 'gcg'],
        'key': {'g': 'minecraft:glowstone_dust', 'c': 'fieldworks:logic_circuit', 's': 'fieldworks:sensor_core'},
        'result': item('fieldpack_terminal')}
    R['tidal_calendar'] = {'type': 'minecraft:crafting_shaped', 'pattern': ['ppp', 'plp', 'ppp'],
        'key': {'p': 'minecraft:prismarine_shard', 'l': 'fieldworks:logic_circuit'},
        'result': item('tidal_calendar')}
    R['spectrum_module'] = {'type': 'minecraft:crafting_shapeless',
        'ingredients': ['fieldworks:sensor_core', 'minecraft:amethyst_shard'],
        'result': item('spectrum_module')}
    # ── 巨物蓝图 ──
    R['giant_blueprint'] = MR('assembler', [ingc_fw('resonant_crystal_refined', 2), ingc_fw('logic_circuit', 4)], [item('giant_blueprint')], 1200, 440)

    # ── 材料链(§9) ──
    # G1：钢(§7.4 富氧鼓风还原——BOF 现实主流工艺)
    R['steel'] = MR('blast_furnace', [ing_tag('compressed_air', 'fieldworks'), ing_item('iron_ingot', ns='minecraft')], [item('steel_ingot')], 300, 200,
                    profile={'fields': [FC('temperature', ideal=60, tolerance=60, penalty=0.5), FC('humidity', ideal=40, tolerance=70, penalty=0.3, weight=0.5)]},
                    atmo={'ox': -0.01})
    R['steel_h2'] = MR('hydrogen_shaft_furnace', [ing_item('iron_ingot', ns='minecraft')], [item('steel_ingot')], 320, 180,
                       fluid_in={'fluid': FID('hydrogen'), 'amount': 100},
                       profile={'fields': [FC('temperature', ideal=70, tolerance=60, penalty=0.5)]})
    R['silicon'] = MR('blast_furnace', [ing_item('quartz', 'minecraft'), ing_tag('compressed_air', 'fieldworks')], [item('silicon_wafer', count=2)], 260, 160,
                      profile={'fields': [FC('temperature', ideal=80, tolerance=50, penalty=0.6)]})
    R['aluminum'] = MR('blast_furnace', [ing_item('raw_bauxite', 'fieldworks'), ing_item('raw_bauxite', 'fieldworks')], [item('aluminum_ingot')], 400, 260,
                       profile={'fields': [FC('temperature', ideal=90, tolerance=50, penalty=0.6)]})
    # 电解(§10.2 分级)
    R['electrolysis_1'] = MR('electrolyzer_1', [ing_tag('compressed_air', 'fieldworks')], [], 400, 240,
                             fluid_out=[{'id': FID('oxygen'), 'amount': 200}, {'id': FID('nitrogen'), 'amount': 300}])
    R['electrolysis_2_water'] = MR('electrolyzer_2', [], [], 500, 200,
                                   fluid_in={'fluid': 'minecraft:water', 'amount': 200},
                                   fluid_out=[{'id': FID('hydrogen'), 'amount': 120}, {'id': FID('oxygen'), 'amount': 80}])
    R['electrolysis_3_inert'] = MR('electrolyzer_3', [ing_tag('compressed_air', 'fieldworks')], [], 600, 200,
                                   profile={'fields': [FC('magnetic', ideal=0.35, tolerance=0.45, penalty=0.8)]},
                                   fluid_out=[{'id': FID('inert_gas'), 'amount': 250}, {'id': FID('oxygen'), 'amount': 150}])
    R['electrolysis_void'] = MR('plasma_electrolysis_matrix', [], [item('void_condensate', count=1)], 2000, 400,
                                fluid_in={'fluid': FID('tuned_void_gas'), 'amount': 100},
                                profile={'fields': [FC('em_field', mn=40)]})
    # 分馏
    R['distill'] = MR('distillation_tower', [ing_tag('compressed_air', 'fieldworks')], [], 500, 300,
                      fluid_out=[{'id': FID('oxygen'), 'amount': 150}, {'id': FID('nitrogen'), 'amount': 250}, {'id': FID('inert_gas'), 'amount': 60}])
    # 压缩机批量空气
    R['compress_air'] = MR('compressor', [], [item('compressed_air', count=2)], 350, 160)
    # 冷凝(潮气→水)
    R['condense'] = MR('condenser', [], [], 240, 160,
                       profile={'fields': [FC('humidity', ideal=70, tolerance=60, penalty=0.7)]},
                       fluid_out=[{'id': 'minecraft:water', 'amount': 120}])
    # 热交换(废热→蒸汽，§8.4 无耗红利)
    R['heat_steam'] = MR('heat_exchanger', [], [], 100, 120,
                         profile={'fields': [FC('temperature', ideal=60, tolerance=45, penalty=0.8)]},
                         fluid_out=[{'id': FID('steam'), 'amount': 100}])
    # 回收炉(60% 返还，§10.3)
    R['recycle_gear'] = MR('recycler', [ing_item('steel_gear', 'fieldworks')], [item('iron_ingot', ns='minecraft', count=1)], 120, 80)
    R['recycle_coil'] = MR('recycler', [ing_item('copper_coil', 'fieldworks')], [item('copper_ingot', ns='minecraft', count=1)], 100, 80)
    # 磁化机(自然磁场选址，§12.4)
    R['magnetize'] = MR('magnetizer', [ing_item('iron_ingot', ns='minecraft'), ing_item('iron_ingot', ns='minecraft')], [item('magnetized_core')], 500, 300,
                        profile={'fields': [FC('magnetic', ideal=0.5, tolerance=0.5, penalty=0.9)]})
    R['pure_iron'] = MR('magnetizer', [ing_item('iron_ingot', ns='minecraft'), ing_item('iron_ingot', ns='minecraft'), ing_item('iron_ingot', ns='minecraft')], [item('pure_iron_ingot', count=2)], 700, 400,
                        profile={'fields': [FC('magnetic', ideal=0.5, tolerance=0.5, penalty=0.9)]})
    # 磁选/重力分选(月相窗口，§12.4)
    R['monazite_sep'] = MR('magnetic_separator', [ing_item('raw_monazite', 'fieldworks')], [item('thorium_pellet'), item('inert_concentrate')], 600, 300,
                           profile={'fields': [FC('magnetic', ideal=0.4, tolerance=0.5, penalty=0.8), FC('gravity', ideal=1.15, tolerance=0.3, penalty=0.5, weight=0.5)]},
                           chances=[1.0, 0.6])
    R['gravel_sep'] = MR('gravity_separator', [ing_item('gravel', 'minecraft')], [item('flint', ns='minecraft'), item('raw_monazite')], 400, 220,
                         profile={'fields': [FC('gravity', ideal=1.1, tolerance=0.25, penalty=0.6, weight=0.8)]},
                         chances=[0.9, 0.15])
    # 深钻(防 L8：能量+磨损成本)
    R['deep_iron'] = MR('deep_drill', [ing_item('deepslate', 'minecraft')], [item('raw_iron', ns='minecraft')], 800, 400,
                        profile={'fields': [FC('temperature', ideal=30, tolerance=60, penalty=0.4)]},
                        chances=[0.7])
    R['deep_monazite'] = MR('deep_drill', [ing_item('deepslate', 'minecraft')], [item('raw_monazite')], 800, 400, chances=[0.12])
    # 负压捕获(虚空界面/末地，§4.4)
    R['catch_void'] = MR('negative_pressure_catcher', [], [item('void_condensate')], 600, 320,
                         profile={'fields': [FC('vacuum', ideal=30, tolerance=2000, penalty=0.7)]})
    R['catch_tuned'] = MR('negative_pressure_catcher', [], [item('tuned_void_essence')], 700, 360,
                          profile={'fields': [FC('vacuum', ideal=30, tolerance=3000, penalty=0.7)]},
                          fluid_out=[{'id': FID('tuned_void_gas'), 'amount': 50}])
    # 辐照室
    R['irradiate_lens'] = MR('irradiation_chamber', [ing_item('amethyst_shard', 'minecraft'), ing_item('amethyst_shard', 'minecraft')], [item('amethyst_lens')], 500, 300,
                             profile={'fields': [FC('radiation', ideal=60, tolerance=80, penalty=0.7)]})
    R['irradiate_raw'] = MR('irradiation_chamber', [ing_item('resonant_shard', 'fieldworks'), ing_item('resonant_shard', 'fieldworks'), ing_item('resonant_shard', 'fieldworks'), ing_item('resonant_shard', 'fieldworks')], [item('resonant_crystal_raw')], 600, 400,
                            profile={'fields': [FC('radiation', ideal=40, tolerance=100, penalty=0.6)]})
    # 晶体提纯链
    R['crystal_refine'] = MR('electrolyzer_3', [ing_item('resonant_crystal_raw', 'fieldworks')], [item('resonant_crystal_refined')], 800, 400,
                             profile={'fields': [FC('magnetic', ideal=0.35, tolerance=0.4, penalty=0.8)]})
    R['crystal_ultra'] = MR('plasma_electrolysis_matrix', [ing_item('resonant_crystal_refined', 'fieldworks')], [item('resonant_crystal_ultra')], 1600, 500,
                            profile={'fields': [FC('em_field', mn=30)]})
    # G2 合金(§9.2 特性分化)
    R['stainless'] = MR('blast_furnace', [ingc('steel_ingot', 2, 'fieldworks'), ingc('iron_ingot', 2), ingc('carbon_black', 1, 'fieldworks')], [item('stainless_steel_ingot', count=2)], 420, 260,
                        profile={'fields': [FC('temperature', ideal=80, tolerance=50, penalty=0.6)]})
    R['invar'] = MR('blast_furnace', [ing_item('steel_ingot', 'fieldworks'), ing_item('iron_ingot', ns='minecraft')], [item('invar_ingot', count=2)], 400, 240,
                    profile={'fields': [FC('temperature', ideal=70, tolerance=50, penalty=0.6)]})
    R['permalloy'] = MR('assembler', [ing_item('steel_ingot', 'fieldworks'), ing_item('inert_concentrate', 'fieldworks'), ing_item('copper_ingot', 'minecraft')], [item('permalloy_ingot')], 500, 280)
    R['monel'] = MR('assembler', [ing_item('steel_ingot', 'fieldworks'), ing_item('copper_ingot', 'minecraft'), ing_item('corrosion_salt', 'fieldworks')], [item('monel_ingot')], 500, 280,
                    profile={'fields': [FC('humidity', ideal=70, tolerance=60, penalty=0.4, weight=0.5)]})
    R['aluminum_brass'] = MR('blast_furnace', [ing_item('aluminum_ingot', 'fieldworks'), ing_item('copper_ingot', 'minecraft')], [item('aluminum_brass_ingot', count=2)], 380, 220)
    # G3
    R['field_wire'] = MR('assembler', [ing_item('copper_ingot', 'minecraft'), ing_item('pure_iron_ingot', 'fieldworks')], [item('field_wire', count=2)], 500, 260)
    R['flux_alloy'] = MR('assembler', [ing_item('copper_ingot', 'minecraft'), ing_item('ender_pearl', 'minecraft')], [item('flux_alloy_ingot')], 600, 300)
    R['dry_ice'] = MR('compressor', [], [item('dry_ice')], 400, 200,
                      fluid_in={'fluid': FID('carbon_dioxide'), 'amount': 200},
                      profile={'fields': [FC('temperature', ideal=-10, tolerance=50, penalty=0.4, weight=0.5)]})
    # 碳捕获(§5.4 副产物)
    R['carbon_capture'] = MR('carbon_capturer', [], [item('carbon_black'), item('dry_ice')], 500, 300,
                             profile={'fields': [FC('carbon', ideal=400, tolerance=600, penalty=0.4, weight=0.5)]},
                             chances=[1.0, 0.5], atmo={'cr': 0})
    # 蚀捕器(§4.7 污染也是矿)
    R['corrosion_sulfur'] = MR('corrosion_trap', [], [item('sulfur'), item('corrosion_salt')], 450, 260,
                               profile={'fields': [FC('temperature', ideal=50, tolerance=70, penalty=0.4, weight=0.4)]},
                               chances=[1.0, 0.6], atmo={'cr': -0.02})
    R['marsh_catch'] = MR('corrosion_trap', [], [], 350, 200,
                          fluid_out=[{'id': FID('marsh_gas'), 'amount': 150}])
    R['nether_gas'] = MR('corrosion_trap', [], [], 500, 260,
                         fluid_out=[{'id': FID('nether_gas'), 'amount': 120}])
    # G4
    R['void_lens'] = MR('assembler', [ing_item('void_condensate', 'fieldworks'), ing_item('void_condensate', 'fieldworks'), ing_item('amethyst_lens', 'fieldworks')], [item('void_lens')], 1000, 400)
    R['tuning_fork'] = MR('assembler', [ing_item('tuned_void_essence', 'fieldworks'), ing_item('amethyst_shard', 'minecraft'), ing_item('gold_ingot', 'minecraft')], [item('tuning_fork')], 900, 400)
    R['amethyst_resonator'] = MR('assembler', [ing_item('amethyst_lens', 'fieldworks'), ing_item('resonant_crystal_refined', 'fieldworks')], [item('amethyst_resonator')], 800, 360)
    R['heart_of_sea'] = MR('assembler', [ing_item('prismarine_shard', 'minecraft'), ing_item('diamond', 'minecraft'), ing_item('corrosion_salt', 'fieldworks')], [item('heart_of_the_sea', ns='minecraft')], 900, 400,
                           profile={'fields': [FC('humidity', ideal=75, tolerance=40, penalty=0.6, weight=0.6)]})
    R['tidal_resonator'] = MR('assembler', [ing_item('heart_of_the_sea', 'minecraft'), ing_item('prismarine_shard', 'minecraft'), ing_item('invar_ingot', 'fieldworks')], [item('tidal_resonator')], 1000, 420)
    # G5(§9.2 超导)
    R['superconductor_alloy'] = MR('assembler', [ing_item('resonant_crystal_ultra', 'fieldworks'), ing_item('ghast_tear', 'minecraft')], [item('superconductor_alloy')], 2000, 600,
                                   profile={'fields': [FC('temperature', ideal=-30, tolerance=40, penalty=0.8), FC('vacuum', ideal=1000, tolerance=90000, penalty=0.6, weight=0.7)]})
    R['superconductor_ring'] = MR('assembler', [ing_item('superconductor_alloy', 'fieldworks'), ing_item('superconductor_alloy', 'fieldworks'), ing_item('invar_ingot', 'fieldworks'), ing_item('superconductor_alloy', 'fieldworks')], [item('superconductor_ring')], 3000, 800,
                                  profile={'fields': [FC('vacuum', ideal=1000, tolerance=90000, penalty=0.8, weight=1.0)]})
    # 谐振燃料(§13.3 三因子简化：晶体=燃料气/压缩空气=氧/硫=催化)
    R['resonance_fuel'] = MR('assembler', [ing_item('resonant_crystal_raw', 'fieldworks'), ing_tag('compressed_air', 'fieldworks'), ing_item('sulfur', 'fieldworks')], [item('resonance_fuel', count=2)], 600, 300)
    R['resonance_fuel_premium'] = MR('assembler', [ing_item('resonant_crystal_refined', 'fieldworks'), ing_tag('compressed_air', 'fieldworks'), ing_item('mercury_fulminate', 'fieldworks')], [item('resonance_fuel', count=4)], 900, 400)
    R['mercury_fulminate'] = MR('irradiation_chamber', [ing_item('gunpowder', 'minecraft'), ing_item('sulfur', 'fieldworks'), ing_item('gold_nugget', 'minecraft')], [item('mercury_fulminate')], 500, 300,
                                profile={'fields': [FC('radiation', ideal=30, tolerance=100, penalty=0.5, weight=0.4)]})
    # 稀有材料转换(§9.3)
    R['phantom_membrane'] = MR('assembler', [ing_item('leather', 'minecraft'), ing_item('leather', 'minecraft'), ing_item('ender_pearl', 'minecraft')], [item('phantom_membrane', ns='minecraft')], 700, 400,
                               profile={'fields': [FC('vacuum', ideal=1000, tolerance=90000, penalty=0.7, weight=0.8)]})
    R['ghast_tear_conv'] = MR('condenser', [], [item('ghast_tear', ns='minecraft')], 600, 400,
                              fluid_in={'fluid': FID('nether_gas'), 'amount': 200},
                              chances=[0.6])
    # 律动之书(§12.1)
    R['rhythmic_book'] = MR('assembler', [ing_item('enchanted_book', 'minecraft'), ing_item('resonant_crystal_refined', 'fieldworks'), ing_item('resonant_crystal_refined', 'fieldworks'), ing_item('gold_ingot', 'minecraft')], [item('rhythmic_book')], 1000, 500)
    # 巨物方块合成(图纸+核心)
    for g in ['resonance_tower', 'deep_electrolysis_array', 'eco_dome', 'warp_anchor', 'harmonic_cathedral', 'planetary_array']:
        R['g_' + g] = {'type': 'minecraft:crafting_shapeless',
                       'ingredients': ['fieldworks:giant_core', 'fieldworks:giant_blueprint'],
                       'result': item(g)}

    for name, r in R.items():
        wj(f'data/fieldworks/recipe/{name}.json', r)
        n += 1
    return n


# ═══════════════ 世界生成(3 矿，§9.2 克制) ═══════════════

GONE_BLOCKS = {'gas_pipe', 'pump', 'vent_pipe'}   # 261014 已删方块(掉落表/资产禁回写)


def gen_worldgen():
    n = 0
    ores = [
        ('bauxite_ore', 'minecraft:stone', 8, 10, -16, 64, 'fieldworks:raw_bauxite'),
        ('deepslate_bauxite_ore', 'minecraft:deepslate', 8, 8, -64, 0, 'fieldworks:raw_bauxite'),
        ('monazite_ore', 'minecraft:deepslate', 4, 3, -64, -16, 'fieldworks:raw_monazite'),
        ('resonant_cluster', 'minecraft:stone', 6, 4, -40, 60, 'fieldworks:resonant_shard'),
    ]
    for name, base, size, per_chunk, min_y, max_y, drop in ores:
        wj(f'data/fieldworks/worldgen/configured_feature/{name}.json', {
            'type': 'minecraft:ore',
            'config': {
                'size': size, 'discard_chance_on_air_exposure': 0.4,
                'targets': [{'target': {'predicate_type': 'minecraft:block_match', 'block': base},
                             'state': {'Name': f'fieldworks:{name}'}}],
            }})
        wj(f'data/fieldworks/worldgen/placed_feature/{name}.json', {
            'feature': f'fieldworks:{name}',
            'placement': [
                {'type': 'minecraft:count', 'count': per_chunk},
                {'type': 'minecraft:in_square'},
                {'type': 'minecraft:height_range', 'height': {'type': 'minecraft:trapezoid',
                    'min_inclusive': {'absolute': min_y}, 'max_inclusive': {'absolute': max_y}}},
                {'type': 'minecraft:biome'},
            ]})
        n += 2
    wj('data/fieldworks/neoforge/biome_modifier/ores.json', {
        'type': 'neoforge:add_features',
        'biomes': '#minecraft:is_overworld',
        'features': ['fieldworks:bauxite_ore', 'fieldworks:deepslate_bauxite_ore',
                     'fieldworks:monazite_ore', 'fieldworks:resonant_cluster'],
        'step': 'underground_ores'})
    n += 1
    # 掉落表(loot tables)
    for name, drop, count in [('bauxite_ore', 'raw_bauxite', 1), ('deepslate_bauxite_ore', 'raw_bauxite', 1),
                              ('monazite_ore', 'raw_monazite', 1), ('resonant_cluster', 'resonant_shard', 2)]:
        wj(f'data/fieldworks/loot_table/blocks/{name}.json', {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'fieldworks:{drop}',
                'functions': [{'function': 'minecraft:set_count', 'count': count},
                              {'function': 'minecraft:apply_bonus', 'enchantment': 'minecraft:fortune', 'formula': 'minecraft:ore_drops'},
                              {'function': 'minecraft:explosion_decay'}]}]}]})
        n += 1
    # 其余方块掉落(self drop；管道三族 261014 补齐)
    for name in MACHINE_BLOCKS + [b[0] for b in SIMPLE_BLOCKS] + CONTROLLER_BLOCKS + ['fluid_pipe', 'item_pipe', 'energy_pipe']:
        wj(f'data/fieldworks/loot_table/blocks/{name}.json', {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'fieldworks:{name}'}]}]})
        n += 1
    return n


# ═══════════════ 标签 ═══════════════

def gen_tags():
    n = 0
    wj('data/fieldworks/tags/item/compressed_air.json', {'values': ['fieldworks:compressed_air']})
    wj('data/fieldworks/tags/block/moss_like.json', {
        'values': ['minecraft:moss_block', 'minecraft:moss_carpet', 'minecraft:cave_vines', 'minecraft:big_dripleaf']})
    pickaxe = [b for b in MACHINE_BLOCKS] + [b[0] for b in SIMPLE_BLOCKS] + CONTROLLER_BLOCKS
    pickaxe += ['fluid_pipe', 'item_pipe', 'energy_pipe']   # 261014 管道三族可挖
    wj('data/minecraft/tags/block/mineable/pickaxe.json', {'values': [f'fieldworks:{b}' for b in pickaxe]})
    wj('data/minecraft/tags/block/needs_stone_tool.json', {
        'values': [f'fieldworks:{b}' for b in ['bauxite_ore', 'deepslate_bauxite_ore', 'monazite_ore', 'resonant_cluster']]})
    # 附魔书架效力(§12.1 紫晶铭牌)
    wj('data/minecraft/tags/block/enchantment_power_provider.json', {
        'values': [{'id': 'fieldworks:amethyst_plate', 'required': False}, 'minecraft:bookshelf']})
    # 261014 T10：Curios 槽位绑定(数据驱动零代码耦合)
    wj('data/curios/tags/item/ring.json', {'values': ['fieldworks:resonant_band', 'fieldworks:void_step_ring']})
    wj('data/curios/tags/item/necklace.json', {'values': ['fieldworks:tidal_charm', 'fieldworks:superconductor_core']})
    wj('data/curios/tags/item/belt.json', {'values': ['fieldworks:flux_belt']})
    wj('data/curios/tags/item/charm.json', {'values': ['fieldworks:fieldline_amulet', 'fieldworks:magnetic_lens']})
    n += 9
    return n


# ═══════════════ 进度(§8.5 前期 12 条) ═══════════════

def gen_advancements():
    n = 0
    # 谓词格式(26.1.2 对 vanilla story/iron_tools.json 核对)：
    #   conditions.items = [{"items": "ns:id"}]  ← 内层键 "items"(字符串)
    #   display.icon = {"id": "ns:id"}
    #   root.background = "minecraft:block/deepslate"(无 textures/ 前缀)
    # kind: 'inv' = 获得物品触发；'code' = 代码授予(触发器 impossible，由 AdvancementHelper 授予)
    advs = [
        ('probe', 'fieldworks:probe_staff', 'inv', 'fieldworks:probe_staff'),
        ('first_reading', 'fieldworks:probe_staff', 'code', None),
        ('air_caught', 'fieldworks:compressed_air', 'inv', 'fieldworks:compressed_air'),
        ('power_on', 'fieldworks:steam_generator', 'code', None),
        ('partsmith', 'fieldworks:copper_coil', 'inv', 'fieldworks:copper_coil'),
        ('first_machine', 'fieldworks:electrolyzer_1', 'inv', 'fieldworks:electrolyzer_1'),
        ('steelsmith', 'fieldworks:steel_ingot', 'inv', 'fieldworks:steel_ingot'),
        ('carbon_ledger', 'fieldworks:carbon_gauge', 'code', None),
        ('five_airs', 'fieldworks:fieldpack_terminal', 'code', None),
        ('siting_master', 'fieldworks:probe_staff', 'code', None),
        ('deep_breath', 'fieldworks:condenser', 'inv', 'fieldworks:condenser'),
        ('envelope', 'fieldworks:field_frame', 'inv', 'fieldworks:field_frame'),
        ('giant', 'fieldworks:giant_core', 'inv', 'fieldworks:giant_core'),
        ('superconduct', 'fieldworks:superconductor_ring', 'inv', 'fieldworks:superconductor_ring'),
    ]
    # root：木棍触发(开局一分钟内可得，vanilla story/root 同思路)
    wj('data/fieldworks/advancement/root.json', {
        'display': {'icon': {'id': 'fieldworks:probe_staff'},
                    'title': {'translate': 'adv.fieldworks.root'},
                    'description': {'translate': 'adv.fieldworks.root.desc'},
                    'background': 'minecraft:block/deepslate',
                    'frame': 'task', 'show_toast': False, 'announce_chat': False},
        'criteria': {'sticks': {'trigger': 'minecraft:inventory_changed',
                                'conditions': {'items': [{'items': 'minecraft:stick'}]}}}})
    n += 1
    for i, (key, icon, kind, item_id) in enumerate(advs):
        parent = 'fieldworks:root' if i < 4 else 'fieldworks:' + advs[i - 1][0]
        if kind == 'inv':
            criteria = {'has': {'trigger': 'minecraft:inventory_changed',
                                'conditions': {'items': [{'items': item_id}]}}}
        else:
            criteria = {'granted': {'trigger': 'minecraft:impossible'}}
        wj(f'data/fieldworks/advancement/{key}.json', {
            'display': {'icon': {'id': icon}, 'title': {'translate': f'adv.fieldworks.{key}'},
                        'description': {'translate': f'adv.fieldworks.{key}.desc'}, 'frame': 'task'},
            'parent': parent, 'criteria': criteria})
        n += 1
    return n


def gen_screens():
    wj('assets/fieldworks/screens/machine.json', {
        'texture': 'textures/guis/machine.png',
        'src_rect': [0, 0, 176, 166],
        'progress_x': 79, 'progress_y': 35, 'progress_w': 24, 'progress_h': 6,
        'energy_x': 8, 'energy_y': 21,
        'fit_x': -72, 'fit_y': 2,   # v3: Fit 徽章在主框左上角外
        'palette': {'ACCENT': 0xE8A33D, 'TEAL': 0x1E9E96, 'TEXT': 0x3F3F46, 'MUTED': 0x6E7B83, 'GOOD': 0x2FA84F, 'WARN': 0xD9A728, 'BAD': 0xC0392B}})
    return 1


if __name__ == '__main__':
    total = 0
    total += gen_blockstates_models()
    total += gen_recipes()
    total += gen_worldgen()
    total += gen_tags()
    total += gen_advancements()
    total += gen_screens()
    print(f'[gen_data] wrote {total} json files -> {os.path.abspath(BASE)}')
