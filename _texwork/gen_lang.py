# -*- coding: utf-8 -*-
"""场域工艺 lang 生成器(zh_cn + en_us，§15.3 同步交付)。运行：python _texwork/gen_lang.py"""
import json
import os
from gen_data import MACHINE_BLOCKS, SIMPLE_BLOCKS, ITEMS

BASE = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')

ZH_BLOCKS = {
    'steam_generator': '固体燃料蒸汽发电机', 'part_bench': '简易部件台', 'infinite_water_pump': '无限水泵',
    'electrolyzer_1': '电解器 Ⅰ', 'electrolyzer_2': '电解器 Ⅱ', 'electrolyzer_3': '电解器 Ⅲ',
    'plasma_electrolysis_matrix': '等离子电解矩阵', 'blast_furnace': '鼓风熔炉',
    'compressor': '压缩机', 'distillation_tower': '分馏塔', 'blower': '鼓风机',
    'hydrogen_shaft_furnace': '氢气竖炉', 'steam_turbine': '蒸汽轮机', 'assembler': '装配机',
    'condenser': '冷凝器', 'recycler': '回收炉', 'magnetizer': '磁化机',
    'magnetic_separator': '磁选机', 'gravity_separator': '重力分选机', 'deep_drill': '深钻',
    'negative_pressure_catcher': '负压捕获器', 'irradiation_chamber': '辐照室',
    'heat_exchanger': '热交换器', 'carbon_capturer': '碳捕获器', 'corrosion_trap': '蚀捕器',
    'gas_tank_1': '储气罐', 'gas_tank_4': '大型储气罐', 'gas_tank_16': '巨型储气罐',
    'magnetometer': '磁场仪', 'tidal_meter': '潮汐计', 'carbon_gauge': '碳压计',
    'observation_lighthouse': '观测灯塔', 'spectrometer': '光谱仪',
    'stone_catcher': '石制捕获器', 'fluid_pipe': '流体管道', 'item_pipe': '物品管道', 'energy_pipe': '能量管道',
    'field_frame': '场框架', 'field_injector': '场输入器', 'field_coupler': '场耦合器',
    'field_constrainer': '场约束器', 'field_stabilizer': '场稳定器',
    'reinforced_truss': '强化桁架', 'steel_shell': '钢外壳板', 'observation_glass': '观测玻璃',
    'steel_casing': '钢外壳', 'giant_core': '巨物控制核心', 'amethyst_plate': '紫晶蚀刻铭牌',
    'resonance_tower': '谐振聚能塔', 'deep_electrolysis_array': '深井电解阵列',
    'eco_dome': '生态穹顶', 'warp_anchor': '曲率锚',
    'harmonic_cathedral': '谐律大教堂', 'planetary_array': '行星调谐阵列',
    'bauxite_ore': '铝土矿', 'deepslate_bauxite_ore': '深板岩铝土矿',
    'monazite_ore': '独居石矿', 'resonant_cluster': '谐振晶簇',
    'sky_platform': '高空平台', 'harmonic_chamber': '谐律腔',
    'harmonic_enchant_pod': '谐律附魔舱', 'tame_generator': '驯息发生器',
    'phase_item_bridge': '相位物品桥', 'resonant_drill': '谐振钻头', 'crystal_farm': '晶圃',
}
EN_BLOCKS = {
    'steam_generator': 'Solid-Fuel Steam Generator', 'part_bench': 'Simple Part Bench',
    'infinite_water_pump': 'Infinite Water Pump',
    'electrolyzer_1': 'Electrolyzer I', 'electrolyzer_2': 'Electrolyzer II', 'electrolyzer_3': 'Electrolyzer III',
    'plasma_electrolysis_matrix': 'Plasma Electrolysis Matrix', 'blast_furnace': 'Blast Furnace',
    'compressor': 'Compressor', 'distillation_tower': 'Distillation Tower', 'blower': 'Blower',
    'hydrogen_shaft_furnace': 'Hydrogen Shaft Furnace', 'steam_turbine': 'Steam Turbine',
    'assembler': 'Assembler', 'condenser': 'Condenser', 'recycler': 'Recycler', 'magnetizer': 'Magnetizer',
    'magnetic_separator': 'Magnetic Separator', 'gravity_separator': 'Gravity Separator', 'deep_drill': 'Deep Drill',
    'negative_pressure_catcher': 'Negative Pressure Catcher', 'irradiation_chamber': 'Irradiation Chamber',
    'heat_exchanger': 'Heat Exchanger', 'carbon_capturer': 'Carbon Capturer', 'corrosion_trap': 'Corrosion Trap',
    'gas_tank_1': 'Gas Tank', 'gas_tank_4': 'Gas Tank (Large)', 'gas_tank_16': 'Gas Tank (Huge)',
    'magnetometer': 'Magnetometer', 'tidal_meter': 'Tidal Meter', 'carbon_gauge': 'Carbon Gauge',
    'observation_lighthouse': 'Observation Lighthouse', 'spectrometer': 'Spectrometer',
    'stone_catcher': 'Stone Catcher', 'fluid_pipe': 'Fluid Pipe', 'item_pipe': 'Item Pipe', 'energy_pipe': 'Energy Pipe',
    'field_frame': 'Field Frame', 'field_injector': 'Field Injector', 'field_coupler': 'Field Coupler',
    'field_constrainer': 'Field Constrainer', 'field_stabilizer': 'Field Stabilizer',
    'reinforced_truss': 'Reinforced Truss', 'steel_shell': 'Steel Shell Plate',
    'observation_glass': 'Observation Glass', 'steel_casing': 'Steel Casing',
    'giant_core': 'Giant Control Core', 'amethyst_plate': 'Amethyst Etched Plate',
    'resonance_tower': 'Resonance Power Tower', 'deep_electrolysis_array': 'Deep-Well Electrolysis Array',
    'eco_dome': 'Eco Dome', 'warp_anchor': 'Warp Anchor',
    'harmonic_cathedral': 'Harmonic Cathedral', 'planetary_array': 'Planetary Tuning Array',
    'bauxite_ore': 'Bauxite Ore', 'deepslate_bauxite_ore': 'Deepslate Bauxite Ore',
    'monazite_ore': 'Monazite Ore', 'resonant_cluster': 'Resonant Cluster',
    'sky_platform': 'Sky Platform', 'harmonic_chamber': 'Harmonic Chamber',
    'harmonic_enchant_pod': 'Harmonic Enchanting Pod', 'tame_generator': 'Tame Field Generator',
    'phase_item_bridge': 'Phase Item Bridge', 'resonant_drill': 'Resonant Drill', 'crystal_farm': 'Crystal Farm',
}
ZH_ITEMS = {
    'probe_staff': '探测杖', 'spectrum_module': '光谱仪部件', 'fieldpack_terminal': '场谱背包终端',
    'tidal_calendar': '潮汐日历', 'wrench': '扳手',
    'steel_ingot': '钢锭', 'aluminum_ingot': '铝锭', 'silicon_wafer': '硅晶片',
    'compressed_air': '压缩空气', 'carbon_black': '碳黑',
    'stainless_steel_ingot': '不锈钢锭', 'pure_iron_ingot': '电磁纯铁锭', 'invar_ingot': '殷钢锭',
    'permalloy_ingot': '坡莫合金锭', 'monel_ingot': '蒙乃尔锭', 'aluminum_brass_ingot': '铝黄铜锭',
    'resonant_crystal_raw': '粗炼谐振晶体', 'resonant_crystal_refined': '精炼谐振晶体',
    'resonant_crystal_ultra': '超纯谐振晶体', 'magnetized_core': '磁化铁芯', 'field_wire': '场导线',
    'flux_alloy_ingot': '通量合金锭', 'thorium_pellet': '钍丸', 'dry_ice': '干冰',
    'inert_concentrate': '惰性气体浓缩', 'amethyst_lens': '紫晶透镜', 'sulfur': '硫磺',
    'corrosion_salt': '蚀盐', 'void_condensate': '虚空气凝华', 'tuned_void_essence': '调谐虚空精华',
    'void_lens': '虚空透镜', 'tuning_fork': '调谐叉', 'tidal_resonator': '潮汐谐振器',
    'amethyst_resonator': '紫晶谐振腔', 'superconductor_alloy': '超导合金', 'superconductor_ring': '超导环',
    'copper_coil': '铜线圈', 'steel_gear': '钢齿轮', 'piston_frame': '活塞框架',
    'precision_bearing': '精密轴承', 'logic_circuit': '逻辑电路板', 'sensor_core': '传感芯',
    'nozzle_assembly': '喷嘴组件', 'distillation_tray': '精馏塔板', 'electromagnet_core': '电磁铁芯',
    'turbine_blade': '透平叶轮', 'seal_flange': '密封法兰', 'resonator_shell': '谐振腔壳',
    'raw_bauxite': '生铝土', 'raw_monazite': '生独居石', 'resonant_shard': '谐振碎片',
    'upgrade_efficiency_1': '效率芯 Ⅰ', 'upgrade_efficiency_2': '效率芯 Ⅱ', 'upgrade_efficiency_3': '效率芯 Ⅲ',
    'upgrade_field_capacity': '场容芯', 'upgrade_rectifier': '精馏芯', 'upgrade_isolation': '隔离芯',
    'upgrade_superconductor': '超导芯', 'resonance_fuel': '谐振燃料',
    'mercury_fulminate': '雷汞', 'rhythmic_book': '律动之书', 'giant_blueprint': '巨物蓝图',
    'resonant_band': '谐振指环', 'magnetic_lens': '磁准透镜', 'fieldline_amulet': '场线护符',
    'flux_belt': '通量腰带', 'tidal_charm': '潮汐坠饰', 'void_step_ring': '虚空步径戒',
    'superconductor_core': '超导谐律核心',
}
EN_ITEMS = {
    'probe_staff': 'Probe Staff', 'spectrum_module': 'Spectrum Module', 'fieldpack_terminal': 'Fieldpack Terminal',
    'tidal_calendar': 'Tidal Calendar', 'wrench': 'Wrench',
    'steel_ingot': 'Steel Ingot', 'aluminum_ingot': 'Aluminum Ingot', 'silicon_wafer': 'Silicon Wafer',
    'compressed_air': 'Compressed Air', 'carbon_black': 'Carbon Black',
    'stainless_steel_ingot': 'Stainless Steel Ingot', 'pure_iron_ingot': 'Electromagnetic Pure Iron Ingot',
    'invar_ingot': 'Invar Ingot', 'permalloy_ingot': 'Permalloy Ingot', 'monel_ingot': 'Monel Ingot',
    'aluminum_brass_ingot': 'Aluminum Brass Ingot',
    'resonant_crystal_raw': 'Resonant Crystal (Raw)', 'resonant_crystal_refined': 'Resonant Crystal (Refined)',
    'resonant_crystal_ultra': 'Resonant Crystal (Ultra-pure)', 'magnetized_core': 'Magnetized Core',
    'field_wire': 'Field Wire', 'flux_alloy_ingot': 'Flux Alloy Ingot', 'thorium_pellet': 'Thorium Pellet',
    'dry_ice': 'Dry Ice', 'inert_concentrate': 'Inert Gas Concentrate', 'amethyst_lens': 'Amethyst Lens',
    'sulfur': 'Sulfur', 'corrosion_salt': 'Corrosion Salt', 'void_condensate': 'Void Air Condensate',
    'tuned_void_essence': 'Tuned Void Essence', 'void_lens': 'Void Lens', 'tuning_fork': 'Tuning Fork',
    'tidal_resonator': 'Tidal Resonator', 'amethyst_resonator': 'Amethyst Resonator',
    'superconductor_alloy': 'Superconductor Alloy', 'superconductor_ring': 'Superconductor Ring',
    'copper_coil': 'Copper Coil', 'steel_gear': 'Steel Gear', 'piston_frame': 'Piston Frame',
    'precision_bearing': 'Precision Bearing', 'logic_circuit': 'Logic Circuit', 'sensor_core': 'Sensor Core',
    'nozzle_assembly': 'Nozzle Assembly', 'distillation_tray': 'Distillation Tray',
    'electromagnet_core': 'Electromagnet Core', 'turbine_blade': 'Turbine Blade',
    'seal_flange': 'Seal Flange', 'resonator_shell': 'Resonator Shell',
    'raw_bauxite': 'Raw Bauxite', 'raw_monazite': 'Raw Monazite', 'resonant_shard': 'Resonant Shard',
    'upgrade_efficiency_1': 'Efficiency Core I', 'upgrade_efficiency_2': 'Efficiency Core II',
    'upgrade_efficiency_3': 'Efficiency Core III', 'upgrade_field_capacity': 'Field Capacity Core',
    'upgrade_rectifier': 'Rectifier Core', 'upgrade_isolation': 'Isolation Core',
    'upgrade_superconductor': 'Superconductor Core', 'resonance_fuel': 'Resonance Fuel',
    'mercury_fulminate': 'Mercury Fulminate', 'rhythmic_book': 'Rhythmic Book', 'giant_blueprint': 'Giant Blueprint',
    'resonant_band': 'Resonant Band', 'magnetic_lens': 'Magnetic Focus Lens', 'fieldline_amulet': 'Fieldline Amulet',
    'flux_belt': 'Flux Belt', 'tidal_charm': 'Tidal Charm', 'void_step_ring': 'Void Step Ring',
    'superconductor_core': 'Superconductor Harmonic Core',
}

