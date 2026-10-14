#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""资产审计(261012规划 T10.3)：贴图引用/物品定义/模型/blockstate 全量扫描零缺失。"""
import json, os, re, sys, zipfile

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(ROOT, "..", "src", "main", "resources")
A = os.path.join(SRC, "assets", "fieldworks")
DATA_D = os.path.join(SRC, "data", "fieldworks")
fails = []
warns = []

def walk(d, ext):
    out = []
    for root, _, files in os.walk(d):
        for f in files:
            if f.endswith(ext):
                out.append(os.path.join(root, f))
    return out

tex_files = set()
for root, _, files in os.walk(os.path.join(A, "textures")):
    for f in files:
        if f.endswith(".png"):
            rel = os.path.relpath(os.path.join(root, f), os.path.join(A, 'textures')).replace(os.sep, '/')
            tex_files.add("fieldworks:" + rel[:-4])

def check_tex(ref, where):
    if ref.startswith("fieldworks:") and ref not in tex_files:
        fails.append(f"{where}: 贴图不存在 {ref}")
    elif not ref.startswith(("fieldworks:", "minecraft:", "patchouli:")):
        warns.append(f"{where}: 外部命名空间贴图 {ref}")

# 1) 模型引用
for p in walk(os.path.join(A, "models"), ".json"):
    d = json.load(open(p, encoding="utf-8"))
    rel = os.path.relpath(p, A).replace(os.sep, '/')
    for key, v in (d.get("textures") or {}).items():
        if key == "particle" and v.startswith("#"):
            continue
        check_tex(v, rel)
    for el in d.get("elements", []) or []:
        for face in (el.get("faces") or {}).values():
            t = face.get("texture", "")
            if not t.startswith("#"):
                check_tex(t, rel)
    # parent 存在性(本地模型)
    par = d.get("parent", "")
    if par.startswith("fieldworks:"):
        pp = os.path.join(A, "models", par.split(":", 1)[1] + ".json")
        if not os.path.exists(pp):
            fails.append(f"{rel}: parent 不存在 {par}")

# 2) blockstate 模型引用
for p in walk(os.path.join(A, "blockstates"), ".json"):
    d = json.load(open(p, encoding="utf-8"))
    rel = os.path.relpath(p, A).replace(os.sep, '/')
    def scan_variants(obj):
        for v in (obj.get("variants") or {}).values():
            mods = v if isinstance(v, list) else [v]
            for m in mods:
                mid = m.get("model", "")
                if mid.startswith("fieldworks:") and not os.path.exists(
                        os.path.join(A, "models", mid.split(":", 1)[1] + ".json")):
                    fails.append(f"{rel}: variant 模型不存在 {mid}")
    scan_variants(d)
    for part in (d.get("multipart") or []):
        applies = part.get("apply")
        if not isinstance(applies, list):
            applies = [applies]
        for app in applies:
            if app is None:
                continue
            if isinstance(app, list):   # 嵌套列表
                apps = app
            else:
                apps = app.get("models") or [app]
            for mod in apps:
                if not isinstance(mod, dict):
                    continue
                mid = mod.get("model", "")
                if mid.startswith("fieldworks:") and not os.path.exists(
                        os.path.join(A, "models", mid.split(":", 1)[1] + ".json")):
                    fails.append(f"{rel}: multipart 模型不存在 {mid}")

# 3) 物品定义(items/*.json) model 字段有效
n_items = 0
for p in walk(os.path.join(A, "items"), ".json"):
    n_items += 1
    d = json.load(open(p, encoding="utf-8"))
    rel = os.path.relpath(p, A).replace(os.sep, '/')
    def scan_model_ref(m):
        t = m.get("type", "")
        if t == "minecraft:model":
            mid = m.get("model", "")
            if mid.startswith("fieldworks:") and not os.path.exists(
                    os.path.join(A, "models", mid.split(":", 1)[1] + ".json")):
                fails.append(f"{rel}: items 模型不存在 {mid}")
        for c in (m.get("models") or []):
            scan_model_ref(c)
    top = d.get("model")
    if top:
        scan_model_ref(top)

# 3.5) 回归哨兵(261012 管道/机器 blockstate 被旧生成器覆盖事故)：管道必须 multipart、机器必须 facing×active 8 变体
import glob as _glob
for pipe in ("fluid_pipe", "item_pipe", "energy_pipe"):
    d = json.load(open(os.path.join(A, "blockstates", pipe + ".json"), encoding="utf-8"))
    if "multipart" not in d or len(d["multipart"]) < 7:
        fails.append(f"blockstates/{pipe}.json: 不是 multipart(六向连接丢失——疑似被旧生成器覆盖!)")
