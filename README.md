<div align="center">

<img src="docs/logo.png" alt="场域工艺 Fieldworks" width="220"/>

# 场域工艺 Fieldworks

**以"场"为核心世界规则的大型科技模组 —— 读懂大地，营造强场，驾驭巨物，调谐星球。**

[![Mod Version](https://img.shields.io/badge/version-0.4.0-1E9E96?style=for-the-badge&labelColor=0C6B66)]()
[![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62B47A?style=for-the-badge&labelColor=3E7350&logo=minecraft)]()
[![NeoForge](https://img.shields.io/badge/NeoForge-26.1.2.87-E8A33D?style=for-the-badge&labelColor=9A6B1E)]()
[![Java](https://img.shields.io/badge/Java-25-F89820?style=for-the-badge&labelColor=5382A1&logo=openjdk&logoColor=white)]()

[![License](https://img.shields.io/badge/代码%20MPL--2.0-蓝?style=flat-square&labelColor=2B4C7E)](LICENSE)
[![Curios](https://img.shields.io/badge/可选%20Curios-15.0-8E6FC9?style=flat-square&labelColor=5C4494)]()
[![Patchouli](https://img.shields.io/badge/可选%20Patchouli-26.1-7FD8D0?style=flat-square&labelColor=0C6B66)]()
[![JEI](https://img.shields.io/badge/联动%20JEI-29.43-D9A728?style=flat-square&labelColor=8A6A20)]()
[![Build](https://img.shields.io/badge/服务器冒烟-0%20ERROR-2FA84F?style=flat-square&labelColor=1E5C31)]()

*From four natural fields under your feet — to artificial strong fields, giant multiblocks, and planetary tuning.*

</div>

---

## ✨ 这个模组不一样在哪

大多数科技模组的变量在**机器里**；场域工艺的变量在**大地里**。

- 🌡️ **环境即配方参数** —— 温度 / 湿度 / 光照 / 磁场 / 引力 / 真空度实时参与每一条配方：同一台机器，换个选址就换一套效率（环境适配率 Fit%），雷暴是免费磁化窗口，满月之夜分选效率 +20%。
- 🏗️ **巨物多方块** —— 六座超大结构（谐振聚能塔 / 深井电解阵列 / 生态穹顶 / 曲率锚 / 谐律大教堂 / 行星调谐阵列）：**书内 3D 投影一键放置**、手持**木棍点控制器尽力搭建**（缺料自动清单）、成形即温和区块加载。
- 🔋 **FE 能量体系** —— 能量以 FE 计（内部 1J=1FE=1RF），点击机器 GUI 能量条即可在 **FE / RF / J** 间切换显示；发电机经**能量管道**供电、耗电机器自动受电。
- 🎒 **场学饰品（Curios）** —— 强人工场的产物可以随身携带：磁吸透镜、虚空闪现戒……直至终极的**超导谐律核心**：受击伤害按比例转为电容电量抵挡（完全抵挡连本次附带的状态效果一并免疫），实际耗电还会**叠加随机属性增幅**——层数无上限、每层独立计时。
- 🌊 **无限水泵** —— 浸入水源（≥2 面相邻）即仅耗电产水：低速 1 桶/秒、高速 3 桶/秒、**无限档**随管道吞吐放量、耗电指数上升。
- 🧰 **三族管道** —— 流体管道（气液通吃）/ 物品管道 / 能量管道：相连即工作，运载内容以颜色点亮管芯，越满越亮。

## 🚀 快速上手

1. 合成**探测杖**（铁粒+木棍）——潜行右键读取脚下四大场，首次合成附赠《场域学入门》手册；
2. 石头+玻璃合成**石制捕获器**，再合成**固体燃料蒸汽发电机**与**简易部件台**（3×3 输入格，复合配方）；
3. 鼓风熔炉富氧炼钢 → 电解分级 → 分馏三路气体，第一座气体工厂成型；
4. 顺着手册《场域学入门》九章 54 页走完从田野调查到行星调谐的完整旅程。

## 📦 版本亮点（v0.4.0）

- 管道三族重构：气/液管道合并为通用流体管道，新增物品管道与能量管道（PULL/PUSH 尊重六面 IO）
- 机器 GUI 四连修：数据接线（能量/储罐实时刷新）、附属部件垂直分布、IO 浮窗置顶、槽位像素对齐
- 部件台 3×3 + **计数配料**：`{"ingredient": "...", "count": 16}` 复合配方，JEI 显示 ×N 角标
- 无限水泵（三档位/水源判定/无限档指数耗电）
- 巨物核心方块贴图全重制（机器同语言三面贴图）
- 手册投影点击眼睛自动关书进入放置；木棍点击控制器尽力搭建
- 七件场学饰品 + 终极转电盾（Curios 软依赖）

## 🛠️ 构建与运行

> 首次构建前请将可选依赖 jar 放入对应位置（不入库）：Curios/Patchouli → `libs/`，FTB 系列与拼音搜索 → `FTB系列模组/`，Jade/JEI → 上级目录原名（见 `build.gradle`）。缺省时仅影响对应联动，核心源码编译仍需 `libs/` 两个 jar。

```bash
./gradlew build          # 产出 build/libs/fieldworks-0.4.0.jar
./gradlew runServer      # 无头服务器（冒烟已验证：0 ERROR / 1678 配方）
./gradlew runClient      # 客户端（需显示环境）
```

资产管线（`_texwork/` 内 Python 生成器为唯一事实源，规范顺序）：

```bash
python _texwork/gen_textures.py      # 机器底图
python _texwork/gen_controllers_v3.py# 巨物方块贴图
python _texwork/gen_data.py          # 配方/模型/标签/进度
python _texwork/gen_assets_v2.py     # multipart/激活变体/管道资产
python _texwork/gen_pipes_v3.py      # 管道贴图三族
python _texwork/gen_giants_v3.py     # 巨物结构 JSON
python _texwork/gen_atlas_v3.py      # GUI 图集
python _texwork/gen_textures_v3.py   # 物品贴图
python _texwork/gen_lang.py          # 双语 lang
python _texwork/gen_book.py          # 帕秋莉手册（最后跑）
```

## 🧩 核心系统

| 系统 | 实现位置 |
|---|---|
| 场引擎四层（静态/实例/全局/人工）+ 预算制 + 三级缓存 + 订阅推送 | `fieldengine/FieldEngine` |
| 16 场注册表 / 15 大气类型 / 植被指数 / 碳氛围场 | `fieldengine/` `atmosphere/` `carbon/` |
| 28 台工艺机器（spec 驱动）+ 升级芯 + 六面 IO + 休眠调度 | `machine/` |
| 18 配方族 + 计数配料 + 场档案 FieldProfile | `api/recipe/` |
| 三族管道（流体/物品/能量）+ 运载可视化 BER | `block/Pipe*` `client/render/` |
| 人工场五件套 + 谐律附魔 + 驯息场 | `envelope/` `harmonic/` `tame/` |
| 六巨物多方块 + 蓝图投影 + 木棍搭建 + 曲率锚迁移 | `giant/` `block/Giant*` `relocation/` |
| 场学饰品七件 + 超导谐律核心（转电盾/谐律增幅） | `compat/curios/` |
| 浅色机器 GUI（Fit 徽章/IO 浮窗/能量单位切换） | `client/gui/` |
| 帕秋莉手册《场域学入门》9 章 56 页双语 | `patchouli_books/manual` |

## 📜 指令

`/fieldworks sample` 场采样 · `/fieldworks carbon` 碳压 · `/fieldworks perf` 引擎分项 · `/fieldworks setcp <0-1000>`（OP）

## 📄 许可与致谢

- 代码：**MPL-2.0**；美术资产：CC BY-NC-SA 4.0
- 可选软依赖：[Curios](https://curios.readthedocs.io/) / [Patchouli](https://github.com/VazkiiMods/Patchouli) / JEI / FTB Quests / jecharacters —— 缺席时核心功能完整
- 灵感致谢：Mekanism · Thermal Series · Applied Energistics 2（管线/布局范式参考）

<div align="center">

**大地有谱，谱上建厂。**

</div>