ATMOS_ZH = {
    'ambient_air': ('常规大气', '这里的空气平平无奇。'),
    'oxygen_rich': ('富氧大气', '这片森林的空气尝起来很清新。'),
    'marsh_gas': ('富沼大气', '空气里有沼泽发酵的甜腥味。'),
    'arid_air': ('干热大气', '干燥的热风抽走一切水分。'),
    'frigid_air': ('寒燥大气', '冷冽而干燥，呼出的白汽瞬间消散。'),
    'salt_spray': ('盐雾大气', '带着咸味的海风。'),
    'spore_air': ('菌孢大气', '孢子在光线里浮沉。'),
    'deep_air': ('幽邃大气', '深处的空气沉滞而稀薄。'),
    'void_air': ('虚空气', '这里的空气不属于这个世界。'),
    'sulfuric_gas': ('硫化气', '硫磺的刺鼻气息灼烧着鼻腔。'),
    'ashen_gas': ('灰烬气', '灰烬遮天蔽日。'),
    'basaltic_gas': ('玄温气', '灼热而沉重的火山气体。'),
    'scorch_gas': ('灼流气', '热浪扭曲了视野。'),
    'fungal_gas': ('菌林气', '菌类的气息浓郁得化不开。'),
    'tuned_void': ('调谐虚空气', '空气在某种频率下微微震颤。'),
}
ATMOS_EN = {
    'ambient_air': ('Ambient Air', 'Plain, unremarkable air.'),
    'oxygen_rich': ('Oxygen-rich Air', 'This forest breathes freshness.'),
    'marsh_gas': ('Marsh Gas', 'Sweet ferment of the swamp.'),
    'arid_air': ('Arid Air', 'Dry heat leaches all moisture.'),
    'frigid_air': ('Frigid Air', 'Cold and dry; your breath vanishes.'),
    'salt_spray': ('Salt Spray', 'A briny sea wind.'),
    'spore_air': ('Spore Air', 'Spores drift through the light.'),
    'deep_air': ('Deep Air', 'Stagnant, thin air of the depths.'),
    'void_air': ('Void Air', 'Air that belongs to no world.'),
    'sulfuric_gas': ('Sulfuric Gas', 'Burning sulfur stings your nose.'),
    'ashen_gas': ('Ashen Gas', 'Ash chokes the sky.'),
    'basaltic_gas': ('Basaltic Gas', 'Heavy volcanic heat.'),
    'scorch_gas': ('Scorch Gas', 'Heat shimmers distort your sight.'),
    'fungal_gas': ('Fungal Gas', 'Thick with fungal musk.'),
    'tuned_void': ('Tuned Void Air', 'The air hums at some frequency.'),
}

