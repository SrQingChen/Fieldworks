#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""手册审计(261012规划 T8.4)：结构同构、lang 键全覆盖、内部链接有效、多方块页合法。"""
import json, os, re, sys

ROOT = os.path.dirname(os.path.abspath(__file__))
B = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "fieldworks", "patchouli_books", "manual")
LANG = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "fieldworks", "lang")
fails = []

zh = json.load(open(os.path.join(LANG, "zh_cn.json"), encoding="utf-8"))
en = json.load(open(os.path.join(LANG, "en_us.json"), encoding="utf-8"))

# 1) en_us / zh_cn 目录同构
def tree(d):
    out = set()
    for root, _, files in os.walk(d):
        for f in files:
            out.add(os.path.relpath(os.path.join(root, f), d).replace("\\", "/"))
    return out
te, tz = tree(os.path.join(B, "en_us")), tree(os.path.join(B, "zh_cn"))
if te != tz:
    fails.append(f"双语目录不同构: 仅en={sorted(te-tz)[:5]} 仅zh={sorted(tz-te)[:5]}")

# 2) 全部文本键存在于两个 lang；categories/entries 必填字段
MB_KEYS = ("pattern", "mapping")
for rel in sorted(tz):
    p = os.path.join(B, "zh_cn", rel)
    d = json.load(open(p, encoding="utf-8"))
    if rel.startswith("categories/"):
        for k in ("name", "description", "icon"):
            if k not in d:
                fails.append(f"{rel}: category 缺 {k}")
        keys = [d["name"], d["description"]]
    elif rel.startswith("entries/"):
        if not str(d.get("category", "")).startswith("fieldworks:"):
            fails.append(f"{rel}: category 非全限定")
        if "icon" not in d or "name" not in d or not d.get("pages"):
            fails.append(f"{rel}: 缺 name/icon/pages")
        keys = [d["name"]]
        for pg in d["pages"]:
            t = pg.get("type", "patchouli:text")
            if not t.startswith("patchouli:") and not t.startswith("fieldworks:"):
                fails.append(f"{rel}: 页面 type 异常 {t}")
            for k in ("text", "title", "name"):
                if k in pg and isinstance(pg[k], str) and "." in pg[k]:
                    keys.append(pg[k])
            if t in ("patchouli:multiblock", "fieldworks:multiblock"):
                if t == "fieldworks:multiblock" and not pg.get("multiblock_variant"):
                    fails.append(f"{rel}: fieldworks:multiblock 页缺 multiblock_variant")
                mb = pg.get("multiblock")
                if not mb or any(k not in mb for k in MB_KEYS):
                    fails.append(f"{rel}: multiblock 页缺 {MB_KEYS}")
                rows = mb["pattern"]
                w = {len(r) for layer in rows for r in layer}
                if len(w) != 1:
                    fails.append(f"{rel}: multiblock pattern 行宽不齐 {w}")
                flat = "".join("".join(r) for r in rows)
                if flat.count("0") != 1:
                    fails.append(f"{rel}: multiblock 中心锚 '0' 数量={flat.count('0')} (须为 1)")
                unknown = set(flat) - set(mb["mapping"].keys()) - {" ", "_"}
                if unknown:
                    fails.append(f"{rel}: pattern 未映射字符 {unknown}")
    for k in keys:
        if k not in zh:
            fails.append(f"{rel}: 键 {k} 缺 zh_cn")
        if k not in en:
            fails.append(f"{rel}: 键 {k} 缺 en_us")

# 3) 内部链接 $(l:fieldworks:manual/xxx) 目标存在
entries = {f"fieldworks:{e[:-5].replace('/', '/')}" for e in tz if e.startswith("entries/")}
# entry id = 文件名(不含扩展)，链接格式 ns:book/entry
valid = {f"fieldworks:manual/{os.path.basename(e)[:-5]}" for e in tz if e.startswith("entries/")}
for lang_file, tbl in (("zh_cn", zh), ("en_us", en)):
    for k, v in tbl.items():
        for m in re.finditer(r"\$\(l:([a-z0-9_:/.]+)", v):
            if m.group(1) not in valid:
                fails.append(f"{lang_file}:{k} 链接目标不存在 {m.group(1)}")

# 4) book.json 合法性
bj = json.load(open(os.path.join(ROOT, "..", "src", "main", "resources", "data", "fieldworks",
                                 "patchouli_books", "manual", "book.json"), encoding="utf-8"))
if bj.get("use_resource_pack") is not True:
    fails.append("book.json use_resource_pack 必须为 true")
for k in ("name", "landing_text", "model"):
    if k not in bj:
        fails.append(f"book.json 缺 {k}")

print("手册审计:", "✗ FAIL" if fails else "✓ PASS")
for f in fails[:20]:
    print(" ✗", f)
print(f"  条目 {len([e for e in tz if e.startswith('entries/')])} × 双语 | 多方块页 {sum(1 for e in tz if 'entries' in e and 'multiblock' in open(os.path.join(B,'zh_cn',e),encoding='utf-8').read())} × 双语")
sys.exit(1 if fails else 0)
