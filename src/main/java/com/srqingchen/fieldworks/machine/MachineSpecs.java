package com.srqingchen.fieldworks.machine;

import com.srqingchen.fieldworks.machine.MachineSpec.Family;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.srqingchen.fieldworks.machine.MachineSpec.Mode.*;

/**
 * 全部工艺机器规格登记(§10.3 MVP 47 台口径：spec 驱动的通用机器 + custom BE 机器并列)。
 * 数值锚点：P0 蒸汽发电机 16J/t、部件台每件 ≤200J(§8.3)。
 */
public final class MachineSpecs {
    private static final Map<Identifier, MachineSpec> ALL = new LinkedHashMap<>();

    // ═══════ P0 ═══════
    public static final MachineSpec STEAM_GENERATOR = gen("steam_generator", 1, Family.ENERGY, 20_000, 16, 1, 1, 1, 2000);
    public static final MachineSpec PART_BENCH = proc("part_bench", 1, Family.MATERIAL, 10_000, 64, 9, 1, 0, 0);
    public static final MachineSpec ELECTROLYZER_1 = proc("electrolyzer_1", 1, Family.FIELD, 16_000, 32, 0, 0, 2, 4000);
    public static final MachineSpec BLAST_FURNACE = proc("blast_furnace", 1, Family.ENERGY, 16_000, 48, 2, 1, 0, 0);
    // 无限水泵(261014 T7)：前中期，仅耗电产水，三档位
    public static final MachineSpec INFINITE_WATER_PUMP = pump("infinite_water_pump", 1, Family.ENVIRONMENT,
            40_000, 512, 1, 8000);

    // ═══════ P1 ═══════
    public static final MachineSpec COMPRESSOR = proc("compressor", 1, Family.MATERIAL, 24_000, 64, 1, 1, 1, 4000);
    public static final MachineSpec DISTILLATION_TOWER = proc("distillation_tower", 2, Family.MATERIAL, 48_000, 96, 0, 0, 2, 8000);
    public static final MachineSpec GAS_TANK_1 = stor("gas_tank_1", 1, Family.CONVENIENCE, 1, 1000);
    public static final MachineSpec GAS_TANK_4 = stor("gas_tank_4", 2, Family.CONVENIENCE, 1, 4000);
    public static final MachineSpec GAS_TANK_16 = stor("gas_tank_16", 3, Family.CONVENIENCE, 1, 16000);
    public static final MachineSpec BLOWER = proc("blower", 1, Family.ENVIRONMENT, 16_000, 32, 0, 0, 1, 2000);
    public static final MachineSpec HYDROGEN_SHAFT_FURNACE = proc("hydrogen_shaft_furnace", 2, Family.ENERGY, 48_000, 128, 1, 1, 1, 4000);
    public static final MachineSpec STEAM_TURBINE = gen2("steam_turbine", 2, Family.ENERGY, 64_000, 96, 0, 0, 1, 8000);
    public static final MachineSpec ASSEMBLER = proc("assembler", 2, Family.MATERIAL, 48_000, 96, 3, 1, 0, 0);
    public static final MachineSpec CONDENSER = proc("condenser", 1, Family.ENVIRONMENT, 24_000, 48, 0, 0, 1, 4000);
    public static final MachineSpec RECYCLER = proc("recycler", 1, Family.MATERIAL, 24_000, 48, 1, 1, 0, 0);