ZHD = {}
END = {}
for name, zh in ZH_BLOCKS.items():
    ZHD[f'block.fieldworks.{name}'] = zh
for name, en in EN_BLOCKS.items():
    END[f'block.fieldworks.{name}'] = en
for name, zh in ZH_ITEMS.items():
    ZHD[f'item.fieldworks.{name}'] = zh
for name, en in EN_ITEMS.items():
    END[f'item.fieldworks.{name}'] = en

# 大气类型 + 风味(§8.3 首入提示)
for k, (zh, flavor) in ATMOS_ZH.items():
    ZHD[f'atmosphere.fieldworks.{k}'] = zh
    ZHD[f'atmosphere.fieldworks.{k}.flavor'] = '§8' + flavor
for k, (en, flavor) in ATMOS_EN.items():
    END[f'atmosphere.fieldworks.{k}'] = en
    END[f'atmosphere.fieldworks.{k}.flavor'] = flavor

# 流体类型
for fid, zh in [('compressed_air', '压缩空气'), ('oxygen', '氧气'), ('nitrogen', '氮气'),
                ('hydrogen', '氢气'), ('marsh_gas', '沼气'), ('carbon_dioxide', '二氧化碳'),
                ('inert_gas', '惰性气体'), ('sulfuric_dilute', '稀硫酸'), ('steam', '蒸汽'),
                ('void_essence', '虚空气'), ('tuned_void_gas', '调谐虚空气'), ('nether_gas', '下界瓦斯')]:
    ZHD[f'fluid_type.fieldworks.{fid}'] = zh
    END[f'fluid_type.fieldworks.{fid}'] = fid.replace('_', ' ').title()

