#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
JEI 类目布局自检(261013 T3)：
模拟 FWJeiPlugin.FamilyCategory 两类布局(物品族/流体族)在全部机器配方下的
槽位/部件矩形——断言互不重叠且不越出类目面板。
与 Java 侧常量手动镜像；改 FWJeiPlugin 布局常量必须同步此处。
"""
import json
import os
import glob

ROOT = os.path.dirname(os.path.abspath(__file__))
R = os.path.join(ROOT, "..", "src", "main", "resources", "data", "fieldworks", "recipe")

# ── FWJeiPlugin 布局镜像 ──
IN_X, IN_Y_ITEM, IN_Y_FLUID = 30, 23, 64
ARROW = (88, 24, 24, 16)
BI = (56, 38, 16, 6)
OUT_X, OUT_Y0 = 118, 5
OUT_X_FLUID, OUT_Y_FLUID = 90, 64
GIN_X, GIN_Y = 10, 10
GOUT_X0, GOUT_STEP, GOUT_Y = 118, 20, 10
ENERGY_ICON_FLUID = (32, 10, 18, 18)
ENERGY_ITEM = (10, 4, 8, 56)
PANEL_ITEM = (146, 64)
PANEL_FLUID = (146, 88)
# 261014 T5：部件台(part_crafting 族=9 输入槽机器) 网格布局(JEI_BG_TALL)
GRID_X, GRID_Y, GRID_STEP = 24, 14, 19
GRID_ARROW = (86, 36, 24, 16)
GRID_OUT = (116, 36, 18, 18)
GRID_ENERGY = (10, 16, 8, 56)

FLUID_FAMS = {'fieldworks:electrolysis', 'fieldworks:compressing', 'fieldworks:distilling',
              'fieldworks:condensing', 'fieldworks:vacuum_catching', 'fieldworks:heat_exchanging',
              'fieldworks:corrosion_trapping', 'fieldworks:shaft_smelting'}


def rects_item(nin, nout):
    return [ENERGY_ITEM] \
        + [(IN_X + i * 19, IN_Y_ITEM, 18, 18) for i in range(min(nin, 3))] \
        + [ARROW] \
        + [(OUT_X, OUT_Y0 + i * 19, 18, 18) for i in range(min(nout, 3))]


def rects_grid(nin, nout):
    return [GRID_ENERGY]         + [(GRID_X + (i % 3) * GRID_STEP, GRID_Y + (i // 3) * GRID_STEP, 18, 18) for i in range(min(nin, 9))]         + [GRID_ARROW, GRID_OUT]


def rects_fluid(nin, nout, f_in, f_out):
    r = [(IN_X + i * 19, IN_Y_FLUID, 18, 18) for i in range(min(nin, 3))]
    if f_in:
        r.append((GIN_X, GIN_Y, 18, 62))
    for i in range(min(f_out, 3)):
        r.append((GOUT_X0 - i * GOUT_STEP, GOUT_Y, 18, 62))
    r.append(BI)
    r.append(ENERGY_ICON_FLUID)
    r += [(OUT_X_FLUID + i * 19, OUT_Y_FLUID, 18, 18) for i in range(min(nout, 3))]
    return r


def main():
    bad = 0
    n = 0
    for f in sorted(glob.glob(os.path.join(R, "*.json"))):
        d = json.load(open(f, encoding="utf-8"))
        t = d.get("type", "")
        if not t.startswith("fieldworks:"):
            continue
        n += 1
        nin, nout = len(d.get("inputs", [])), len(d.get("outputs", []))
        f_in = 1 if d.get("fluid_input") else 0
        f_out = len(d.get("fluid_outputs", []))
        if t == "fieldworks:part_crafting":
            rects, panel = rects_grid(nin, nout), PANEL_FLUID   # 网格用加高面板
        else:
            rects = rects_fluid(nin, nout, f_in, f_out) if t in FLUID_FAMS else rects_item(nin, nout)
            panel = PANEL_FLUID if t in FLUID_FAMS else PANEL_ITEM
        for i in range(len(rects)):
            for j in range(i + 1, len(rects)):
                a, b = rects[i], rects[j]
                if min(a[0] + a[2], b[0] + b[2]) - max(a[0], b[0]) > 0 \
                        and min(a[1] + a[3], b[1] + b[3]) - max(a[1], b[1]) > 0:
                    print(f"  ✗ OVERLAP {os.path.basename(f)}: {a} × {b}")
                    bad += 1
        for a in rects:
            if a[0] + a[2] > panel[0] or a[1] + a[3] > panel[1]:
                print(f"  ✗ OUT-OF-PANEL {os.path.basename(f)}: {a} panel={panel}")
                bad += 1
    print(f"JEI 布局自检: {'✓ PASS' if bad == 0 else '✗ FAIL'} (机器配方 {n})")
    return 1 if bad else 0


if __name__ == "__main__":
    raise SystemExit(main())