    // ═══════ P2 ═══════
    public static final MachineSpec MAGNETIZER = proc("magnetizer", 2, Family.FIELD, 48_000, 96, 1, 1, 0, 0);
    public static final MachineSpec MAGNETIC_SEPARATOR = proc("magnetic_separator", 2, Family.FIELD, 48_000, 96, 1, 2, 0, 0);
    public static final MachineSpec GRAVITY_SEPARATOR = proc("gravity_separator", 2, Family.FIELD, 48_000, 96, 1, 2, 0, 0);
    public static final MachineSpec DEEP_DRILL = proc("deep_drill", 2, Family.MATERIAL, 64_000, 128, 1, 1, 0, 0);
    public static final MachineSpec NEGATIVE_PRESSURE_CATCHER = proc("negative_pressure_catcher", 2, Family.FIELD, 48_000, 96, 0, 0, 2, 4000);
    public static final MachineSpec IRRADIATION_CHAMBER = proc("irradiation_chamber", 2, Family.FIELD, 64_000, 128, 1, 1, 0, 0);
    public static final MachineSpec HEAT_EXCHANGER = proc("heat_exchanger", 2, Family.ENERGY, 48_000, 96, 0, 0, 1, 4000);

    // ═══════ P3 ═══════
    public static final MachineSpec CARBON_CAPTURER = proc("carbon_capturer", 2, Family.ENVIRONMENT, 48_000, 96, 0, 1, 0, 0);
    public static final MachineSpec CORROSION_TRAP = proc("corrosion_trap", 2, Family.ENVIRONMENT, 48_000, 96, 0, 1, 1, 4000);
    public static final MachineSpec ELECTROLYZER_2 = proc("electrolyzer_2", 2, Family.FIELD, 48_000, 96, 0, 0, 3, 6000);
    public static final MachineSpec ELECTROLYZER_3 = proc("electrolyzer_3", 3, Family.FIELD, 96_000, 192, 0, 0, 3, 8000);
    public static final MachineSpec PLASMA_ELECTROLYSIS = proc("plasma_electrolysis_matrix", 5, Family.FIELD, 256_000, 512, 0, 0, 3, 16000);

    private static MachineSpec proc(String path, int tier, Family f, int cap, int maxIn, int in, int out, int tanks, int tankCap) {
        MachineSpec s = MachineSpec.processor(path, tier, f, cap, maxIn, in, out, tanks, tankCap);
        ALL.put(s.id(), s);
        return s;
    }

    private static MachineSpec gen(String path, int tier, Family f, int cap, int maxOut, int in, int out, int tanks, int tankCap) {
        MachineSpec s = new MachineSpec(com.srqingchen.fieldworks.Fieldworks.id(path), tier, f, cap, maxOut, in, out, tanks, tankCap, GENERATOR);
        ALL.put(s.id(), s);
        return s;
    }

    private static MachineSpec stor(String path, int tier, Family f, int tanks, int tankCap) {
        MachineSpec s = new MachineSpec(com.srqingchen.fieldworks.Fieldworks.id(path), tier, f, 0, 0, 0, 0, tanks, tankCap, STORAGE);
        ALL.put(s.id(), s);
        return s;
    }

    private static MachineSpec gen2(String path, int tier, Family f, int cap, int maxOut, int in, int out, int tanks, int tankCap) {
        MachineSpec s = new MachineSpec(com.srqingchen.fieldworks.Fieldworks.id(path), tier, f, cap, maxOut, in, out, tanks, tankCap, FLUID_BURNER);
        ALL.put(s.id(), s);
        return s;
    }

    private static MachineSpec pump(String path, int tier, Family f, int cap, int maxIn, int tanks, int tankCap) {
        MachineSpec s = new MachineSpec(com.srqingchen.fieldworks.Fieldworks.id(path), tier, f, cap, maxIn, 0, 0, tanks, tankCap, PUMP);
        ALL.put(s.id(), s);
        return s;
    }

    /** 追加 custom BE 机器的 spec(仅作展示/统一索引)。 */
    public static MachineSpec custom(String path, int tier, Family f) {
        MachineSpec s = new MachineSpec(com.srqingchen.fieldworks.Fieldworks.id(path), tier, f, 0, 0, 0, 0, 0, 0, PROCESSOR);
        ALL.putIfAbsent(s.id(), s);
        return s;
    }

    public static MachineSpec get(Identifier id) {
        return ALL.get(id);
    }

    public static Collection<MachineSpec> all() {
        return ALL.values();
    }

    private MachineSpecs() {}
}