ZHD.update({
    'itemGroup.fieldworks.main': '场域工艺',
    'tooltip.fieldworks.probe_staff': '潜行右键：读取脚下四大场(耐久 16 次)',
    'tooltip.fieldworks.wrench': '右键机器：旋转朝向(保留 IO 配置与数据)',
    'tooltip.fieldworks.fieldpack_terminal': '右键：便携全场学读数',
    'tooltip.fieldworks.fit.line1': '环境适配率 %s%%——当前为什么是这个速度',
    'tooltip.fieldworks.fit.line2': '§7场学归因(Shift)：',
    'gui.fieldworks.fit.title': '环境适配',
    'tooltip.fieldworks.rhythmic_book.clarify': '谐律附魔「等级突破」材料（≥9 律时使用）',
    'tooltip.fieldworks.rhythmic_book.guide': '教程书是《场域学入门》：合成、或首次合成探测杖附赠',
    'gui.fieldworks.giant.status.formed': '§a巨物运转中(结构 %s%%)',
    'gui.fieldworks.giant.status.incomplete': '§6结构未完成：%s%%(需桁架/外壳/玻璃)',
    'message.fieldworks.tower.warn1': '§6[场域工艺] ⚠ 谐振告警：%s %s %s —— 输出降至 80%(60 秒内补介质/降负载可恢复)',
    'message.fieldworks.tower.warn2': '§c[场域工艺] ⚠ 场强跌落：强制停机中(燃料将损耗 50%)',
    'message.fieldworks.tower.warn3': '§4[场域工艺] ☢ 谐振失控！持续注燃料触发爆炸预警：30 秒',
    'message.fieldworks.tower.explode': '§4[场域工艺] ☢ 谐振失控爆炸！',
    'message.fieldworks.furnace.on': '§a持续燃烧模式：开启(放入原料自动交还原版)',
    'message.fieldworks.furnace.off': '§7持续燃烧模式：关闭',
    'message.fieldworks.void_interface_open': '§5基岩贯穿——虚空界面已开启(§7采集需负压捕获器§5)',
    'message.fieldworks.tidal_calendar': '§b[潮汐日历] 月相 %s 引力 %s 分选效率 %s',
    'message.fieldworks.relocation.start': '§b[曲率锚] 区域冻结：%s 方块，序列化中…',
    'message.fieldworks.relocation.done': '§a[曲率锚] 迁移完成：%s',
    'message.fieldworks.relocation.kept': '§7.fwr 凭据保留 %s 游戏日(管理员可找回)',
    'message.fieldworks.relocation.failed': '§c[曲率锚] 迁移失败：事务已回滚',
    'message.fieldworks.relocation.too_large': '§c超出最大迁移边长：%s',
    'message.fieldworks.relocation.blacklist': '§6区域内有 %s 个黑名单方块(传送门/末影箱/加载器)——请先移除',
})
END.update({
    'itemGroup.fieldworks.main': 'Fieldworks',
    'tooltip.fieldworks.probe_staff': 'Sneak-use: read the four base fields (16 uses)',
    'tooltip.fieldworks.wrench': 'Use on machine: rotate facing (keeps IO & data)',
    'tooltip.fieldworks.fieldpack_terminal': 'Use: portable field readings',
    'tooltip.fieldworks.fit.line1': 'Field Fit %s%% — why the machine runs at this speed',
    'tooltip.fieldworks.fit.line2': '§7Field breakdown (Shift):',
    'gui.fieldworks.fit.title': 'Field Fit',
    'tooltip.fieldworks.rhythmic_book.clarify': 'Material for harmonic enchantment level breakthrough (use at 9+ lü)',
    'tooltip.fieldworks.rhythmic_book.guide': 'The tutorial book is the Introduction to Field Studies',
    'gui.fieldworks.giant.status.formed': '§aGiant operational (structure %s%%)',
    'gui.fieldworks.giant.status.incomplete': '§6Structure incomplete: %s%% (needs truss/shell/glass)',
    'message.fieldworks.tower.warn1': '§6[Fieldworks] Resonance warning: output reduced (60s to recover)',
    'message.fieldworks.tower.warn2': '§c[Fieldworks] Field collapse: forced shutdown',
    'message.fieldworks.tower.warn3': '§4[Fieldworks] RUNAWAY RESONANCE! Explosion in 30s',
    'message.fieldworks.tower.explode': '§4[Fieldworks] Catastrophic resonance explosion!',
    'message.fieldworks.furnace.on': '§aContinuous firing: ON',
    'message.fieldworks.furnace.off': '§7Continuous firing: OFF',
    'message.fieldworks.void_interface_open': '§5Bedrock breached — Void Interface open',
    'message.fieldworks.tidal_calendar': '§b[Tidal Calendar] Phase %s gravity %s bonus %s',
    'message.fieldworks.relocation.start': '§b[Warp Anchor] Region frozen: %s blocks, serializing...',
    'message.fieldworks.relocation.done': '§a[Warp Anchor] Relocated: %s',
    'message.fieldworks.relocation.kept': '§7.fwr kept %s game days',
    'message.fieldworks.relocation.failed': '§c[Warp Anchor] Failed: rolled back',
    'message.fieldworks.relocation.too_large': '§cExceeds max size: %s',
    'message.fieldworks.relocation.blacklist': '§6%d blacklisted blocks in region — remove them first',
})

