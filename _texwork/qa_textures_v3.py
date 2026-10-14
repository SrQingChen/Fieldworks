#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""贴图 QA(261012规划 T5.3)：物品贴图结构化质检(261014 起动态数量)。"""
import os, sys
import numpy as np
from PIL import Image

ROOT = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(ROOT, "..", "src", "main", "resources", "assets", "fieldworks", "textures", "item")

INGOTS = {"aluminum_ingot","aluminum_brass_ingot","flux_alloy_ingot","invar_ingot","monel_ingot",
          "permalloy_ingot","pure_iron_ingot","stainless_steel_ingot","steel_ingot","superconductor_alloy",
          "raw_bauxite","raw_monazite"}

fails = []
files = sorted(f for f in os.listdir(OUT) if f.endswith(".png"))
print(f"检查 {len(files)} 张")
for fn in files:
    im = Image.open(os.path.join(OUT, fn))
    name = fn[:-4]
    if im.size != (64, 64) or im.mode != "RGBA":
        fails.append(f"{name}: {im.size}{im.mode} 非 64×64 RGBA")
        continue
    a = np.array(im).astype(np.float32)
    alpha = a[..., 3]
    cov = (alpha > 40).mean()
    if cov < 0.10 or cov > 0.80:
        fails.append(f"{name}: 主体覆盖率 {cov:.0%} 越界")
    vis = a[alpha > 40][:, :3]
    uniq = len(np.unique((vis[:, :3] // 4).astype(int), axis=0)) * 4
    if uniq < 48:
        fails.append(f"{name}: 颜色数过少({uniq})")
    # 平坦检查(排除几乎单色)
    lum = vis.mean(axis=1)
    if lum.std() < 8:
        fails.append(f"{name}: 亮度方差过低({lum.std():.1f}) 平涂嫌疑")
    # 立体感: 亮度分层(阴影/中间/高光三带都要有)
    p10, p90 = np.percentile(lum, 10), np.percentile(lum, 90)
    if p90 - p10 < 45:
        fails.append(f"{name}: 明暗对比不足(ΔL={p90-p10:.0f}) 立体感弱")

ok = len(files) - len(fails)
for f in fails:
    print("✗", f)
print(f"贴图 QA: {'✗ FAIL' if fails else '✓ PASS'} ({ok}/{len(files)})")
sys.exit(1 if fails else 0)
