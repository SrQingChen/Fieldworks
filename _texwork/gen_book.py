# -*- coding: utf-8 -*-
"""帕秋莉手册《场域学入门》生成器(§18，Botania 式进度揭示)。运行：python _texwork/gen_book.py"""
import json
import os

BASE = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
DATA = os.path.join(BASE, 'data', 'fieldworks', 'patchouli_books', 'manual')
ASSETS = os.path.join(BASE, 'assets', 'fieldworks', 'patchouli_books', 'manual')

FIELDS = 'fieldworks:'
MC = 'minecraft:'


def wj(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)


# ── book.json ──
wj(os.path.join(DATA, 'book.json'), {
    'name': 'book.fieldworks.title', 'landing_text': 'book.fieldworks.landing',
    'i18n': True, 'version': 1, 'subtitle': 'book.fieldworks.subtitle',
    'book_texture': 'patchouli:textures/gui/book_purple.png',
    'model': 'patchouli:book_purple', 'pause_game': True,
    'show_progress': False, 'use_resource_pack': True,
    # 261012: creative_tab 注入移除——书由 FWCreativeTabs 显式注册(避免双重出现)
})

# ── 分类 ──
CATS = [
    ('start', 'book.fieldworks.cat.start', 'book.fieldworks.cat.start.desc', FIELDS + 'probe_staff', 0),
    ('reading', 'book.fieldworks.cat.reading', 'book.fieldworks.cat.reading.desc', FIELDS + 'spectrum_module', 1),
    ('steam', 'book.fieldworks.cat.steam', 'book.fieldworks.cat.steam.desc', FIELDS + 'steam_generator', 2),
    ('air', 'book.fieldworks.cat.air', 'book.fieldworks.cat.air.desc', FIELDS + 'compressed_air', 3),
    ('nature', 'book.fieldworks.cat.nature', 'book.fieldworks.cat.nature.desc', FIELDS + 'magnetized_core', 4),
    ('envelope', 'book.fieldworks.cat.envelope', 'book.fieldworks.cat.envelope.desc', FIELDS + 'field_frame', 5),
    ('giants', 'book.fieldworks.cat.giants', 'book.fieldworks.cat.giants.desc', FIELDS + 'giant_core', 6),
    ('carbon', 'book.fieldworks.cat.carbon', 'book.fieldworks.cat.carbon.desc', FIELDS + 'carbon_gauge', 7),
    ('appendix', 'book.fieldworks.cat.appendix', 'book.fieldworks.cat.appendix.desc', FIELDS + 'fieldpack_terminal', 8),
]
for cid, name, desc, icon, sortnum in CATS:
    for lang in ('zh_cn', 'en_us'):
        wj(os.path.join(ASSETS, lang, 'categories', f'{cid}.json'),
           {'name': name, 'description': desc, 'icon': icon, 'sortnum': sortnum})


def entry(lang, eid, cat, name, icon, pages, sortnum=0, adv=None):
    d = {'name': name, 'category': FIELDS + cat, 'icon': icon, 'pages': pages, 'sortnum': sortnum}
    if adv:
        d['advancement'] = adv
    wj(os.path.join(ASSETS, lang, 'entries', cat, f'{eid}.json'), d)


def T(key):
    return {'type': 'patchouli:text', 'text': key}


def SPOT(item, key, title_key=None):
    d = {'type': 'patchouli:spotlight', 'item': item, 'text': key}
    if title_key:
        d['title'] = title_key
    return d


def MB(name):
    """巨物多方块渲染页(内联 dense pattern, 由 gen_giants_v3.py 产出)。"""
    p = os.path.join(os.path.dirname(__file__), 'giant_patchouli', name + '.json')
    with open(p, encoding='utf-8') as f:
        return json.load(f)


def LINK(url, key):
    return {'type': 'patchouli:link', 'url': url, 'text': key, 'link_text': 'patchouli.gui.lexicon.button.website'}