# ═══ 261013 T4：机器 GUI/JEI/投影/Jade 补全(此前代码引用但底表缺失) ═══

# 配方族 JEI 类目标题(与 Java MachineRecipeFamily 18 族镜像)
FW_FAMILY_ZH = {
    'electrolysis': '电解', 'part_crafting': '部件加工', 'blast_smelting': '鼓风熔炼',
    'compressing': '压缩', 'distilling': '分馏', 'condensing': '冷凝',
    'assembling': '装配', 'recycling': '回收', 'magnetizing': '磁化',
    'magnetic_separating': '磁选', 'gravity_separating': '重力分选',
    'deep_drilling': '深钻', 'vacuum_catching': '负压捕获', 'irradiating': '辐照',
    'heat_exchanging': '热交换', 'shaft_smelting': '竖炉熔炼',
    'corrosion_trapping': '蚀捕', 'carbon_capturing': '碳捕获',
}
FW_FAMILY_EN = {
    'electrolysis': 'Electrolysis', 'part_crafting': 'Part Crafting', 'blast_smelting': 'Blast Smelting',
    'compressing': 'Compressing', 'distilling': 'Distilling', 'condensing': 'Condensing',
    'assembling': 'Assembling', 'recycling': 'Recycling', 'magnetizing': 'Magnetizing',
    'magnetic_separating': 'Magnetic Separation', 'gravity_separating': 'Gravity Separation',
    'deep_drilling': 'Deep Drilling', 'vacuum_catching': 'Vacuum Catching', 'irradiating': 'Irradiating',
    'heat_exchanging': 'Heat Exchanging', 'shaft_smelting': 'Shaft Smelting',
    'corrosion_trapping': 'Corrosion Trapping', 'carbon_capturing': 'Carbon Capturing',
}
for _f, _zh in FW_FAMILY_ZH.items():
    ZHD[f'jei.fieldworks.{_f}.title'] = _zh
for _f, _en in FW_FAMILY_EN.items():
    END[f'jei.fieldworks.{_f}.title'] = _en

# 巨物蓝图变体名与 tooltip(6 变体, GiantBlueprintItem 动态键)
BLUEPRINT_ZH = {
    'resonance_tower': ('谐振聚能塔蓝图', '投影并搭建：三层收分塔身+谐振冠'),
    'deep_electrolysis_array': ('深井电解阵列蓝图', '投影并搭建：井架+玻璃井'),
    'eco_dome': ('生态穹顶蓝图', '投影并搭建：五环穹顶+地面十字'),
    'warp_anchor': ('曲率锚蓝图', '投影并搭建：双稳定环+四柱'),
    'harmonic_cathedral': ('谐律大教堂蓝图', '投影并搭建：中厅+双塔+玫瑰窗'),
    'planetary_array': ('行星调谐阵列蓝图', '投影并搭建：六调谐柱+谐振井'),
}
BLUEPRINT_EN = {
    'resonance_tower': ('Resonance Tower Blueprint', 'Project & build: tiered tower + resonant crown'),
    'deep_electrolysis_array': ('Deep Electrolysis Array Blueprint', 'Project & build: derrick + glass well'),
    'eco_dome': ('Eco Dome Blueprint', 'Project & build: five-ring dome + cross base'),
    'warp_anchor': ('Warp Anchor Blueprint', 'Project & build: twin rings + four pillars'),
    'harmonic_cathedral': ('Harmonic Cathedral Blueprint', 'Project & build: nave + twin towers + rose window'),
    'planetary_array': ('Planetary Array Blueprint', 'Project & build: six tuning pillars + well'),
}
for _v, (_n, _t) in BLUEPRINT_ZH.items():
    ZHD[f'item.fieldworks.giant_blueprint.{_v}'] = _n
    ZHD[f'tooltip.fieldworks.blueprint.{_v}'] = _t
