# fieldworks-samples 数据包样例（§19.2 验收用例 3）

把 `fieldworks-samples` 文件夹打进 zip 即可作为数据包放入 `world/datapacks/`：

- `data/fieldworks/field/*.json` — 覆盖任意内置场的单位/区间/同步策略（§3.2）
- `data/fieldworks/atmosphere/*.json` — 强制指定群系→大气映射（未知群系与模组群系通用）
- `data/fieldworks/recipes/*.json` — 新增/覆盖机器配方（type: fieldworks:machine）
- `assets/fieldworks/screens/<machine>.json` — 资源包换肤机器 GUI（§15.2 样式层）

第三方模组代码侧注册新场：`FieldRegistry.register(new FieldDefinition(...))` ——
探测器/场谱面板/配方 FieldProfile 立即识别，机器与配方零改动生效（§19.2 用例 1）。
