package com.srqingchen.fieldworks.atmosphere;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.atmosphere.AtmosphereRegistry;
import com.srqingchen.fieldworks.api.atmosphere.AtmosphereType;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;

import java.util.HashMap;
import java.util.Map;

/**
 * L0 静态层(§6.2)：群系 → 大气类型映射 + 四大场基线。
 * 内置映射覆盖原版全部 65 群系(26.1.2 已反编译核对，M0-T3)；
 * 未知(模组)群系走 {@link BiomeCensus} 标签普查归族(§4.5)；
 * 数据包可对任意群系 ID 强制指定(§19.3)。
 */
public final class BiomeAtmospheres {

    /** 数据包强制指定表(biome id → atmosphere id)。 */
    private static final Map<Identifier, Identifier> OVERRIDES = new HashMap<>();

    /** 普查归族结果缓存(biome id → atmosphere id)。 */
    private static final Map<Identifier, Identifier> CENSUS = new HashMap<>();

    // ── 原版群系映射(§4.2 主表 + 附录 A) ──
    private static final Map<String, Identifier> VANILLA = new HashMap<>();

    static {
        // 森林族 → 富氧(大概率，实例扰动造成差异)
        for (String b : new String[]{"forest", "flower_forest", "birch_forest", "old_growth_birch_forest",
                "meadow", "lush_caves", "warm_ocean", "deep_lukewarm_ocean", "cherry_grove", "windswept_forest"}) {
            VANILLA.put(b, AtmosphereRegistry.OXYGEN_RICH);
        }
        // 富沼(必然)
        VANILLA.put("swamp", AtmosphereRegistry.MARSH_GAS);
        VANILLA.put("mangrove_swamp", AtmosphereRegistry.MARSH_GAS);
        // 干热
        for (String b : new String[]{"desert", "savanna", "savanna_plateau", "windswept_savanna",
                "badlands", "eroded_badlands", "wooded_badlands", "stony_peaks"}) {
            VANILLA.put(b, AtmosphereRegistry.ARID_AIR);
        }
        // 寒燥(冰原事实上很干燥 §4.2)
        for (String b : new String[]{"snowy_plains", "ice_spikes", "snowy_taiga", "snowy_beach", "frozen_river",
                "frozen_ocean", "deep_frozen_ocean", "grove", "snowy_slopes", "jagged_peaks", "frozen_peaks"}) {
            VANILLA.put(b, AtmosphereRegistry.FRIGID_AIR);
        }
        // 盐雾(海洋族非暖水 + 沙滩石岸)
        for (String b : new String[]{"ocean", "deep_ocean", "cold_ocean", "deep_cold_ocean", "lukewarm_ocean",
                "beach", "stony_shore", "river"}) {
            VANILLA.put(b, AtmosphereRegistry.SALT_SPRAY);
        }
        // 菌孢
        VANILLA.put("mushroom_fields", AtmosphereRegistry.SPORE_AIR);
        VANILLA.put("dark_forest", AtmosphereRegistry.SPORE_AIR);
        VANILLA.put("pale_garden", AtmosphereRegistry.SPORE_AIR);
        // 下界
        VANILLA.put("nether_wastes", AtmosphereRegistry.SULFURIC_GAS);
        VANILLA.put("soul_sand_valley", AtmosphereRegistry.ASHEN_GAS);
        VANILLA.put("basalt_deltas", AtmosphereRegistry.BASALTIC_GAS);
        VANILLA.put("crimson_forest", AtmosphereRegistry.FUNGAL_GAS);
        VANILLA.put("warped_forest", AtmosphereRegistry.FUNGAL_GAS);
        // 末地
        for (String b : new String[]{"the_end", "end_highlands", "end_midlands", "small_end_islands", "end_barrens"}) {
            VANILLA.put(b, AtmosphereRegistry.TUNED_VOID);
        }
        // 其余主世界群系(平原族/丛林/针叶/风袭丘陵/荒漠口岸等) → 常规大气(默认基调)
    }

    private BiomeAtmospheres() {}

    public static void applyOverrides(Map<Identifier, Identifier> map, boolean replaceAll) {
        if (replaceAll) OVERRIDES.clear();
        OVERRIDES.putAll(map);
    }