MACHINES_V2 = ['steam_generator','part_bench','infinite_water_pump','electrolyzer_1','electrolyzer_2','electrolyzer_3',
    'plasma_electrolysis_matrix','blast_furnace','compressor','distillation_tower','blower',
    'hydrogen_shaft_furnace','steam_turbine','assembler','condenser','recycler','magnetizer',
    'magnetic_separator','gravity_separator','deep_drill','negative_pressure_catcher',
    'irradiation_chamber','heat_exchanger','carbon_capturer','corrosion_trap',
    'gas_tank_1','gas_tank_4','gas_tank_16']
# 261013 哨兵：管道物品模型必须是直段组合模型(不再指向 v1 遗留方块模型)
for pipe in ("fluid_pipe", "item_pipe", "energy_pipe"):
    im = json.load(open(os.path.join(A, "models", "item", pipe + ".json"), encoding="utf-8"))
    if im.get("parent") != f"fieldworks:block/{pipe}_item":
        fails.append(f"models/item/{pipe}.json: parent={im.get('parent')} (应 block/{pipe}_item 直段模型)")
    if not os.path.exists(os.path.join(A, "models", "block", pipe + "_item.json")):
        fails.append(f"models/block/{pipe}_item.json 缺失")
    if os.path.exists(os.path.join(A, "models", "block", pipe + ".json")):
        fails.append(f"models/block/{pipe}.json 是 v1 遗留模型，应已删除")
# 261014 哨兵：已删方块(gas_pipe/pump/vent_pipe)不得残留任何资产/配方/标签
for gone in ('gas_pipe', 'pump', 'vent_pipe'):
    for root, _, files in os.walk(os.path.join(A, '..')):
        for f in files:
            rel = os.path.relpath(p, A).replace(os.sep, '/')
            if f == gone + '.json' or f.startswith(gone + '_') and f.endswith('.json'):
                raise SystemExit(f"已删方块资产残留: {rel}")
# 261014 哨兵：巨物控制器/核心必须是多面模型(front/side/top)
for ctrl in ('resonance_tower','deep_electrolysis_array','eco_dome','warp_anchor',
             'harmonic_cathedral','planetary_array','giant_core'):
    m = json.load(open(os.path.join(A, 'models/block', ctrl + '.json'), encoding='utf-8'))
    north = m.get('textures', {}).get('north', '')
    if '_front' not in north:
        raise SystemExit(f"控制器模型必须多面贴图: {ctrl}")
    for face in ('front', 'side', 'top'):
        if not os.path.exists(os.path.join(A, 'textures/block', f'{ctrl}_{face}.png')):
            raise SystemExit(f"控制器贴图缺失: {ctrl}_{face}.png")
# 261014 哨兵：饰品物品定义+贴图必须存在
for acc in ('resonant_band','magnetic_lens','fieldline_amulet','flux_belt',
            'tidal_charm','void_step_ring','superconductor_core'):
    for d in ('items', 'models/item'):
        if not os.path.exists(os.path.join(A, d, acc + '.json')):
            raise SystemExit(f"饰品资产缺失: {d}/{acc}.json")
    if not os.path.exists(os.path.join(A, 'textures/item', acc + '.png')):
        raise SystemExit(f"饰品贴图缺失: {acc}.png")
# 261014 哨兵：无限水泵 8 变体 blockstate
bs = json.load(open(os.path.join(A, 'blockstates', 'infinite_water_pump.json'), encoding='utf-8'))
if len(bs.get('variants', {})) != 8:
    raise SystemExit("无限水泵必须 facing×active 8 变体")
# 261014 哨兵：curios 槽位标签存在
for slot in ('ring', 'necklace', 'belt', 'charm'):
    if not os.path.exists(os.path.join(A, '..', '..', 'data', 'curios', 'tags', 'item', slot + '.json')):
        raise SystemExit(f"curios 槽位标签缺失: {slot}")
# 261013 哨兵：projection 模型必须可解析(空 faces 元素在 26.1.2 非法)
_pj = json.load(open(os.path.join(A, "models", "block", "projection.json"), encoding="utf-8"))
if any(e.get("faces") == {} for e in _pj.get("elements", [])):
    fails.append("models/block/projection.json: 含空 faces 元素(26.1.2 非法——空 elements 表才对)")
# 261013 哨兵：巨物结构必须带显式 controller(v2 旧结构无此字段)
for gf in _glob.glob(os.path.join(DATA_D, "giant_structures", "*.json")):
    d = json.load(open(gf, encoding="utf-8"))
    if "controller" not in d:
        fails.append(f"giant_structures/{os.path.basename(gf)}: 缺 controller(疑似 v2 旧结构覆盖!)")