for _v, (_n, _t) in BLUEPRINT_EN.items():
    END[f'item.fieldworks.giant_blueprint.{_v}'] = _n
    END[f'tooltip.fieldworks.blueprint.{_v}'] = _t

ZHD.update({
    # 机器 GUI 页签
    'gui.fieldworks.tab.io': 'IO 配置',
    'gui.fieldworks.tab.energy': '能量',
    'gui.fieldworks.tab.upgrade': '升级芯',
    # IO 浮窗
    'gui.fieldworks.io.title': '六面 IO 配置',
    'gui.fieldworks.io.hint': '左键=下一模式 · 右键=上一模式 · Shift+左键=自动',
    'gui.fieldworks.io.face.up': '顶面', 'gui.fieldworks.io.face.down': '底面',
    'gui.fieldworks.io.face.north': '北面', 'gui.fieldworks.io.face.south': '南面',
    'gui.fieldworks.io.face.west': '西面', 'gui.fieldworks.io.face.east': '东面',
    'gui.fieldworks.io.mode.input': '输入面', 'gui.fieldworks.io.mode.output': '输出面',
    'gui.fieldworks.io.mode.disabled': '禁用', 'gui.fieldworks.io.mode.auto': '自动',
    # 能量/储罐 tooltip
    'tooltip.fieldworks.energy': '能量：%s / %s J',
    'tooltip.fieldworks.tank.amount': '%s / %s mB',
    'tooltip.fieldworks.tank.empty': '空罐',
    # JEI 通用 tooltip
    'jei.fieldworks.recipe.energy': '能耗：%s J',
    'jei.fieldworks.recipe.time': '耗时：%s 秒',
    'jei.fieldworks.recipe.machines': '适用机器：',
    'jei.fieldworks.recipe.fluid_in': '流体输入：%s mB',
    'jei.fieldworks.recipe.fluid_out': '流体输出：%s mB',
    # 投影/手册消息
    'message.fieldworks.book_given': '§b《场域学入门》已放入你的背包——本模组的教程书。',
    'message.fieldworks.projection.hint': '§7Shift+右键投影锚点=清点背包自动搭建；破坏锚点返还蓝图',
    'message.fieldworks.projection.status': '§b投影就绪：%s 个方块待放置',
    'message.fieldworks.projection.invalid': '§c蓝图数据无效(结构缺失)',
    'message.fieldworks.projection.blocked': '§c位置被占用：%s %s %s',
    'message.fieldworks.projection.missing_header': '§6材料清单(缺口)：',
    'message.fieldworks.projection.missing_entry': '§7- %s ×%s',
    'message.fieldworks.projection.missing_core': '§7- 巨物控制核心 ×%s',
    'message.fieldworks.projection.built': '§a巨物搭建完成！结构校验中…',
    # 蓝图用法
    'tooltip.fieldworks.blueprint.use': '§7右键地面放置全息投影',
    # Jade
    'jade.fieldworks.fit': '环境适配 %s%%',
    'jade.fieldworks.energy': '能量 %s / %s J',
    # 蓝图投影/书内投影引导(261013 T5)
    'message.fieldworks.ghost.on': '§b多方块投影已开启——右键方块锚定(以控制核心落位)，Shift+右键旋转',
    'message.fieldworks.ghost.off': '§7多方块投影已关闭',
    'message.fieldworks.ghost.anchored': '§a投影已锚定：缺失 %s / %s；Shift+右键旋转，再次点击书页按钮关闭',
    'message.fieldworks.ghost.rotate': '§7投影旋转：%s°',
})
END.update({
    'gui.fieldworks.tab.io': 'IO Config',
    'gui.fieldworks.tab.energy': 'Energy',
    'gui.fieldworks.tab.upgrade': 'Upgrade Cores',
    'gui.fieldworks.io.title': 'Face IO Configuration',
    'gui.fieldworks.io.hint': 'Left=next mode · Right=previous · Shift+Left=auto',
    'gui.fieldworks.io.face.up': 'Top', 'gui.fieldworks.io.face.down': 'Bottom',
    'gui.fieldworks.io.face.north': 'North', 'gui.fieldworks.io.face.south': 'South',
    'gui.fieldworks.io.face.west': 'West', 'gui.fieldworks.io.face.east': 'East',
    'gui.fieldworks.io.mode.input': 'Input face', 'gui.fieldworks.io.mode.output': 'Output face',
    'gui.fieldworks.io.mode.disabled': 'Disabled', 'gui.fieldworks.io.mode.auto': 'Auto',
    'tooltip.fieldworks.energy': 'Energy: %s / %s J',
    'tooltip.fieldworks.tank.amount': '%s / %s mB',
    'tooltip.fieldworks.tank.empty': 'Empty',
    'jei.fieldworks.recipe.energy': 'Energy: %s J',
    'jei.fieldworks.recipe.time': 'Time: %s s',
    'jei.fieldworks.recipe.machines': 'Machines:',
    'jei.fieldworks.recipe.fluid_in': 'Fluid input: %s mB',
    'jei.fieldworks.recipe.fluid_out': 'Fluid output: %s mB',
    'message.fieldworks.book_given': '§bThe Introduction to Field Studies is in your inventory — this mod\'s guide book.',
    'message.fieldworks.projection.hint': '§7Shift+right-click the anchor = build from inventory; break it to refund the blueprint',
    'message.fieldworks.projection.status': '§bProjection ready: %s blocks to place',
    'message.fieldworks.projection.invalid': '§cInvalid blueprint data (structure missing)',
    'message.fieldworks.projection.blocked': '§cBlocked at: %s %s %s',
    'message.fieldworks.projection.missing_header': '§6Materials needed (missing):',
    'message.fieldworks.projection.missing_entry': '§7- %s ×%s',
    'message.fieldworks.projection.missing_core': '§7- Giant Control Core ×%s',
    'message.fieldworks.projection.built': '§aGiant assembled! Verifying structure…',
    'tooltip.fieldworks.blueprint.use': '§7Right-click ground to place a holographic projection',
    'jade.fieldworks.fit': 'Field Fit %s%%',
    'jade.fieldworks.energy': 'Energy %s / %s J',
    'message.fieldworks.ghost.on': '§bMultiblock projection on — right-click a block to anchor (control core position), Shift+right-click to rotate',
    'message.fieldworks.ghost.off': '§7Multiblock projection off',
    'message.fieldworks.ghost.anchored': '§aAnchored: %s / %s missing; Shift+right-click to rotate, click the book button again to close',
    'message.fieldworks.ghost.rotate': '§7Rotated: %s°',
})

