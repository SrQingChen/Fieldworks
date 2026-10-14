package com.srqingchen.fieldworks.api.recipe;

import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 机器配方族(§7.1，261013 规划 T2，对标 AE2 AERecipeTypes)：
 * 每个配方族一个注册类型——"都是电解配方属于一个配方族"口径。
 * 电解族 4 台机器(Ⅰ/Ⅱ/Ⅲ/等离子矩阵)共用 {@code fieldworks:electrolysis}，
 * 其余工艺机器各自成族；族内多机仍靠配方 machines 白名单分级。
 *
 * <p>此表是 机器id → 配方族 的唯一映射，与数据包生成器(gen_data.py FAMILIES)
 * 镜像——修改必须两侧同步(qa_assets.py 比对)。</p>
 */
public final class MachineRecipeFamily {
    public final String path;
    /** 成员机器(按阶次，首元素=主机器/JEI 图标)。 */
    public final List<Identifier> machines;

    private MachineRecipeFamily(String path, String... machinePaths) {
        this.path = path;
        this.machines = List.of(machinePaths).stream()
                .map(p -> (Identifier) Fieldworks.id(p)).toList();
    }

    public Identifier id() {
        return Fieldworks.id(path);
    }

    /** JEI 类目标题键。 */
    public String titleKey() {
        return "jei.fieldworks." + path + ".title";
    }

    /** 主机器(最低阶，JEI 图标/催化剂)。 */
    public Identifier primary() {
        return machines.getFirst();
    }

    public RecipeType<MachineRecipe> type() {
        return com.srqingchen.fieldworks.registry.FWRecipes.typeOf(this);
    }

    public RecipeSerializer<MachineRecipe> serializer() {
        return com.srqingchen.fieldworks.registry.FWRecipes.serializerOf(this);
    }

    public boolean contains(Identifier machineId) {
        return machines.contains(machineId);
    }

    // ── 全部配方族(18) ──

    private static final Map<Identifier, MachineRecipeFamily> BY_MACHINE = new LinkedHashMap<>();
    private static final Map<Identifier, MachineRecipeFamily> BY_ID = new LinkedHashMap<>();

    public static final MachineRecipeFamily ELECTROLYSIS = reg("electrolysis",
            "electrolyzer_1", "electrolyzer_2", "electrolyzer_3", "plasma_electrolysis_matrix");
    public static final MachineRecipeFamily PART_CRAFTING = reg("part_crafting", "part_bench");
    public static final MachineRecipeFamily BLAST_SMELTING = reg("blast_smelting", "blast_furnace");
    public static final MachineRecipeFamily COMPRESSING = reg("compressing", "compressor");
    public static final MachineRecipeFamily DISTILLING = reg("distilling", "distillation_tower");
    public static final MachineRecipeFamily CONDENSING = reg("condensing", "condenser");
    public static final MachineRecipeFamily ASSEMBLING = reg("assembling", "assembler");
    public static final MachineRecipeFamily RECYCLING = reg("recycling", "recycler");
    public static final MachineRecipeFamily MAGNETIZING = reg("magnetizing", "magnetizer");
    public static final MachineRecipeFamily MAGNETIC_SEPARATING = reg("magnetic_separating", "magnetic_separator");
    public static final MachineRecipeFamily GRAVITY_SEPARATING = reg("gravity_separating", "gravity_separator");
    public static final MachineRecipeFamily DEEP_DRILLING = reg("deep_drilling", "deep_drill");
    public static final MachineRecipeFamily VACUUM_CATCHING = reg("vacuum_catching", "negative_pressure_catcher");
    public static final MachineRecipeFamily IRRADIATING = reg("irradiating", "irradiation_chamber");
    public static final MachineRecipeFamily HEAT_EXCHANGING = reg("heat_exchanging", "heat_exchanger");
    public static final MachineRecipeFamily SHAFT_SMELTING = reg("shaft_smelting", "hydrogen_shaft_furnace");
    public static final MachineRecipeFamily CORROSION_TRAPPING = reg("corrosion_trapping", "corrosion_trap");
    public static final MachineRecipeFamily CARBON_CAPTURING = reg("carbon_capturing", "carbon_capturer");

    private static MachineRecipeFamily reg(String path, String... machines) {
        var f = new MachineRecipeFamily(path, machines);
        BY_ID.put(f.id(), f);
        for (Identifier m : f.machines) {
            var prev = BY_MACHINE.putIfAbsent(m, f);
            if (prev != null) {
                throw new IllegalStateException("机器 " + m + " 同时属于配方族 " + prev.path + " 与 " + path);
            }
        }
        return f;
    }

    /** 机器 id → 配方族；无配方机器(发电机/储罐等)返回 null。 */
    public static MachineRecipeFamily fromMachine(Identifier machineId) {
        return BY_MACHINE.get(machineId);
    }

    public static MachineRecipeFamily fromId(Identifier id) {
        return BY_ID.get(id);
    }

    public static Collection<MachineRecipeFamily> all() {
        return BY_ID.values();
    }
}
