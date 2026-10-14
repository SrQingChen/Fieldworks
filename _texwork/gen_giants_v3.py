#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
巨物多方块结构 v3 —— 261012规划 T7。
六大结构全部重新设计：明确的可搭形状(桁架骨架+钢外壳蒙皮+观测玻璃+紫晶饰顶)，
控制器位置显式声明，4 向旋转均可成形。
产物：
  1. data/fieldworks/giant_structures/<name>.json   (结构 JSON + controller 偏移)
  2. _texwork/giant_patchouli/<name>.json           (patchouli:multiblock 页片段, T8 用)
  3. 材料清单打印(校验搭建成本)
"""
import os, json

ROOT = os.path.dirname(os.path.abspath(__file__))
D = os.path.join(ROOT, "..", "src", "main", "resources", "data", "fieldworks", "giant_structures")
POUT = os.path.join(ROOT, "giant_patchouli")

# 字符 → 方块 id
CH = {
    "T": "fieldworks:reinforced_truss",
    "S": "fieldworks:steel_shell",
    "G": "fieldworks:observation_glass",
    "A": "fieldworks:amethyst_plate",
    "K": "fieldworks:steel_casing",
}
VARIANT_CONTROLLER = {
    "resonance_tower": "resonance_tower",
    "deep_electrolysis_array": "deep_electrolysis_array",
    "eco_dome": "eco_dome",
    "warp_anchor": "warp_anchor",
    "harmonic_cathedral": "harmonic_cathedral",
    "planetary_array": "planetary_array",
}

class Grid:
    def __init__(self, sx, sy, sz):
        self.s = (sx, sy, sz)
        self.c = {}

    def set(self, x, y, z, ch):
        assert 0 <= x < self.s[0] and 0 <= y < self.s[1] and 0 <= z < self.s[2], f"越界 {x},{y},{z} in {self.s}"
        self.c[(x, y, z)] = ch

    def ring(self, y, x0, z0, x1, z1, ch, every=1):
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    self.set(x, y, z, ch)

    def ring_alt(self, y, x0, z0, x1, z1, ch_a, ch_b):
        """四角 ch_a，边中段 ch_b。"""
        for x in range(x0, x1 + 1):
            for z in range(z0, z1 + 1):
                if x in (x0, x1) or z in (z0, z1):
                    corner = (x in (x0, x1)) and (z in (z0, z1))
                    mid = (x in (x0 + 2, x1 - 2)) or (z in (z0 + 2, z1 - 2))
                    self.set(x, y, z, ch_a if corner else (ch_b if mid else ch_a))

    def line(self, x0, y0, z0, x1, y1, z1, ch):
        n = max(abs(x1 - x0), abs(y1 - y0), abs(z1 - z0))
        for i in range(n + 1):
            t = 0 if n == 0 else i / n
            self.set(round(x0 + (x1 - x0) * t), round(y0 + (y1 - y0) * t), round(z0 + (z1 - z0) * t), ch)

    def col(self, x, z, y0, y1, ch):
        for y in range(y0, y1 + 1):
            self.set(x, y, z, ch)

    def cross(self, y, cx, cz, half, ch):
        for d in range(-half, half + 1):
            self.set(cx + d, y, cz, ch)
            self.set(cx, y, cz + d, ch)

def g_tower():
    g = Grid(9, 11, 9)
    g.ring(0, 1, 1, 7, 7, "T")
    g.ring(1, 1, 1, 7, 7, "S")
    for (x, z) in [(4, 1), (4, 7), (1, 4), (7, 4)]:
        g.set(x, 1, z, "G")
    g.ring(2, 1, 1, 7, 7, "G")
    for y in (3, 4, 5, 6):
        g.ring_alt(y, 2, 2, 6, 6, "T", "G")
    g.ring(7, 1, 1, 7, 7, "S")
    g.ring(8, 1, 1, 7, 7, "G")
    g.ring(9, 3, 3, 5, 5, "A")
    g.set(4, 10, 4, "A")
    g.set(4, 1, 4, "C")   # 控制器：基座中心
    return g, (4, 1, 4)

def g_array():
    g = Grid(9, 9, 9)
    g.ring(0, 0, 0, 8, 8, "S")
    for (x, z) in [(0, 0), (8, 0), (0, 8), (8, 8)]:
        g.col(x, z, 0, 6, "T")
    for y in range(1, 7):
        g.ring(y, 3, 3, 5, 5, "G")
    g.ring(3, 0, 0, 8, 8, "T")       # 井架横梁
    g.ring(6, 0, 0, 8, 8, "T")
    g.ring(7, 0, 0, 8, 8, "S")
    for (x, z) in [(4, 0), (4, 8), (0, 4), (8, 4)]:
        g.set(x, 7, z, "G")
    g.ring(8, 3, 3, 5, 5, "A")
    g.set(4, 8, 4, "G")
    g.set(4, 0, 4, "C")              # 控制器：井底中心
    return g, (4, 0, 4)

def g_dome():
    g = Grid(11, 7, 11)
    g.ring(0, 0, 0, 10, 10, "S")
    for (x, z) in [(5, 0), (5, 10), (0, 5), (10, 5)]:
        g.set(x, 0, z, "G")
    g.cross(0, 5, 5, 4, "K")
    g.ring(1, 1, 1, 9, 9, "S")
    for (x, z) in [(5, 1), (5, 9), (1, 5), (9, 5)]:
        g.set(x, 1, z, "G")
    g.ring(2, 2, 2, 8, 8, "G")
    g.ring(3, 3, 3, 7, 7, "G")
    g.ring(4, 4, 4, 6, 6, "A")
    g.set(5, 5, 5, "A")
    g.set(5, 0, 5, "C")              # 控制器：地面中心
    return g, (5, 0, 5)

def g_anchor():
    g = Grid(9, 8, 9)
    g.ring(0, 0, 0, 8, 8, "S")
    g.cross(0, 4, 4, 3, "K")
    g.ring(1, 1, 1, 7, 7, "S")
    for (x, z) in [(1, 1), (7, 1), (1, 7), (7, 7)]:
        g.col(x, z, 2, 3, "T")
    for y in (2, 3):
        g.ring(y, 3, 3, 5, 5, "G")
    g.ring(4, 1, 1, 7, 7, "S")
    g.ring(5, 2, 2, 6, 6, "A")
    g.ring(6, 3, 3, 5, 5, "A")
    g.set(4, 7, 4, "A")
    g.set(4, 1, 4, "C")              # 控制器：环心
    return g, (4, 1, 4)

def g_cathedral():
    g = Grid(11, 10, 11)
    # 地基环
    g.ring(0, 0, 0, 10, 10, "K")
    # 中厅墙体 y1..3 (x2..8, z1..9)，窗玻璃
    for y in (1, 2, 3):
        for x in (2, 8):
            for z in range(1, 10):
                g.set(x, y, z, "G" if (z in (3, 5, 7) and y == 2) else "S")
        for x in range(3, 8):
            g.set(x, y, 9, "G" if y == 2 else "S")     # 后墙
    # 正立面 z=0(门洞 x4..6 y1..2 留空, 玫瑰窗)
    for y in (1, 2, 3):
        for x in range(2, 9):
            if 4 <= x <= 6 and y <= 2:
                continue
            g.set(x, y, 0, "G" if (x in (4, 5, 6) and y == 2) else "S")
    # 山墙顶 y4..6
    for x in range(3, 8):
        g.set(x, 4, 5, "S")
    for x in range(4, 7):
        g.set(x, 5, 5, "S")
    g.set(5, 6, 5, "A")
    # 双塔 (x0..2 与 x8..10, z0..2)
    for tx0 in (0, 8):
        for y in range(1, 8):
            g.ring(y, tx0, 0, tx0 + 2, 2, "S")
        g.ring(8, tx0, 0, tx0 + 2, 2, "G")
        g.ring(9, tx0, 0, tx0 + 2, 2, "A")
    g.set(5, 0, 5, "C")              # 控制器：中厅地面中心
    return g, (5, 0, 5)

def g_planetary():
    g = Grid(11, 5, 11)
    g.ring(0, 0, 0, 10, 10, "S")
    g.cross(0, 5, 5, 4, "K")
    # 六调谐柱(六边形近似)
    for (x, z) in [(5, 0), (9, 2), (10, 7), (5, 10), (1, 7), (0, 2)]:
        g.col(x, z, 0, 1, "T")
        g.set(x, 2, z, "A")
    # 中央谐振井
    for y in (0, 1):
        g.ring(y, 4, 4, 6, 6, "T")
    g.ring(2, 4, 4, 6, 6, "A")
    g.set(5, 3, 5, "A")
    g.set(5, 4, 5, "G")
    g.set(5, 1, 5, "C")              # 控制器：井心
    return g, (5, 1, 5)

DESIGNS = {
    "resonance_tower": g_tower,
    "deep_electrolysis_array": g_array,
    "eco_dome": g_dome,
    "warp_anchor": g_anchor,
    "harmonic_cathedral": g_cathedral,
    "planetary_array": g_planetary,
}

def build():
    os.makedirs(POUT, exist_ok=True)
    for name, fn in DESIGNS.items():
        g, (cx, cy, cz) = fn()
        sx, sy, sz = g.s
        blocks = [{"x": x, "y": y, "z": z, "block": ("fieldworks:" + VARIANT_CONTROLLER[name] if ch == "C" else CH[ch])}
                  for (x, y, z), ch in sorted(g.c.items())]
        doc = {"size": [sx, sy, sz], "origin": "anchor_min_corner", "controller": [cx, cy, cz], "blocks": blocks}
        json.dump(doc, open(os.path.join(D, name + ".json"), "w", encoding="utf-8"), indent=1)
        # 材料清单
        from collections import Counter
        cnt = Counter(b["block"] for b in blocks)
        print(f"{name}: {sx}×{sy}×{sz}, {len(blocks)} 方块, 控制器@({cx},{cy},{cz})")
        for k, v in sorted(cnt.items(), key=lambda kv: -kv[1]):
            print(f"    {v:3d} × {k}")
        # patchouli multiblock 页片段(dense pattern 顶层→底层)
        layers = []
        for y in range(sy - 1, -1, -1):
            layer = []
            for z in range(sz - 1, -1, -1):
                row = ""
                for x in range(sx):
                    ch = g.c.get((x, y, z))
                    if ch is None:
                        row += " "
                    elif ch == "C":
                        row += "0"
                    else:
                        row += ch
                layer.append(row)
            layers.append(layer)
        mapping = {k: v for k, v in CH.items()}
        mapping["0"] = "fieldworks:" + VARIANT_CONTROLLER[name]
        frag = {"type": "fieldworks:multiblock", "name": f"book.fieldworks.e.{short(name)}.mb",
                "text": f"book.fieldworks.e.{short(name)}.mb_t",
                "multiblock": {"pattern": layers, "mapping": mapping, "symmetrical": False},
                "multiblock_variant": name,
                "enable_visualize": True, "custom_game": None}
        json.dump(frag, open(os.path.join(POUT, name + ".json"), "w", encoding="utf-8"), ensure_ascii=False, indent=1)
    print("✓ 巨物结构 v3 + patchouli 片段")

def short(name):
    return {"resonance_tower": "tower", "deep_electrolysis_array": "deep_array", "eco_dome": "dome",
            "warp_anchor": "anchor", "harmonic_cathedral": "cathedral", "planetary_array": "array"}[name]

if __name__ == "__main__":
    build()