# 进度(§8.5)
ADV_ZH = [('root', '场域工艺', '世界本身就是一台机器。'),
          ('probe', '场学初醒', '合成探测杖——第一份场学读数在等你。'),
          ('first_reading', '第一次读数', '用探测杖读取环境。'),
          ('air_caught', '空气也是资源', '获得压缩空气。'),
          ('power_on', '来电！', '蒸汽发电机首次发电。'),
          ('partsmith', '部件工匠', '部件台产出首个部件。'),
          ('first_machine', '第一台机器', '组装任意场学机器。'),
          ('steelsmith', '炼钢师', '获得钢锭。'),
          ('carbon_ledger', '碳账本', '查看一次碳压计。'),
          ('five_airs', '五方水土', '探测 5 种不同大气。'),
          ('siting_master', '选址师', '任一配方以 Fit≥90% 运行。'),
          ('deep_breath', '深呼吸', '在幽邃大气中满速运转(通风管建成)。'),
          ('envelope', '营造你的场', '放下第一个场框架。'),
          ('giant', '巨物时代', '获得巨物控制核心。'),
          ('superconduct', '超导之心', '制造超导环。')]
ADV_EN = [('root', 'Fieldworks', 'The world itself is a machine.'),
          ('probe', 'Field Awakening', 'Craft a Probe Staff.'),
          ('first_reading', 'First Reading', 'Take a field reading.'),
          ('air_caught', 'Air Is Material', 'Obtain Compressed Air.'),
          ('power_on', 'Power On!', 'First power from the Steam Generator.'),
          ('partsmith', 'Partsmith', 'Craft your first part.'),
          ('first_machine', 'First Machine', 'Assemble any field machine.'),
          ('steelsmith', 'Steelsmith', 'Obtain a Steel Ingot.'),
          ('carbon_ledger', 'Carbon Ledger', 'Check the Carbon Gauge.'),
          ('five_airs', 'Five Skies', 'Sample 5 different atmospheres.'),
          ('siting_master', 'Site Master', 'Run any recipe at Fit >= 90%.'),
          ('deep_breath', 'Deep Breath', 'Full speed in deep air (vent built).'),
          ('envelope', 'Make Your Field', 'Place a Field Frame.'),
          ('giant', 'Age of Giants', 'Obtain a Giant Control Core.'),
          ('superconduct', 'Superconducting Heart', 'Craft a Superconductor Ring.')]
for key, zh, desc in ADV_ZH:
    ZHD[f'adv.fieldworks.{key}'] = zh
    ZHD[f'adv.fieldworks.{key}.desc'] = desc
for key, en, desc in ADV_EN:
    END[f'adv.fieldworks.{key}'] = en
    END[f'adv.fieldworks.{key}.desc'] = desc