# 261013 哨兵：机器配方 type 必须是 18 个配方族之一(gen_data FAMILIES 镜像)
FW_FAMILY_TYPES = {f"fieldworks:{f}" for f in (
    'electrolysis','part_crafting','blast_smelting','compressing','distilling','condensing',
    'assembling','recycling','magnetizing','magnetic_separating','gravity_separating',
    'deep_drilling','vacuum_catching','irradiating','heat_exchanging','shaft_smelting',
    'corrosion_trapping','carbon_capturing')}
for rf in _glob.glob(os.path.join(DATA_D, "recipe", "*.json")):
    d = json.load(open(rf, encoding="utf-8"))
    t = d.get("type", "")
    if t == "fieldworks:machine":
        fails.append(f"recipe/{os.path.basename(rf)}: 仍用旧单一类型 fieldworks:machine(应按配方族)")
    elif t.startswith("fieldworks:") and t not in FW_FAMILY_TYPES:
        fails.append(f"recipe/{os.path.basename(rf)}: 未知配方族 {t}")
for m in MACHINES_V2:
    p = os.path.join(A, "blockstates", m + ".json")
    if not os.path.exists(p):
        fails.append(f"blockstates/{m}.json 缺失")
        continue
    d = json.load(open(p, encoding="utf-8"))
    vs = list(d.get("variants", {}).keys())
    if len(vs) != 8 or not all("active=" in v for v in vs):
        fails.append(f"blockstates/{m}.json: {len(vs)} 变体(应 8 含 active——疑似被旧生成器覆盖!)")

# 4) 配方引用 id 快查(全部 fieldworks:id 必须在注册名集合中)
mod_ids = set()
REG = os.path.join(ROOT, "..", "src", "main", "java", "com", "srqingchen", "fieldworks", "registry")
for f in os.listdir(REG):
    s = open(os.path.join(REG, f), encoding="utf-8").read()
    mod_ids |= {m.group(1) for m in re.finditer(r'\("([a-z_0-9]+)"', s)}
s = open(os.path.join(ROOT, "..", "src", "main", "java", "com", "srqingchen", "fieldworks",
                     "machine", "MachineSpecs.java"), encoding="utf-8").read()
mod_ids |= {m.group(1) for m in re.finditer(r'\("([a-z_0-9]+)"', s)}
mod_ids |= {"resonance_tower", "eco_dome", "warp_anchor", "harmonic_cathedral", "planetary_array",
            "deep_electrolysis_array"}
for p in walk(os.path.join(SRC, "data", "fieldworks", "recipe"), ".json"):
    d = json.load(open(p, encoding="utf-8"))
    rel = os.path.relpath(p, A).replace(os.sep, '/')
    def scan_id(v, where):
        if isinstance(v, str) and v.startswith("fieldworks:") and "#" not in v:
            path = v.split(":", 1)[1]
            if path not in mod_ids:
                fails.append(f"{rel}: {where} 未知 fieldworks id {v}")
    for k2, v in (d.get("key") or {}).items():
        scan_id(v, "key")
    for x in d.get("ingredients", []) if isinstance(d.get("ingredients"), list) else []:
        scan_id(x, "ingredients")
    r = d.get("result")
    if isinstance(r, dict) and "id" in r:
        scan_id(r["id"], "result")

# 5) 产物 jar 内资源抽查(atlas/machine/管道/巨物结构)
jar = None
for f in os.listdir(os.path.join(ROOT, "..", "build", "libs")):
    if f.endswith(".jar"):
        jar = os.path.join(ROOT, "..", "build", "libs", f)
if jar:
    z = zipfile.ZipFile(jar)
    names = set(z.namelist())
    for must in ["assets/fieldworks/textures/guis/atlas.png",
                 "assets/fieldworks/textures/guis/machine.png",
                 "assets/fieldworks/textures/block/item_pipe_center.png",
                 "assets/fieldworks/textures/block/energy_pipe_center.png",
                 "assets/fieldworks/textures/block/resonance_tower_front.png",
                 "data/fieldworks/giant_structures/resonance_tower.json",
                 "data/fieldworks/recipe/book_manual.json",
                 "data/fieldworks/patchouli_books/manual/book.json",
                 "assets/fieldworks/patchouli_books/manual/zh_cn/entries/giants/tower.json"]:
        if must not in names:
            fails.append(f"jar 缺资源 {must}")
    n_recipe = sum(1 for n in names if n.startswith("data/fieldworks/recipe/"))
    print(f"  jar 内配方 JSON: {n_recipe}")

print(f"  物品定义 {n_items} | 贴图 {len(tex_files)}")
print("资产审计:", "✗ FAIL" if fails else "✓ PASS")
for f in fails[:20]:
    print(" ✗", f)
sys.exit(1 if fails else 0)