# ── 手册条目(每章 4—8 页，§18.1) ──
def gen(lang):
    L = lang
    # 序章：什么是场
    entry(L, 'intro', 'start', 'book.fieldworks.e.intro', FIELDS + 'probe_staff', [
        T('book.fieldworks.e.intro.p1'), T('book.fieldworks.e.intro.p2'),
        T('book.fieldworks.e.intro.p3'), T('book.fieldworks.e.intro.p4'),
    ], 0)
    entry(L, 'probe', 'start', 'book.fieldworks.e.probe', FIELDS + 'probe_staff', [
        {'type': 'patchouli:crafting', 'recipe': FIELDS + 'probe_staff', 'text': 'book.fieldworks.e.probe.p1'},
        T('book.fieldworks.e.probe.p2'),
    ], 1)
    entry(L, 'first_power', 'start', 'book.fieldworks.e.first_power', FIELDS + 'steam_generator', [
        {'type': 'patchouli:crafting', 'recipe': FIELDS + 'steam_generator', 'text': 'book.fieldworks.e.first_power.p1'},
        T('book.fieldworks.e.first_power.p2'),
    ], 2)
    entry(L, 'catcher', 'start', 'book.fieldworks.e.catcher', FIELDS + 'stone_catcher', [
        {'type': 'patchouli:crafting', 'recipe': FIELDS + 'stone_catcher', 'text': 'book.fieldworks.e.catcher.p1'},
        T('book.fieldworks.e.catcher.p2'),
    ], 3)
    entry(L, 'manual', 'start', 'book.fieldworks.e.manual', FIELDS + 'probe_staff', [
        {'type': 'patchouli:crafting', 'recipe': FIELDS + 'book_manual', 'text': 'book.fieldworks.e.manual.p1'},
        T('book.fieldworks.e.manual.p2'),
    ], 4)
    # 大地读谱
    entry(L, 'four_fields', 'reading', 'book.fieldworks.e.four_fields', FIELDS + 'fieldpack_terminal', [
        T('book.fieldworks.e.four_fields.p1'), T('book.fieldworks.e.four_fields.p2'),
    ], 0)
    entry(L, 'atmos', 'reading', 'book.fieldworks.e.atmos', FIELDS + 'compressed_air', [
        T('book.fieldworks.e.atmos.p1'), T('book.fieldworks.e.atmos.p2'),
        T('book.fieldworks.e.atmos.p3'),
    ], 1)
    entry(L, 'fit', 'reading', 'book.fieldworks.e.fit', FIELDS + 'blast_furnace', [
        T('book.fieldworks.e.fit.p1'), T('book.fieldworks.e.fit.p2'),
    ], 2, adv=FIELDS + 'first_machine')
    entry(L, 'deep', 'reading', 'book.fieldworks.e.deep', FIELDS + 'condenser', [
        T('book.fieldworks.e.deep.p1'),
    ], 3, adv=FIELDS + 'deep_breath')
    # 蒸汽与部件
    entry(L, 'bench', 'steam', 'book.fieldworks.e.bench', FIELDS + 'part_bench', [
        {'type': 'patchouli:crafting', 'recipe': FIELDS + 'part_bench', 'text': 'book.fieldworks.e.bench.p1'},
        T('book.fieldworks.e.bench.p2'),
    ], 0)
    entry(L, 'first_machines', 'steam', 'book.fieldworks.e.first_machines', FIELDS + 'electrolyzer_1', [
        T('book.fieldworks.e.first_machines.p1'),
        T('book.fieldworks.e.first_machines.p2'),
    ], 1, adv=FIELDS + 'power_on')
    entry(L, 'steel', 'steam', 'book.fieldworks.e.steel', FIELDS + 'steel_ingot', [
        SPOT(FIELDS + 'steel_ingot', 'book.fieldworks.e.steel.p1'),
        T('book.fieldworks.e.steel.p2'),
    ], 2, adv=FIELDS + 'partsmith')
    entry(L, 'ui_guide', 'steam', 'book.fieldworks.e.ui_guide', FIELDS + 'electrolyzer_2', [
        T('book.fieldworks.e.ui_guide.p1'),
        T('book.fieldworks.e.ui_guide.p2'),
        T('book.fieldworks.e.ui_guide.p3'),
    ], 3, adv=FIELDS + 'power_on')
    # 空气工业
    entry(L, 'electrolysis', 'air', 'book.fieldworks.e.electrolysis', FIELDS + 'electrolyzer_2', [
        T('book.fieldworks.e.electrolysis.p1'),
        T('book.fieldworks.e.electrolysis.p2'),
        T('book.fieldworks.e.electrolysis.p3'),
    ], 0)
    entry(L, 'distill', 'air', 'book.fieldworks.e.distill', FIELDS + 'distillation_tower', [
        T('book.fieldworks.e.distill.p1'),
    ], 1, adv=FIELDS + 'steelsmith')
    entry(L, 'pipes', 'air', 'book.fieldworks.e.pipes', FIELDS + 'fluid_pipe', [
        {'type': 'patchouli:crafting', 'recipe': FIELDS + 'fluid_pipe', 'text': 'book.fieldworks.e.pipes.p1'},
        T('book.fieldworks.e.pipes.p2'),
        {'type': 'patchouli:crafting', 'recipe': FIELDS + 'item_pipe', 'text': 'book.fieldworks.e.pipes.p3'},
        {'type': 'patchouli:crafting', 'recipe': FIELDS + 'energy_pipe', 'text': 'book.fieldworks.e.pipes.p4'},
    ], 2)
    entry(L, 'water_pump', 'steam', 'book.fieldworks.e.water_pump', FIELDS + 'infinite_water_pump', [
        T('book.fieldworks.e.water_pump.p1'),
        T('book.fieldworks.e.water_pump.p2'),
    ], 4, adv=FIELDS + 'power_on')
    entry(L, 'purity', 'air', 'book.fieldworks.e.purity', FIELDS + 'resonant_crystal_ultra', [
        T('book.fieldworks.e.purity.p1'),
        T('book.fieldworks.e.purity.p2'),
    ], 3, adv=FIELDS + 'steelsmith')
    # 261014 T10 场学饰品
    entry(L, 'accessories', 'reading', 'book.fieldworks.e.accessories', FIELDS + 'resonant_band', [
        T('book.fieldworks.e.accessories.p1'),
        T('book.fieldworks.e.accessories.p2'),
        SPOT(FIELDS + 'superconductor_core', 'book.fieldworks.e.accessories.p3'),
    ], 5, adv=FIELDS + 'envelope')
    # 自然的馈赠
    entry(L, 'magnetic', 'nature', 'book.fieldworks.e.magnetic', FIELDS + 'magnetometer', [
        T('book.fieldworks.e.magnetic.p1'),
        T('book.fieldworks.e.magnetic.p2'),
    ], 0)
    entry(L, 'tide', 'nature', 'book.fieldworks.e.tide', FIELDS + 'tidal_meter', [
        T('book.fieldworks.e.tide.p1'),
    ], 1)
    entry(L, 'vacuum', 'nature', 'book.fieldworks.e.vacuum', FIELDS + 'sky_platform', [
        T('book.fieldworks.e.vacuum.p1'),
    ], 2, adv=FIELDS + 'siting_master')
    entry(L, 'voidline', 'nature', 'book.fieldworks.e.voidline', FIELDS + 'resonant_drill', [
        T('book.fieldworks.e.voidline.p1'),
    ], 3, adv=FIELDS + 'five_airs')
    # 营造你的场
    entry(L, 'five_pieces', 'envelope', 'book.fieldworks.e.five_pieces', FIELDS + 'field_frame', [
        T('book.fieldworks.e.five_pieces.p1'),
        T('book.fieldworks.e.five_pieces.p2'),
        T('book.fieldworks.e.five_pieces.p3'),
    ], 0, adv=FIELDS + 'envelope')
    entry(L, 'coupling', 'envelope', 'book.fieldworks.e.coupling', FIELDS + 'field_coupler', [
        T('book.fieldworks.e.coupling.p1'),
        T('book.fieldworks.e.coupling.p2'),
    ], 1, adv=FIELDS + 'envelope')
    entry(L, 'harmonic', 'envelope', 'book.fieldworks.e.harmonic', FIELDS + 'harmonic_chamber', [
        T('book.fieldworks.e.harmonic.p1'),
        T('book.fieldworks.e.harmonic.p2'),
        T('book.fieldworks.e.harmonic.p3'),
    ], 2, adv=FIELDS + 'envelope')
    entry(L, 'tame', 'envelope', 'book.fieldworks.e.tame', FIELDS + 'tame_generator', [
        T('book.fieldworks.e.tame.p1'),
    ], 3, adv=FIELDS + 'envelope')
    # 巨物图鉴(261012: 每巨物带书中 3D 多方块渲染页 + 世界内投影按钮)
    entry(L, 'multiblock', 'giants', 'book.fieldworks.e.multiblock', FIELDS + 'giant_core', [
        T('book.fieldworks.e.multiblock.p1'),
        T('book.fieldworks.e.multiblock.p2'),
        T('book.fieldworks.e.multiblock.p3'),
    ], 0, adv=FIELDS + 'giant')
    entry(L, 'tower', 'giants', 'book.fieldworks.e.tower', FIELDS + 'resonance_tower', [
        T('book.fieldworks.e.tower.p1'),
        MB('resonance_tower'),
        T('book.fieldworks.e.tower.p2'),
    ], 1, adv=FIELDS + 'giant')
    entry(L, 'dome', 'giants', 'book.fieldworks.e.dome', FIELDS + 'eco_dome', [
        T('book.fieldworks.e.dome.p1'),
        MB('eco_dome'),
    ], 2, adv=FIELDS + 'giant')
    entry(L, 'anchor', 'giants', 'book.fieldworks.e.anchor', FIELDS + 'warp_anchor', [
        T('book.fieldworks.e.anchor.p1'),
        MB('warp_anchor'),
        T('book.fieldworks.e.anchor.p2'),
    ], 3, adv=FIELDS + 'giant')
    entry(L, 'deep_array', 'giants', 'book.fieldworks.e.deep_array', FIELDS + 'deep_electrolysis_array', [
        T('book.fieldworks.e.deep_array.p1'),
        MB('deep_electrolysis_array'),
    ], 4, adv=FIELDS + 'giant')
    entry(L, 'cathedral', 'giants', 'book.fieldworks.e.cathedral', FIELDS + 'harmonic_cathedral', [
        T('book.fieldworks.e.cathedral.p1'),
        MB('harmonic_cathedral'),
    ], 5, adv=FIELDS + 'envelope')
    entry(L, 'array', 'giants', 'book.fieldworks.e.array', FIELDS + 'planetary_array', [
        T('book.fieldworks.e.array.p1'),
        MB('planetary_array'),
        T('book.fieldworks.e.array.p2'),
    ], 6, adv=FIELDS + 'superconduct')
    # 环境与责任
    entry(L, 'carbon', 'carbon', 'book.fieldworks.e.carbon', FIELDS + 'carbon_gauge', [
        T('book.fieldworks.e.carbon.p1'),
        T('book.fieldworks.e.carbon.p2'),
    ], 0)
    entry(L, 'capture', 'carbon', 'book.fieldworks.e.capture', FIELDS + 'carbon_capturer', [
        T('book.fieldworks.e.capture.p1'),
    ], 1, adv=FIELDS + 'carbon_ledger')
    entry(L, 'corrosion', 'carbon', 'book.fieldworks.e.corrosion', FIELDS + 'corrosion_trap', [
        T('book.fieldworks.e.corrosion.p1'),
    ], 2, adv=FIELDS + 'carbon_ledger')
    # 附录
    entry(L, 'fields_table', 'appendix', 'book.fieldworks.e.fields_table', FIELDS + 'fieldpack_terminal', [
        T('book.fieldworks.e.fields_table.p1'),
    ], 0)
    entry(L, 'api', 'appendix', 'book.fieldworks.e.api', FIELDS + 'giant_blueprint', [
        T('book.fieldworks.e.api.p1'),
    ], 1)
    entry(L, 'furnace', 'appendix', 'book.fieldworks.e.furnace', MC + 'flint_and_steel', [
        T('book.fieldworks.e.furnace.p1'),
        T('book.fieldworks.e.furnace.p2'),
    ], 2)


