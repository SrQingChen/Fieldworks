#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
GUI 几何自检（261012规划 T3.3 / T10）：
1. FWWidgets 常量与 atlas_coords.java.frag(生成器事实源) 完全一致
2. MachineLayout v3 全模板：menu 槽位 vs 部件矩形 vs 背包区 互不重叠
3. 图集矩形(生成器侧) 无重叠/越界
"""
import os, re, sys

ROOT = os.path.dirname(os.path.abspath(__file__))
SRC = os.path.join(ROOT, "..", "src", "main", "java", "com", "srqingchen", "fieldworks")
fail = 0

# ── 1. FWWidgets 常量 vs 生成器坐标 ──
# Java 命名规则：SLOTS/OV/ICON/PROG 族 X 用裸名, Y 共享(SLOT_Y/OV_Y/ICON_Y/PROG_Y)；
# ENERGY 族共享 ENERGY_Y；其余用 NAME_X/NAME_Y。
frag = {}
for line in open(os.path.join(ROOT, "atlas_coords.java.frag"), encoding="utf-8"):
    n, x, y, w, h = line.split()
    frag[n] = (int(x), int(y), int(w), int(h))
jw = open(os.path.join(SRC, "client", "gui", "widget", "FWWidgets.java"), encoding="utf-8").read()
def java_const(pat):
    m = re.search(rf'{re.escape(pat)}\s*=\s*(\d+)', jw)
    return int(m.group(1)) if m else None
SHARED_Y = {"SLOT": "SLOT_Y", "OV": "OV_Y", "ICON": "ICON_Y", "PROG": "PROG_Y", "ENERGY_WELL": "ENERGY_Y",
            "ENERGY_FILL": "ENERGY_Y"}
for n, (x, y, w, h) in frag.items():
    if n.startswith("SLOT_"):
        jx, jy = java_const(n), java_const("SLOT_Y")
    elif n.startswith("OV_"):
        jx, jy = java_const(n), java_const("OV_Y")
    elif n.startswith("ICON_"):
        jx, jy = java_const(n), java_const("ICON_Y")
    elif n.startswith("PROG_"):
        jx, jy = java_const(n), java_const("PROG_Y")
    elif n in ("ENERGY_WELL", "ENERGY_FILL"):
        jx, jy = java_const(n + "_X"), java_const("ENERGY_Y")
    elif n.startswith("TAB_"):
        jx, jy = java_const(n + "_X"), java_const("TAB_Y")
    elif n in ("FACEBTN", "CFGOV"):
        jx, jy = java_const(n + "_X"), java_const(n + "_Y")
    else:
        jx, jy = java_const(n + "_X"), java_const(n + "_Y")
    if jx != x or jy != y:
        print(f"✗ 坐标不一致: {n} java=({jx},{jy}) gen=({x},{y})"); fail += 1
    if n.startswith("BG_") or n in ("FITBADGE", "UPGPANEL", "SIDEWIN", "JEI_BG"):
        jwv, jhv = java_const(n + "_W"), java_const(n + "_H")
        if jwv != w or jhv != h:
            print(f"✗ 尺寸不一致: {n} java=({jwv},{jhv}) gen=({w},{h})"); fail += 1

# ── 2. 布局重叠检查(与 MachineLayout.java v3 的硬拷贝核对) ──
ml = open(os.path.join(SRC, "machine", "MachineLayout.java"), encoding="utf-8").read()
expect = {
    "playerInvY TANK=108": "108", "playerInvY default=84": "84",
    "upgradeX=windowW()+7": "+ 7",
    "GEN fuel (56,30)": "56, 30", "WIDE input 44": "44 + i * 18, 36",
}
for k, needle in expect.items():
    if needle not in ml:
        print(f"✗ MachineLayout 缺少 {k}"); fail += 1

def rects(template, inputs, outputs, tanks):
    """复算 MachineLayout v3 各模板矩形(窗口 176×H, 机器区 y17..79)。"""
    inv_y = 108 if template == "TANK" else 84
    H = 190 if template == "TANK" else 166
    rs = [("inv", 8, inv_y, 9 * 18, 4 * 18 + 4), ("panel", 0, 0, 176, H)]
    slots = []
    for i in range(inputs):
        slots.append((f"in{i}", 44 + i * 18, 36) if template in ("WIDE", "COMPACT") else (f"in{i}", 56, 30))
    for i in range(outputs):
        slots.append((f"out{i}", 116 + i * 18, 36))
    widgets = [("energy", 8, 22, 8, 56)]
    if template == "TANK":
        widgets += [("gaugeIn", 24, 20, 18, 62), ("gaugeOut", 134, 20, 18, 62), ("prog", 78, 44, 16, 6)]
    if template == "GEN":
        widgets += [("gaugeIn", 24, 18, 18, 62), ("prog", 56, 50, 13, 13)]
        slots = [("fuel", 56, 30), ("out0", 116, 30)]
    if template == "STORE":
        widgets = [("gaugeIn", 79, 18, 18, 62)]
        slots = []
    if template == "PART":
        slots = [(f"in{i}", 30 + (i % 3) * 18, 26 + (i // 3) * 18) for i in range(inputs)][:9]
        widgets += [("prog", 90, 43, 24, 16)]
        slots = slots + [("out0", 116, 44)]
    if template == "PUMP":
        widgets += [("gaugeIn", 79, 22, 18, 62)]
        slots = []
    for n, x, y in slots:
        widgets.append((n, x - 1, y - 1, 18, 18))
    # 槽与部件互检(不含 energy/inv/panel 基件)
    check = [w for w in widgets if w[0] != "energy"]
    for i in range(len(check)):
        for j in range(i + 1, len(check)):
            n1, x1, y1, w1, h1 = check[i]
            n2, x2, y2, w2, h2 = check[j]
            ox = max(0, min(x1 + w1, x2 + w2) - max(x1, x2))
            oy = max(0, min(y1 + h1, y2 + h2) - max(y1, y2))
            if ox > 0 and oy > 0:
                print(f"✗ {template} 部件重叠: {n1} × {n2}"); fail += 1
    # 槽/部件不越机器区(背包 inv_y 之上)
    for n, x, y, w, h in check:
        if y + h > inv_y:
            print(f"✗ {template} {n} 越入背包区(y+h={y+h} > {inv_y})"); fail += 1
        if x + w > 176:
            print(f"✗ {template} {n} 越出右缘({x+w})"); fail += 1

rects("WIDE", 2, 1, 0)
rects("TANK", 0, 0, 2)
rects("GEN", 1, 1, 1)
rects("STORE", 0, 0, 1)
rects("COMPACT", 2, 1, 0)
rects("PART", 9, 1, 0)     # 261014 T5：部件台 3×3
rects("PUMP", 0, 0, 1)     # 261014 T7：无限水泵

# ── 4. 261014 新哨兵 ──
SRCJ = os.path.join(ROOT, "..", "src", "main", "java", "com", "srqingchen", "fieldworks")
ms = open(os.path.join(SRCJ, "client", "gui", "MachineScreen.java"), encoding="utf-8").read()
for k, needle in [("IO tab y=58", "new Tab(58,"), ("Energy tab y=80", "new Tab(80,"),
                  ("Pump gear tab y=102", "new Tab(102,")]:
    if needle not in ms:
        print(f"✗ MachineScreen 缺少 {k}"); fail += 1
atlas_src = open(os.path.join(ROOT, "gen_atlas_v3.py"), encoding="utf-8").read()
if "col * 18 - 1, inv_y + row * 18 - 1" not in atlas_src or "inv_y + 58 - 1" not in atlas_src:
    print("✗ gen_atlas_v3 背包格未按 -1 对齐烘焙"); fail += 1
menu = open(os.path.join(SRCJ, "machine", "MachineMenu.java"), encoding="utf-8").read()
if "client ? clientCache[index] : live(index)" not in menu or "clientCache[index] = value" not in menu:
    print("✗ MachineMenu 数据槽客户端缓存缺失(接线修复回归)"); fail += 1

print("GUI 几何自检:", "✗ FAIL" if fail else "✓ PASS", f"({fail} 处问题)")
sys.exit(1 if fail else 0)