    public static void putCensus(Identifier biome, Identifier atmosphere) {
        CENSUS.put(biome, atmosphere);
    }

    public static Identifier censusOf(Identifier biome) {
        return CENSUS.get(biome);
    }

    /** 群系 → 大气类型。优先级：数据包强制 > 普查结果 > 原版表 > 标签就近归族。 */
    public static Identifier atmosphereOf(Identifier biomeId, Holder<Biome> holder) {
        Identifier o = OVERRIDES.get(biomeId);
        if (o != null) return o;
        Identifier c = CENSUS.get(biomeId);
        if (c != null) return c;
        Identifier v = VANILLA.get(biomeId.getPath());
        if (v != null && biomeId.getNamespace().equals("minecraft")) return v;
        return familyOf(holder);
    }

    /** 标签就近归族(§4.5 规则 2 的标签快路径)。 */
    public static Identifier familyOf(Holder<Biome> holder) {
        if (holder.is(BiomeTags.IS_FOREST)) return AtmosphereRegistry.OXYGEN_RICH;
        if (holder.is(BiomeTags.IS_JUNGLE)) return AtmosphereRegistry.OXYGEN_RICH;
        if (holder.is(BiomeTags.IS_TAIGA)) return AtmosphereRegistry.OXYGEN_RICH;
        if (holder.is(BiomeTags.IS_OCEAN)) return AtmosphereRegistry.SALT_SPRAY;
        if (holder.is(BiomeTags.IS_RIVER) || holder.is(BiomeTags.IS_BEACH)) return AtmosphereRegistry.SALT_SPRAY;
        if (holder.is(BiomeTags.IS_BADLANDS) || holder.is(BiomeTags.IS_SAVANNA)) return AtmosphereRegistry.ARID_AIR;
        if (holder.is(BiomeTags.IS_MOUNTAIN) || holder.is(BiomeTags.IS_HILL)) return AtmosphereRegistry.AMBIENT_AIR;
        if (holder.is(BiomeTags.IS_NETHER)) return AtmosphereRegistry.SULFURIC_GAS;
        if (holder.is(BiomeTags.IS_END)) return AtmosphereRegistry.TUNED_VOID;
        // 湿度/温度线索
        Biome b = holder.value();
        if (b.hasPrecipitation()) {
            float t = b.getBaseTemperature();
            if (t < 0.15f) return AtmosphereRegistry.FRIGID_AIR;
            if (t > 1.5f) return AtmosphereRegistry.ARID_AIR;
        } else {
            return AtmosphereRegistry.ARID_AIR;
        }
        return AtmosphereRegistry.AMBIENT_AIR;
    }

    // ── 四大场基线 ──

    /** 群系基线温度(℃)：vanilla temp 0..2 → -20..70，下界维度基线由引擎再加。 */
    public static float baseTemperatureC(Holder<Biome> holder) {
        float t = holder.value().getBaseTemperature();
        return t * 45f - 20f;
    }

    /** 群系基线湿度(%RH)：downfall 0..1 → 15..90，寒燥/富沼族覆写(§4.2)。 */
    public static float baseHumidity(Holder<Biome> holder, Identifier atmosphereId) {
        if (atmosphereId.equals(AtmosphereRegistry.FRIGID_AIR)) return 18f;   // 冰原事实上很干燥
        if (atmosphereId.equals(AtmosphereRegistry.ARID_AIR)) return 14f;
        if (atmosphereId.equals(AtmosphereRegistry.MARSH_GAS)) return 92f;     // 湿 90+
        if (atmosphereId.equals(AtmosphereRegistry.SALT_SPRAY)) return 75f;
        if (atmosphereId.equals(AtmosphereRegistry.SPORE_AIR)) return 80f;
        if (atmosphereId.equals(AtmosphereRegistry.TUNED_VOID)) return 5f;
        if (atmosphereId.equals(AtmosphereRegistry.VOID_AIR)) return 0f;
        float downfall = holder.value().getModifiedClimateSettings().downfall();
        return 15f + downfall * 70f;
    }
}