# ═══════════ 261014 第五轮新增键(T2/T4/T6/T7/T8/T10) ═══════════
ZHD.update({
    'tooltip.fieldworks.energy.unit_hint': '点击能量条切换显示单位(FE/RF/J)',
    'gui.fieldworks.pump.gear.0': '档位：低速(1桶/秒)',
    'gui.fieldworks.pump.gear.1': '档位：高速(3桶/秒)',
    'gui.fieldworks.pump.gear.2': '档位：无限(耗电指数上升)',
    'gui.fieldworks.pump.gear.hint': '点击切换档位；需浸入水源(≥2 面)',
    'message.fieldworks.stickbuild.invalid': '§c结构数据缺失，无法搭建',
    'message.fieldworks.stickbuild.result': '§b[木棍搭建] 已放置 %s 块 / 原有 %s / 共 %s(朝向 %s°)',
    'message.fieldworks.stickbuild.missing_header': '§6缺少材料：',
    'message.fieldworks.stickbuild.missing_entry': '§7  %s ×%s',
    'tooltip.fieldworks.accessory.slot_hint': '饰品(Curios)：装入对应栏位生效',
    'item.fieldworks.resonant_band.desc': '急迫 I 常驻 + 微幅攻速',
    'item.fieldworks.magnetic_lens.desc': '4 格磁吸掉落物',
    'item.fieldworks.fieldline_amulet.desc': '移速 +3%，摔落伤害 -25%',
    'item.fieldworks.flux_belt.desc': '幸运 +1',
    'item.fieldworks.tidal_charm.desc': '水下供气 + 微幅移速',
    'item.fieldworks.void_step_ring.desc': '潜行移动：3 秒冷却闪现 8 格',
    'item.fieldworks.superconductor_core.desc1': '§d转电盾：受击伤害按 400FE/HP 转为电容耗电抵挡',
    'item.fieldworks.superconductor_core.desc2': '§d谐律增幅：每耗 2000FE 叠一层随机属性(独立 30s)',
    'tooltip.fieldworks.core.energy': '电容：%s / %s FE',
    'tooltip.fieldworks.core.charge_hint': '手持右键消耗谐振燃料充能；强磁场中被动充能',
    'message.fieldworks.core.charged': '§b[超导核心] 充能 +%s FE',
    'message.fieldworks.core.buff.attack': '§d[谐律增幅] 攻击伤害 +1(30s)',
    'message.fieldworks.core.buff.attack_speed': '§d[谐律增幅] 攻击速度 +4%(30s)',
    'message.fieldworks.core.buff.speed': '§d[谐律增幅] 移动速度 +2%(30s)',
    'message.fieldworks.core.buff.armor': '§d[谐律增幅] 护甲 +1(30s)',
    'message.fieldworks.core.buff.knockback': '§d[谐律增幅] 击退抗性 +4%(30s)',
    'block.fieldworks.infinite_water_pump': '无限水泵',
    'item.fieldworks.infinite_water_pump': '无限水泵',
})
END.update({
    'tooltip.fieldworks.energy.unit_hint': 'Click the energy bar to cycle units (FE/RF/J)',
    'gui.fieldworks.pump.gear.0': 'Gear: Low (1 bucket/s)',
    'gui.fieldworks.pump.gear.1': 'Gear: High (3 buckets/s)',
    'gui.fieldworks.pump.gear.2': 'Gear: Unlimited (exponential cost)',
    'gui.fieldworks.pump.gear.hint': 'Click to cycle; must touch water sources (2+ faces)',
    'message.fieldworks.stickbuild.invalid': '§cStructure data missing',
    'message.fieldworks.stickbuild.result': '§b[Stick Build] Placed %s / existing %s / total %s (facing %s°)',
    'message.fieldworks.stickbuild.missing_header': '§6Missing materials:',
    'message.fieldworks.stickbuild.missing_entry': '§7  %s ×%s',
    'tooltip.fieldworks.accessory.slot_hint': 'Curio: equip into its slot to take effect',
    'item.fieldworks.resonant_band.desc': 'Permanent Haste I + slight attack speed',
    'item.fieldworks.magnetic_lens.desc': 'Magnetizes drops within 4 blocks',
    'item.fieldworks.fieldline_amulet.desc': '+3% speed, -25% fall damage',
    'item.fieldworks.flux_belt.desc': 'Luck +1',
    'item.fieldworks.tidal_charm.desc': 'Water breathing + slight speed',
    'item.fieldworks.void_step_ring.desc': 'Sneak-move: blink 8 blocks (3s cooldown)',
    'item.fieldworks.superconductor_core.desc1': '§dPower Shield: converts damage to stored FE (400 FE per HP)',
    'item.fieldworks.superconductor_core.desc2': '§dHarmonic Surge: every 2000 FE spent grants a random stat stack (30s each)',
    'tooltip.fieldworks.core.energy': 'Capacitor: %s / %s FE',
    'tooltip.fieldworks.core.charge_hint': 'Right-click with Resonance Fuel to charge; passive charge in strong fields',
    'message.fieldworks.core.charged': '§b[Core] Charged +%s FE',
    'message.fieldworks.core.buff.attack': '§d[Surge] Attack Damage +1 (30s)',
    'message.fieldworks.core.buff.attack_speed': '§d[Surge] Attack Speed +4% (30s)',
    'message.fieldworks.core.buff.speed': '§d[Surge] Speed +2% (30s)',
    'message.fieldworks.core.buff.armor': '§d[Surge] Armor +1 (30s)',
    'message.fieldworks.core.buff.knockback': '§d[Surge] Knockback Resistance +4% (30s)',
    'block.fieldworks.infinite_water_pump': 'Infinite Water Pump',
    'item.fieldworks.infinite_water_pump': 'Infinite Water Pump',
})

for code, data in [('zh_cn', ZHD), ('en_us', END)]:
    p = os.path.join(BASE, 'assets', 'fieldworks', 'lang', f'{code}.json')
    os.makedirs(os.path.dirname(p), exist_ok=True)
    with open(p, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    print(f'[gen_lang] {code}: {len(data)} keys')