for lang in ('zh_cn', 'en_us'):
    gen(lang)

# 手册文本(两语言)
ZH_ADD = {'book.fieldworks.e.multiblock.p3': '搭建三通道：①$(thing)蓝图右键地面$()放全息投影，Shift+右键投影自动消耗背包材料搭建；②对着书里的结构图点$(thing)可视化按钮$()，在世界内放半透明投影——右键方块锚定(锚点=控制核心落位)，Shift+右键旋转朝向，超过 64 格自动收起；③手搭也行——控制器会实时显示完成度，缺失件每 2 秒冒红色尘粒指引。四个水平朝向任一成形即可。', 'book.fieldworks.e.manual': '这本书从哪来', 'book.fieldworks.e.manual.p1': '木板环、书居中、纸垫底——三种方式获得本手册：合成、首次合成探测杖附赠、或整合包开局直发。', 'book.fieldworks.e.manual.p2': '注意区分：$(thing)《律动之书》$()是谐律附魔"等级突破"的$(italic)材料$()(在第 9 律以上使用)，不是教程书——教程书就是你手里的这本《场域学入门》。', 'book.fieldworks.e.ui_guide': '机器界面导读', 'book.fieldworks.e.ui_guide.p1': '打开任意机器：浅色原版风界面，主框左上角$(italic)外侧$()的青色小徽章是$(thing)环境适配仪$()——实时显示 Fit% 与当前温度/湿度/光照，随环境每秒跳动；悬停看归因(Shift/Ctrl 分层)。', 'book.fieldworks.e.ui_guide.p2': '槽位角标：青色三角=输入、绿色=输出、琥珀闪电=燃料、紫色=升级芯(升级槽在主框右侧外挂)。左缘页签打开六面 IO 配置(立方体展开图，左键循环/右键反循环/Shift+左键重置)。', 'book.fieldworks.e.ui_guide.p3': '$(thing)手持水桶右键$()带罐机器可直接注入/舀出(罐满则打开界面)；JEI 装有"场域机器"类目，全部配方与场需求可直接查询。', 'book.fieldworks.e.deep_array': '深井电解阵列', 'book.fieldworks.e.deep_array.p1': '材料线旗舰：井架环绕中央玻璃井，深层选址(基岩层磁场)时同群系全部电解器效率 +25%。井底中心放置控制器。', 'book.fieldworks.e.cathedral': '谐律大教堂', 'book.fieldworks.e.cathedral.p1': '附魔线旗舰：中厅 + 双塔的教堂形巨物，包络半径 48 内的附魔台全部享受谐律加成(§谐律场)。入口地面中心放置控制器。', 'book.fieldworks.e.tower.mb': '谐振聚能塔 · 结构图', 'book.fieldworks.e.tower.mb_t': '9×11×9，控制器在塔基中心。转动视角查看各层；点"可视化"在世界里放投影。', 'book.fieldworks.e.dome.mb': '生态穹顶 · 结构图', 'book.fieldworks.e.dome.mb_t': '11×7×11 穹顶，控制器在地面中心。穹顶内是 32³ 全参数气候车间。', 'book.fieldworks.e.anchor.mb': '曲率锚 · 结构图', 'book.fieldworks.e.anchor.mb_t': '9×8×9，控制器在环心。Amethyst 环是曲率稳定器。', 'book.fieldworks.e.deep_array.mb': '深井电解阵列 · 结构图', 'book.fieldworks.e.deep_array.mb_t': '9×9×9 井架，控制器在井底中心。', 'book.fieldworks.e.cathedral.mb': '谐律大教堂 · 结构图', 'book.fieldworks.e.cathedral.mb_t': '11×10×11，控制器在中厅地面中心。正立面有玫瑰窗与门洞。', 'book.fieldworks.e.array.mb': '行星调谐阵列 · 结构图', 'book.fieldworks.e.array.mb_t': '11×5×11，六调谐柱环绕中央谐振井，控制器在井心。'}
EN_ADD = {'book.fieldworks.e.multiblock.p3': 'Three ways to build: 1) $(thing)right-click a blueprint$() for a hologram, Shift+right-click it to auto-build from your inventory; 2) press $(thing)Visualize$() on the structure page for an in-world ghost — right-click a block to anchor it (the anchor is where the control core goes), Shift+right-click to rotate, auto-dismiss beyond 64 blocks; 3) build by hand — the controller shows live completion and puffs red dust at missing pieces every 2 s. Any of the four horizontal orientations works.', 'book.fieldworks.e.manual': 'Where This Book Comes From', 'book.fieldworks.e.manual.p1': 'Planks around a book on paper. Three ways: craft it, get it free with your first Probe Staff, or pack-mode auto-issue.', 'book.fieldworks.e.manual.p2': 'Note: the $(thing)Rhythmic Book$() is a $(italic)material$() for harmonic enchantment level breakthrough (used above 9 lü) — not this guide. This guide is the Introduction to Field Studies.', 'book.fieldworks.e.ui_guide': 'Reading Machine GUIs', 'book.fieldworks.e.ui_guide.p1': 'Open any machine: light vanilla-style panel. The teal badge $(italic)outside$() the top-left corner is the $(thing)Field Fit gauge$() — live Fit% plus temperature/humidity/light, ticking with the environment. Hover for attribution (Shift/Ctrl layers).', 'book.fieldworks.e.ui_guide.p2': 'Slot marks: teal arrow = input, green = output, amber bolt = fuel, purple = upgrade core (upgrade slots dock outside the right edge). The left tab opens the six-face IO configurator (cube unfold; left-click cycles, right-click reverses, Shift+left-click resets).', 'book.fieldworks.e.ui_guide.p3': '$(thing)Right-click with a bucket$() on tanked machines to fill or scoop directly (opens the GUI if the tank is full). With JEI installed, the "Fieldworks Machines" category lists every recipe and its field demands.', 'book.fieldworks.e.deep_array': 'Deep Electrolysis Array', 'book.fieldworks.e.deep_array.p1': 'Materials flagship: a derrick around a central glass well. Sited on bedrock magnetism, grants +25% to every electrolyzer in the biome. Controller at the well bottom.', 'book.fieldworks.e.cathedral': 'Harmonic Cathedral', 'book.fieldworks.e.cathedral.p1': 'Enchanting flagship: nave and twin towers, radius-48 envelope blessing every enchanting table (see Harmonic). Controller at the nave floor center.', 'book.fieldworks.e.tower.mb': 'Resonance Tower · Structure', 'book.fieldworks.e.tower.mb_t': '9×11×9; controller at the base center. Rotate to inspect layers; Visualize for an in-world ghost.', 'book.fieldworks.e.dome.mb': 'Eco Dome · Structure', 'book.fieldworks.e.dome.mb_t': '11×7×11 dome; controller at ground center. Inside is a 32³ climate workshop.', 'book.fieldworks.e.anchor.mb': 'Warp Anchor · Structure', 'book.fieldworks.e.anchor.mb_t': '9×8×9; controller at the ring heart. Amethyst rings stabilize the curvature.', 'book.fieldworks.e.deep_array.mb': 'Deep Array · Structure', 'book.fieldworks.e.deep_array.mb_t': '9×9×9 derrick; controller at the well bottom.', 'book.fieldworks.e.cathedral.mb': 'Cathedral · Structure', 'book.fieldworks.e.cathedral.mb_t': '11×10×11; controller at the nave floor center. Rose window and portal on the facade.', 'book.fieldworks.e.array.mb': 'Planetary Array · Structure', 'book.fieldworks.e.array.mb_t': '11×5×11; six tuning pillars ring the central well; controller at the well heart.'}
ZHT = {
    'book.fieldworks.title': '场域学入门',
    'book.fieldworks.subtitle': '世界本身就是一台机器',
    'book.fieldworks.landing': '欢迎来到场学的世界。$(thing)场$()无处不在——你脚下的空气、光照、温度与湿度，都是可以被读懂、被利用、被改造的参数。这本书会带你从第一根探测杖，走到调度整颗星球。',
    'book.fieldworks.cat.start': '序章：什么是场',
    'book.fieldworks.cat.start.desc': '四个页面的"漫画式"入门：探测杖读数解读。',
    'book.fieldworks.cat.reading': '大地读谱',
    'book.fieldworks.cat.reading.desc': '四大场、群系大气速查。',
    'book.fieldworks.cat.steam': '蒸汽与部件',
    'book.fieldworks.cat.steam.desc': 'P0 全流程图文。',
    'book.fieldworks.cat.air': '空气工业',
    'book.fieldworks.cat.air.desc': '捕获/压缩/分馏/储运。',
    'book.fieldworks.cat.nature': '自然的馈赠',
    'book.fieldworks.cat.nature.desc': '磁场/引力/真空/月相潮汐。',
    'book.fieldworks.cat.envelope': '营造你的场',
    'book.fieldworks.cat.envelope.desc': '五件套教学与强度表。',
    'book.fieldworks.cat.giants': '巨物图鉴',
    'book.fieldworks.cat.giants.desc': '五大巨物与行星阵列剖面。',
    'book.fieldworks.cat.carbon': '环境与责任',
    'book.fieldworks.cat.carbon.desc': '碳压系统。',
    'book.fieldworks.cat.appendix': '附录',
    'book.fieldworks.cat.appendix.desc': '场表/单位表/API 指引。',
    'book.fieldworks.e.intro': '什么是场',
    'book.fieldworks.e.intro.p1': '把世界想象成一张布满参数的图纸：每片森林的$(thing)氧气$()浓度不同，每处深海的$(thing)盐蚀$()各异，月光牵动$(thing)引力$()，深处涌着$(thing)磁场$()。这些参数，就是"场"。',
    'book.fieldworks.e.intro.p2': '场学的第一条法则：$(italic)站在哪里，第一次变得重要。$()同一台机器，建在森林、沙漠或深谷，效率截然不同。',
    'book.fieldworks.e.intro.p3': '第二条法则：$(italic)条件是放大器，不是通行证。$()平原上一切基础配方都能以至少 60% 的效率运转——选址好，只是更快、更省。',
    'book.fieldworks.e.intro.p4': '第三条法则：$(italic)任何差异都可诊断。$()探测杖、光谱仪、机器 GUI 会告诉你"为什么是这个速度"，永远没有谜语。',
    'book.fieldworks.e.probe': '探测杖',
    'book.fieldworks.e.probe.p1': '中间与左下放木棍，右上放任意金属粒。三格材料，随手可制。',
    'book.fieldworks.e.probe.p2': '$(thing)潜行右键空气$()：读出气氛类型、温度、湿度、光照与压强。耐久 16 次；装上光谱仪部件后免耐久并显示特征值六元组。',
    'book.fieldworks.e.first_power': '第一度电',
    'book.fieldworks.e.first_power.p1': '铁锭×3 上排，水桶/熔炉/水桶中排，圆石×3 下排。发电机$(italic)内置 2 桶水箱$()，放煤即出电，无需外接水路。',
    'book.fieldworks.e.first_power.p2': '16J/t 起步——原版熔炉级的成本，8 分钟来电。',
    'book.fieldworks.e.catcher': '石制捕获器',
    'book.fieldworks.e.catcher.p1': '圆石与玻璃围一只木桶。$()',
    'book.fieldworks.e.catcher.p2': '$(thing)空手右键充能$()：每击 +34%，三次充满即弹出压缩空气——你第一次把"空气"变成了物品。',
    'book.fieldworks.e.four_fields': '四大场',
    'book.fieldworks.e.four_fields.p1': '$(thing)气氛场$()决定你能捕到什么空气；$(thing)光照场$()直接读原版光照等级；$(thing)温度场$()影响一切热学配方；$(thing)湿度场$()影响水系与发酵。',
    'book.fieldworks.e.four_fields.p2': '它们随群系、高度、深度、昼夜与天气变化——机器 GUI 的"Fit%"实时显示当前环境适配率。',
    'book.fieldworks.e.atmos': '群系大气速查',
    'book.fieldworks.e.atmos.p1': '森林$(bold)大概率$()富氧；沼泽$(bold)必然$()富沼；沙漠干热；冰原寒燥(冰原其实很干！)；海洋盐雾；蘑菇岛菌孢；深板岩层渐入幽邃；下界分荒地硫化/灵魂沙灰烬/玄武岩玄温/绯诡菌林；末地是调谐虚空气。',
    'book.fieldworks.e.atmos.p2': '同一群系的不同区域还有 ±10% 的实例扰动——"这一片森林"氧势 26，"那一片"可能 33。探索永远有惊喜。',
    'book.fieldworks.e.atmos.p3': '未知的(模组)群系会在首次进入时做"标签普查"自动归族——任何世界生成模组零配置兼容。',
    'book.fieldworks.e.fit': '环境适配率',
    'book.fieldworks.e.fit.p1': '单项适配 f = max(0, 1 − |当前值 − 理想值| / 容差 × 惩罚)；总适配率 Fit = 各项按权重连乘，下限 60%。',
    'book.fieldworks.e.fit.p2': '悬停机器 GUI 的 Fit 条：Shift 看逐场归因，Ctrl 看完整诊断。效率永远可解释。',
    'book.fieldworks.e.deep': '深层与通风',
    'book.fieldworks.e.deep.p1': 'y<0 起大气向幽邃插值——但$(bold)幽邃只作用于机器$()，绝不作用于玩家：挖矿体验与原版完全一致。通风管把地表空气导下来，深层配方回到满效。',
    'book.fieldworks.e.bench': '简易部件台',
    'book.fieldworks.e.bench.p1': '电力合成台：铜线圈、活塞框架、传感芯……每件耗电不超过 200J。',
    'book.fieldworks.e.bench.p2': '部件是机器的骨骼——初始机器全部由"部件 + 原版材料"组成。',
    'book.fieldworks.e.first_machines': '第一台机器',
    'book.fieldworks.e.first_machines.p1': '$(thing)电解器 Ⅰ$()：电解压缩空气，得氧气与氮气。$(thing)鼓风熔炉$()：炼钢的核心——在富氧森林厂效率显著更高。',
    'book.fieldworks.e.first_machines.p2': '试试把鼓风熔炉搬到不同的群系，看 Fit 条的读数——这就是你的第一堂选址课。',
    'book.fieldworks.e.steel': '钢',
    'book.fieldworks.e.steel.p1': '富氧鼓风还原：铁锭 + 压缩空气。这是现实中的氧气转炉炼钢(BOF)，自 1950 年代起就是工业标准。',
    'book.fieldworks.e.steel.p2': '沙漠厂可以走$(thing)氢气竖炉$()(多一道电解工序，现实的"绿钢"路线)；末地厂后期走"调谐还原"。',
    'book.fieldworks.e.electrolysis': '电解分级',
    'book.fieldworks.e.electrolysis.p1': '电解器 Ⅰ：压缩空气→氧+氮。Ⅱ 级新增电解水→氢+氧，并支持升级芯。Ⅲ 级$(bold)磁约束电解$()需基岩层选址(自然磁场≥0.3T)，产物纯度+1 档。',
    'book.fieldworks.e.electrolysis.p2': 'Ⅳ 级深井电解阵列是巨物：同群系电解器效率 +25%。Ⅴ 级等离子矩阵可电解调谐虚空气，带废热发电反哺。',
    'book.fieldworks.e.electrolysis.p3': '每一级都不只是更快——它带来$(italic)新机制$()。',
    'book.fieldworks.e.distill': '分馏塔',
    'book.fieldworks.e.distill.p1': '把压缩空气分成氧/氮/惰气三路。配合储气罐与管道，你的第一座气体工厂成型了。',
    'book.fieldworks.e.pipes': '管道',
    'book.fieldworks.e.pipes.p1': '流体管道(蓝)：气体也是流体——一条管道通吃全部气体与液体，相连即工作。',
    'book.fieldworks.e.pipes.p2': '管道里流什么一眼可见——运载的流体会以自身颜色点亮管芯，越满越亮。充压(高纯气体≥90)与通电强化可将传输速率提升至 ×3。',
    'book.fieldworks.e.pipes.p3': '物品管道(琥珀)：自动搬运机器产物——从输出面取料、送入输入面，管间水位平衡自流。',
    'book.fieldworks.e.pipes.p4': '能量管道(青)：发电机输出面取电、耗电机器输入面送电，芯体脉冲可见。',
    'book.fieldworks.e.water_pump': '无限水泵',
    'book.fieldworks.e.water_pump.p1': '浸入式取水：六面中至少两面贴着$(bold)水源方块$()才工作。仅耗电产水——向内置罐与相邻机器供水，管道自动取走。',
    'book.fieldworks.e.water_pump.p2': '三档位：低速 1桶/秒、高速 3桶/秒、$(bold)无限档$()按管道实际吞吐供水，耗电随流速指数上升。GUI 左侧页签切换档位。',
    'book.fieldworks.e.accessories': '场学饰品',
    'book.fieldworks.e.accessories.p1': '人工强场的产物可以随身携带：谐振指环(急迫)、磁准透镜(磁吸)、场线护符(轻身)、通量腰带(幸运)、潮汐坠饰(水中呼吸)、虚空步径戒(闪现)。',
    'book.fieldworks.e.accessories.p2': '饰品装进 Curios 栏位即生效(未安装 Curios 则仅可收藏)。在装配机用场学材料合成。',
    'book.fieldworks.e.accessories.p3': '终极饰品$(bold)超导谐律核心$()：内置 20 万 FE 电容；受击伤害按 400FE/HP 转电抵挡——完全抵挡时连本次附带的状态效果一并免疫；每实际耗电 2000FE 叠一层随机属性增幅，层数可叠加、每层独立计时 30 秒。谐振燃料右键充能，强磁场中被动涓流充电。',
    'book.fieldworks.e.purity': '纯度体系',
    'book.fieldworks.e.purity.p1': '气体携带 0—100 纯度：粗炼 <60 / 精炼 60—90 / 超纯 ≥90。机器档位决定纯度上限：Ⅲ 级磁约束 +10，巨物集群 +10，Ⅴ 级可达 100。',
    'book.fieldworks.e.purity.p2': '纯度三重价值：配方质量加档、管道充压 ×2、人工场介质损耗减半。',
    'book.fieldworks.e.magnetic': '基岩层磁场',
    'book.fieldworks.e.magnetic.p1': 'y=-32 以下自然磁场从 0.1T 线性升至 0.5T——磁化机、磁选机、Ⅲ 级电解器都偏爱深处的选址。',
    'book.fieldworks.e.magnetic.p2': '雷暴天的自然电磁尖峰是免费磁化窗口；指南针在强场内会疯转(风味)。',
    'book.fieldworks.e.tide': '月相与潮汐',
    'book.fieldworks.e.tide.p1': '满月引力 +15%、朔月 −10%，海洋群系叠加潮汐系数。"满月之夜分选效率 +20%"——用潮汐日历排班你的月夜工程。',
    'book.fieldworks.e.vacuum': '高度即真空',
    'book.fieldworks.e.vacuum.p1': '天穹层气压随高度每 100 格减半：高山与浮岛是天然真空场入口。高空平台是你的踏板。',
    'book.fieldworks.e.voidline': '虚空界面',
    'book.fieldworks.e.voidline.p1': '谐振钻头贯穿基岩(约 5 分钟 + 巨量能量)，开启基岩下的 1—8 格薄层——虚空气，终局材料的粗料。往下仍是虚空：这不是新维度。',
    'book.fieldworks.e.five_pieces': '五件套',
    'book.fieldworks.e.five_pieces.p1': '$(thing)框架$()定义包络(3³—32³)，$(thing)输入器$()决定场型(放什么介质成什么场)，$(thing)耦合器$()连接输入器实现增强/多场叠加，$(thing)约束器$()杜绝外溢，$(thing)稳定器$()提升上限 20%。',
    'book.fieldworks.e.five_pieces.p2': '搭积木式营造：放框架→中央放输入器→放介质→通电。没有拼图解谜，只有自由组合。',
    'book.fieldworks.e.five_pieces.p3': '耦合容量 = 1 + 框架边长/8。32³ 的大框架能挂 5 个输入器——框架给空间，输入器给强度。',
    'book.fieldworks.e.coupling': '耦合与场编程',
    'book.fieldworks.e.coupling.p1': '同场型耦合：电磁 ×1.6/个(上限 100T)；异场型并置：一个框架内多场共存——电磁 20T + 真空 10⁻³Pa + −40℃ 的超导退火三条件就是这样做出来的。',
    'book.fieldworks.e.coupling.p2': '场编程不是终局技术：从 P1 的"温×湿×光×大气组合选址"就开始了。',
    'book.fieldworks.e.harmonic': '谐律场与附魔',
    'book.fieldworks.e.harmonic.p1': '调谐叉决定风格：$(bold)祸律$()偏置伤害系(锋利/亡灵杀手/击退……)，$(bold)禄律$()偏置收获系(时运/掠夺/效率……)，$(bold)庇律$()偏置守护系(保护/耐久/经验修补……)——跨物品类别的主题体验。',
    'book.fieldworks.e.harmonic.p2': '≥6 律附魔台等效多摆 15 本书架；≥8 律极稀有条目(经验修补/冰霜行者)×3；≥9 律解锁$(bold)等级突破：锋利/保护/效率可达 VI$()(经律动之书)。',
    'book.fieldworks.e.harmonic.p3': '不同风格的谐律腔不叠加(区域内最强者生效)。Apotheosis 在场时自动让位。',
    'book.fieldworks.e.tame': '驯息场',
    'book.fieldworks.e.tame.p1': '≥20 息消除愤怒；≥40 息幼体成长翻倍、繁殖冷却 −30%；≥60 息稀有掉落 ×1.5；≥80 息驯服生物获得生命恢复——宠物医院与养殖场的核心。',
    'book.fieldworks.e.multiblock': '巨物通则',
    'book.fieldworks.e.multiblock.p1': '强化桁架(骨架，可染色)+钢外壳板(蒙皮)+观测玻璃(透明工作窗)三件套构成 9³ 起的巨物——成形即自带温和区块加载，无需通电。',
    'book.fieldworks.e.multiblock.p2': '错误位置会以粒子标注缺失件，诊断到方块。巨物本身就是仪表：功率=光环亮度，失谐=红闪。',
    'book.fieldworks.e.tower': '谐振聚能塔',
    'book.fieldworks.e.tower.p1': '能源线旗舰：燃烧谐振燃料(三因子——氧纯度 × 燃料气种类 × 催化组合决定效率倍率)，输出随塔体高度倍增。',
    'book.fieldworks.e.tower.p2': '$(bold)失效三阶段$()：过载 10 秒→黄光告警 60 秒(零损失)→强制停机(损失在炉燃料 50%)。三阶爆炸$(italic)仅由"停机中仍持续注燃料"的故意操作触发$()，且带 30 秒全服预警与 8 秒可见扩散的冲击波——120 秒警告，绝无意外。',
    'book.fieldworks.e.dome': '生态穹顶',
    'book.fieldworks.e.dome.p1': '32³ 内四大场全参数设定——在沙漠复刻雨林车间，雪傀儡在沙漠穹顶内存活，"气候配方"就地满效。能量中断只平滑回落，永不破坏。',
    'book.fieldworks.e.anchor': '曲率锚',
    'book.fieldworks.e.anchor.p1': '区域整体迁移(16³/32³/48³)：预检→冻结→序列化到 $(thing).fwr$() 安全文件(SHA-256 校验、原子写入)→曲率残影→原子交换。失败自动回滚，永不毁档。',
    'book.fieldworks.e.anchor.p2': '传送门框架/末影箱/区块加载器在黑名单——迁移前粒子告知，自行处理。',
    'book.fieldworks.e.array': '行星调谐阵列',
    'book.fieldworks.e.array.p1': '终局建筑：环形阵列 + 16 调谐尖塔 + 中央谐振井。全服可见极光、月圆光柱、全服附魔偏置广播、全服药水广播(种类×等级×人数计费)。',
    'book.fieldworks.e.array.p2': '全局干预有反作用：调得越快，能量成本与反弹应力指数上升。这是"运营者"的建筑——你给这颗星球写参数。',
    'book.fieldworks.e.carbon': '碳账本',
    'book.fieldworks.e.carbon.p1': '全存档唯一的碳压 cp(0—1000，自然平衡点 200)：所有模组的燃烧/耗电行为共同抬升，自然消减随偏离加速。高碳给全服一个轻微的温度倍率。',
    'book.fieldworks.e.carbon.p2': '账本永远不催债：高碳的代价是"错过一些效率"，不是灾害。它也是唯一的全局共享进度。',
    'book.fieldworks.e.capture': '碳捕获器',
    'book.fieldworks.e.capture.p1': '耗电捕碳，产出碳黑与干冰；cp 越高捕获效率越高——鼓励"调"而非"清零"。',
    'book.fieldworks.e.corrosion': '蚀捕器',
    'book.fieldworks.e.corrosion.p1': '"污染也是矿"：捕获高蚀势大气产出硫磺与蚀盐，同时净化所在群系——治理与收益同机。',
    'book.fieldworks.e.fields_table': '场与单位速查',
    'book.fieldworks.e.fields_table.p1': '气氛场(特征值向量)/光照(lv 0—15)/温度(℃)/湿度(%RH)/磁场(T)/引力(g)/真空(Pa，对数刻)/辐射(μSv/h)/碳压(cp)/电磁·引力·真空·辐射(人工，量级远超自然)/谐律(律 0—12)/驯息(息 0—100)/曲率(0—9)。',
    'book.fieldworks.e.api': 'API 指引',
    'book.fieldworks.e.api.p1': '场、大气、配方条件、机器行为全部注册表化 + 数据包化：第三方经 $(thing)FieldRegistry.register()$() 注册新场，机器与配方零改动生效；数据包可覆盖群系映射/配方/场强表/UI 样式/本书。开源 MPL-2.0，欢迎附属。',
    'book.fieldworks.e.furnace': '常燃熔炉',
    'book.fieldworks.e.furnace.p1': '$(thing)手持打火石右键原版熔炉$()：进入持续燃烧——燃料烧完自动续燃，空转保温；放入原料自动交还原版。',
    'book.fieldworks.e.furnace.p2': '常燃熔炉向周围 4 格提供 +2℃ 余温(最多 4 台叠加到 +5℃)——P0 可负担的温度场微调手段。',
    **ZH_ADD,
}
ENT = {
    'book.fieldworks.title': 'Introduction to Field Studies',
    'book.fieldworks.subtitle': 'The world itself is a machine',
    'book.fieldworks.landing': 'Welcome to field studies. $(thing)Fields$() are everywhere — the air, light, temperature and humidity beneath your feet are all parameters waiting to be read, used, and reshaped.',
    'book.fieldworks.cat.start': 'Prologue: What Is a Field',
    'book.fieldworks.cat.start.desc': 'A four-page comic-style start.',
    'book.fieldworks.cat.reading': 'Reading the Land',
    'book.fieldworks.cat.reading.desc': 'The Four and biome atmospheres.',
    'book.fieldworks.cat.steam': 'Steam & Parts',
    'book.fieldworks.cat.steam.desc': 'The P0 flow, illustrated.',
    'book.fieldworks.cat.air': 'Air Industry',
    'book.fieldworks.cat.air.desc': 'Catch, compress, distill, store.',
    'book.fieldworks.cat.nature': 'Gifts of Nature',
    'book.fieldworks.cat.nature.desc': 'Magnetism, gravity, vacuum, tides.',
    'book.fieldworks.cat.envelope': 'Make Your Field',
    'book.fieldworks.cat.envelope.desc': 'The five-piece set.',
    'book.fieldworks.cat.giants': 'Bestiary of Giants',
    'book.fieldworks.cat.giants.desc': 'Five giants and the array.',
    'book.fieldworks.cat.carbon': 'Environment & Duty',
    'book.fieldworks.cat.carbon.desc': 'The carbon ledger.',
    'book.fieldworks.cat.appendix': 'Appendix',
    'book.fieldworks.cat.appendix.desc': 'Tables and the API.',
    'book.fieldworks.e.intro': 'What Is a Field',
    'book.fieldworks.e.intro.p1': 'Picture the world as a sheet of parameters: every forest differs in $(thing)oxygen$(), every sea in $(thing)salt corrosion$(); the moon tugs $(thing)gravity$(), the deep hums with $(thing)magnetism$(). These are Fields.',
    'book.fieldworks.e.intro.p2': 'First law: $(italic)where you stand finally matters.$() The same machine runs differently in forest, desert, or deeps.',
    'book.fieldworks.e.intro.p3': 'Second law: $(italic)conditions are amplifiers, not passes.$() Every basic recipe runs at >= 60% on the plains.',
    'book.fieldworks.e.intro.p4': 'Third law: $(italic)every difference is diagnosable.$() The probe, spectrometer, and machine GUIs always explain "why this speed".',
    'book.fieldworks.e.probe': 'Probe Staff',
    'book.fieldworks.e.probe.p1': 'Sticks center and lower-left, any metal nugget upper-right.',
    'book.fieldworks.e.probe.p2': '$(thing)Sneak-use$() to read atmosphere, temperature, humidity, light and pressure. 16 uses; with a Spectrum Module installed: unlimited + full trait vector.',
    'book.fieldworks.e.first_power': 'First Power',
    'book.fieldworks.e.first_power.p1': 'Iron top row, water buckets and a furnace mid, cobble bottom. Built-in water tank — no plumbing needed.',
    'book.fieldworks.e.first_power.p2': '16 J/t from the start. Power in eight minutes.',
    'book.fieldworks.e.catcher': 'Stone Catcher',
    'book.fieldworks.e.catcher.p1': 'Cobble, glass, and a barrel.',
    'book.fieldworks.e.catcher.p2': '$(thing)Punch with an empty hand$(): +34% per hit; full at three — out pops Compressed Air. Your first bottled sky.',
    'book.fieldworks.e.four_fields': 'The Four',
    'book.fieldworks.e.four_fields.p1': '$(thing)Atmosphere$() decides what air you catch; $(thing)Light$() reads vanilla light; $(thing)Temperature$() tints all thermal recipes; $(thing)Humidity$() steers watercraft and fermentation.',
    'book.fieldworks.e.four_fields.p2': 'They shift with biome, height, depth, day and weather — the Fit% bar tracks it live.',
    'book.fieldworks.e.atmos': 'Biome Atmospheres',
    'book.fieldworks.e.atmos.p1': 'Forests are $(bold)likely$() oxygen-rich; swamps $(bold)always$() marsh; deserts arid; ice fields frigid (and dry!); seas salt-sprayed; the deep fades to Deep Air; the Nether splits by wastes/soul/basalt/forests; the End hums tuned void.',
    'book.fieldworks.e.atmos.p2': 'Instances of the same biome vary ±10% — one forest breathes 26 oxygen, another 33.',
    'book.fieldworks.e.atmos.p3': 'Unknown (modded) biomes are classified by automatic tag census — zero-config compat.',
    'book.fieldworks.e.fit': 'Field Fit',
    'book.fieldworks.e.fit.p1': 'Per-field fit f = max(0, 1 − |v − ideal| / tolerance × penalty); overall Fit multiplies by weight, floored at 60%.',
    'book.fieldworks.e.fit.p2': 'Hover the Fit bar: Shift for per-field breakdown, Ctrl for full diagnostics.',
    'book.fieldworks.e.deep': 'The Deep & Venting',
    'book.fieldworks.e.deep.p1': 'Below y=0 air interpolates toward Deep Air — but it affects $(bold)machines only$(), never players. Vent pipes bring surface air down to restore full speed.',
    'book.fieldworks.e.bench': 'Part Bench',
    'book.fieldworks.e.bench.p1': 'An electric crafting bench: coils, frames, sensor cores — each under 200 J.',
    'book.fieldworks.e.bench.p2': 'Parts are the skeleton of every machine.',
    'book.fieldworks.e.first_machines': 'First Machines',
    'book.fieldworks.e.first_machines.p1': '$(thing)Electrolyzer I$(): compressed air into oxygen and nitrogen. $(thing)Blast Furnace$(): steelmaking — noticeably faster in oxygen-rich forests.',
    'book.fieldworks.e.first_machines.p2': 'Move the blast furnace between biomes and watch the Fit bar. That is your first siting lesson.',
    'book.fieldworks.e.steel': 'Steel',
    'book.fieldworks.e.steel.p1': 'Oxygen blast reduction: iron + compressed air — the real-world BOF process.',
    'book.fieldworks.e.steel.p2': 'Desert plants may take the $(thing)hydrogen shaft furnace$() route (real-world green steel); End plants later take tuned reduction.',
    'book.fieldworks.e.electrolysis': 'Electrolyzer Tiers',
    'book.fieldworks.e.electrolysis.p1': 'I: air into O2/N2. II: adds water electrolysis and upgrade cores. III: $(bold)magnetically confined$() — siting on bedrock-layer magnetism, +1 purity tier.',
    'book.fieldworks.e.electrolysis.p2': 'IV is a giant array granting +25% to nearby electrolyzers; V is the plasma matrix with waste-heat feedback.',
    'book.fieldworks.e.electrolysis.p3': 'Every tier brings a $(italic)new mechanism$(), not just numbers.',
    'book.fieldworks.e.distill': 'Distillation',
    'book.fieldworks.e.distill.p1': 'Split compressed air into O2/N2/inert streams. With tanks and pipes, your first gas plant takes shape.',
    'book.fieldworks.e.pipes': 'Pipes',
    'book.fieldworks.e.pipes.p1': 'Gas and fluid pipes that just work when connected — flow is derived automatically.',
    'book.fieldworks.e.pipes.p2': 'See what flows at a glance — carried fluid lights the pipe core in its own color, brighter as it fills. Pressurize (purity >= 90) and electrify to reach x3 throughput.',
    'book.fieldworks.e.pipes.p3': 'Item Pipe (amber): moves machine outputs — pulls from output faces, feeds input faces, self-balances between pipes.',
    'book.fieldworks.e.pipes.p4': 'Energy Pipe (teal): draws from generator output faces, feeds machine input faces; the core visibly pulses.',
    'book.fieldworks.e.water_pump': 'Infinite Water Pump',
    'book.fieldworks.e.water_pump.p1': 'Immersion intake: works only while at least two faces touch $(bold)water source blocks$(). Produces water purely from energy — into its tank and adjacent machines; pipes pull automatically.',
    'book.fieldworks.e.water_pump.p2': 'Three gears: low 1 bucket/s, high 3 buckets/s, and $(bold)unlimited$() — capped only by your pipe network, with exponentially rising power cost. Switch gears via the left tab in the GUI.',
    'book.fieldworks.e.accessories': 'Field Accessories',
    'book.fieldworks.e.accessories.p1': 'Products of strong artificial fields, worn: Resonant Band (haste), Magnetic Focus Lens (magnet), Fieldline Amulet (featherfall), Flux Belt (luck), Tidal Charm (water breathing), Void Step Ring (blink).',
    'book.fieldworks.e.accessories.p2': 'Equip into Curios slots to take effect (collectible without Curios). Craft them in the Assembler from field materials.',
    'book.fieldworks.e.accessories.p3': 'The ultimate curio, the $(bold)Superconductor Harmonic Core$(): 200k FE capacitor; incoming damage is absorbed at 400 FE per HP — a full absorb also immunizes the effects of that very hit; every 2000 FE spent stacks a random stat buff for 30s, stacking freely with independent timers. Charge with Resonance Fuel or passively in strong magnetic fields.',
    'book.fieldworks.e.purity': 'Purity',
    'book.fieldworks.e.purity.p1': 'Gases carry 0–100 purity: raw <60, refined 60–90, ultra >= 90. Machine tier gates the ceiling.',
    'book.fieldworks.e.purity.p2': 'Three values of purity: better recipe outcomes, x2 pipe throughput, halved field-medium wear.',
    'book.fieldworks.e.magnetic': 'Bedrock Magnetism',
    'book.fieldworks.e.magnetic.p1': 'Below y=-32 natural magnetism climbs to 0.5 T — magnetizers and Tier-III electrolyzers love the deep.',
    'book.fieldworks.e.magnetic.p2': 'Thunderstorm spikes are free magnetization windows; compasses spin wild in strong fields.',
    'book.fieldworks.e.tide': 'Moon & Tides',
    'book.fieldworks.e.tide.p1': 'Full moon +15% gravity, new moon −10%, seas add tidal pull. "Separation is +20% on full-moon nights" — schedule with the Tidal Calendar.',
    'book.fieldworks.e.vacuum': 'Height Is Vacuum',
    'book.fieldworks.e.vacuum.p1': 'Sky pressure halves every 100 blocks: mountains are natural vacuum inlets. Sky Platforms are your steps.',
    'book.fieldworks.e.voidline': 'The Void Interface',
    'book.fieldworks.e.voidline.p1': 'The Resonant Drill breaches bedrock (~5 minutes) opening a 1–8 block shell of Void Air — raw material of the endgame. Below is still void: not a new dimension.',
    'book.fieldworks.e.five_pieces': 'The Five-Piece Set',
    'book.fieldworks.e.five_pieces.p1': '$(thing)Frame$() defines the envelope (3³–32³); the $(thing)Injector$() decides the field type by medium; the $(thing)Coupler$() links injectors for stacking or multi-field overlays; the $(thing)Constrainer$() stops leakage; the $(thing)Stabilizer$() lifts the cap 20%.',
    'book.fieldworks.e.five_pieces.p2': 'Free-form building: frame → injector → medium → power. No puzzles, only freedom.',
    'book.fieldworks.e.five_pieces.p3': 'Coupling capacity = 1 + edge/8. A 32³ frame takes five injectors — space for strength.',
    'book.fieldworks.e.coupling': 'Coupling & Field Programming',
    'book.fieldworks.e.coupling.p1': 'Same-type: EM x1.6 each (to 100 T). Different types coexist: 20 T EM + 10⁻³ Pa vacuum + −40 °C is how superconductor annealing happens.',
    'book.fieldworks.e.coupling.p2': 'Field programming starts at P1 siting, not at the endgame.',
    'book.fieldworks.e.harmonic': 'Harmonic & Enchanting',
    'book.fieldworks.e.harmonic.p1': 'Tuning forks set the style: $(bold)Harm$() biases damage enchantments, $(bold)Fortune$() harvest ones, $(bold)Ward$() protection ones — cross-category themes.',
    'book.fieldworks.e.harmonic.p2': '6 lü: +5 shelf power; 8 lü: ultra-rares x3; 9 lü: $(bold)level breakthrough to VI$() via the Rhythmic Book.',
    'book.fieldworks.e.harmonic.p3': 'Chambers of different styles do not stack. Yields to Apotheosis when present.',
    'book.fieldworks.e.tame': 'Tame Field',
    'book.fieldworks.e.tame.p1': '20 breath: anger cleared; 40: growth x2; 60: rare drops x1.5; 80: regeneration for tamed beasts — the heart of ranches and pet hospitals.',
    'book.fieldworks.e.multiblock': 'Giant Rules',
    'book.fieldworks.e.multiblock.p1': 'Truss (paintable) + shell plates + observation glass build 9³+ giants — formed giants self-load their chunks, no power needed.',
    'book.fieldworks.e.multiblock.p2': 'Missing pieces are particle-marked. The giant itself is an instrument: output = halo, fault = red flash.',
    'book.fieldworks.e.tower': 'Resonance Power Tower',
    'book.fieldworks.e.tower.p1': 'Energy flagship: burns resonance fuel (three-factor efficiency) with output scaling by height.',
    'book.fieldworks.e.tower.p2': '$(bold)Three-stage failure$(): warning (lossless) → forced shutdown (50% fuel loss) → explosion $(italic)only by deliberate fuel injection during shutdown$(), with 30 s server-wide warning and an 8-second visible shockwave. 120 seconds of warning, always.',
    'book.fieldworks.e.dome': 'Eco Dome',
    'book.fieldworks.e.dome.p1': 'Full four-field control in 32³ — a rainforest workshop in the desert; climate recipes at full tilt. Blackouts fade smoothly, never break.',
    'book.fieldworks.e.anchor': 'Warp Anchor',
    'book.fieldworks.e.anchor.p1': 'Region relocation (16³/32³/48³): precheck → freeze → serialize to a $(thing).fwr$() file (SHA-256, atomic write) → afterimage → atomic swap. Failure rolls back; saves are never harmed.',
    'book.fieldworks.e.anchor.p2': 'Portal frames, ender chests and chunk loaders are blacklisted — flagged with particles beforehand.',
    'book.fieldworks.e.array': 'Planetary Tuning Array',
    'book.fieldworks.e.array.p1': 'The endgame: a ring of sixteen towers around a resonance well — server-wide auroras, enchantment broadcast, potion broadcast billed by type x level x players.',
    'book.fieldworks.e.array.p2': 'Global intervention has reaction: faster tuning means exponential cost. This is the operator\'s building — you write the planet\'s parameters.',
    'book.fieldworks.e.carbon': 'The Carbon Ledger',
    'book.fieldworks.e.carbon.p1': 'One server-wide cp (0–1000, equilibrium 200), raised by every mod\'s burning and spending, decayed faster the further it strays. High carbon adds a slight temperature multiplier.',
    'book.fieldworks.e.carbon.p2': 'The ledger never calls in debts: the price of carbon is missed efficiency, not disaster. It is the one shared progress.',
    'book.fieldworks.e.capture': 'Carbon Capturer',
    'book.fieldworks.e.capture.p1': 'Spends energy to capture carbon into carbon black and dry ice; efficiency rises with cp — tune, don\'t zero.',
    'book.fieldworks.e.corrosion': 'Corrosion Trap',
    'book.fieldworks.e.corrosion.p1': '"Pollution is ore": traps corrosive air into sulfur and corrosion salts while cleansing the biome.',
    'book.fieldworks.e.fields_table': 'Fields & Units',
    'book.fieldworks.e.fields_table.p1': 'Atmosphere (vector) / Light (0–15) / Temperature (°C) / Humidity (%RH) / Magnetic (T) / Gravity (g) / Vacuum (Pa, log) / Radiation (µSv/h) / Carbon (cp) / artificial EM·Gravity·Vacuum·Radiation / Harmonic (0–12) / Tame (0–100) / Warp (0–9).',
    'book.fieldworks.e.api': 'The API',
    'book.fieldworks.e.api.p1': 'Fields, atmospheres, recipe conditions and machine behavior are all registry-driven and datapackable: register via $(thing)FieldRegistry.register()$() — machines and recipes pick it up with zero changes. Open source under MPL-2.0.',
    'book.fieldworks.e.furnace': 'Continuous Firing',
    'book.fieldworks.e.furnace.p1': '$(thing)Flint & steel on a vanilla furnace$(): continuous firing — auto-relights while idle; placing an ingredient returns it to vanilla.',
    'book.fieldworks.e.furnace.p2': 'Burning furnaces lend +2 °C within 4 blocks (to +5) — an affordable early temperature tool.',
    **EN_ADD,
}

for lang, tbl in (('zh_cn', ZHT), ('en_us', ENT)):
    # 合并进 lang 文件
    import gen_lang  # noqa
    lp = os.path.join(BASE, 'assets', 'fieldworks', 'lang', f'{lang}.json')
    with open(lp, encoding='utf-8') as f:
        data = json.load(f)
    data.update(tbl)
    with open(lp, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f'[gen_book] {lang}: +{len(tbl)} book keys -> {len(data)} total')
