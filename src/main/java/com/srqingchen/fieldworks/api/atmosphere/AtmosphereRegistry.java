package com.srqingchen.fieldworks.api.atmosphere;

import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.resources.Identifier;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 大气注册表(§19.1 api.atmosphere)。内置 15 种(§4.2 表)；
 * 数据包 data/fieldworks/atmosphere/types/*.json 可新增/覆盖(§19.3)。
 */
public final class AtmosphereRegistry {
    private static final Map<Identifier, AtmosphereType> TYPES = new LinkedHashMap<>();

    // ── 内置 15 种大气 ID(§4.2 大气类型总表) ──
    public static final Identifier AMBIENT_AIR = Fieldworks.id("ambient_air");
    public static final Identifier OXYGEN_RICH = Fieldworks.id("oxygen_rich");
    public static final Identifier MARSH_GAS = Fieldworks.id("marsh_gas");
    public static final Identifier ARID_AIR = Fieldworks.id("arid_air");
    public static final Identifier FRIGID_AIR = Fieldworks.id("frigid_air");
    public static final Identifier SALT_SPRAY = Fieldworks.id("salt_spray");
    public static final Identifier SPORE_AIR = Fieldworks.id("spore_air");
    public static final Identifier DEEP_AIR = Fieldworks.id("deep_air");
    public static final Identifier VOID_AIR = Fieldworks.id("void_air");
    public static final Identifier SULFURIC_GAS = Fieldworks.id("sulfuric_gas");
    public static final Identifier ASHEN_GAS = Fieldworks.id("ashen_gas");
    public static final Identifier BASALTIC_GAS = Fieldworks.id("basaltic_gas");
    public static final Identifier SCORCH_GAS = Fieldworks.id("scorch_gas");
    public static final Identifier FUNGAL_GAS = Fieldworks.id("fungal_gas");
    public static final Identifier TUNED_VOID = Fieldworks.id("tuned_void");

    public static synchronized AtmosphereType register(AtmosphereType type) {
        TYPES.put(type.id(), type);
        return type;
    }

    public static AtmosphereType get(Identifier id) {
        return TYPES.get(id);
    }

    public static AtmosphereType ambient() {
        return TYPES.get(AMBIENT_AIR);
    }

    public static Map<Identifier, AtmosphereType> all() {
        return Collections.unmodifiableMap(TYPES);
    }

    /** 有序索引(菜单数据槽用 int 同步大气类型，客户端按索引还原)。 */
    public static int indexOf(Identifier id) {
        int i = 0;
        for (Identifier k : TYPES.keySet()) {
            if (k.equals(id)) return i;
            i++;
        }
        return -1;
    }

    /** 按有序索引取大气类型(越界返回 null)。 */
    public static AtmosphereType byIndex(int index) {
        if (index < 0) return null;
        int i = 0;
        for (AtmosphereType t : TYPES.values()) {
            if (i++ == index) return t;
        }
        return null;
    }

    static {
        register(new AtmosphereType(AMBIENT_AIR, new TraitVector(21, 0, 79, 0, 0, 100), "atmosphere.fieldworks.ambient_air.flavor"));
        register(new AtmosphereType(OXYGEN_RICH, new TraitVector(30, 0, 70, 0, 0, 100), "atmosphere.fieldworks.oxygen_rich.flavor"));
        register(new AtmosphereType(MARSH_GAS, new TraitVector(16, 16, 75, 0, 0, 100), "atmosphere.fieldworks.marsh_gas.flavor"));
        register(new AtmosphereType(ARID_AIR, new TraitVector(19, 0, 81, 0, 0, 98), "atmosphere.fieldworks.arid_air.flavor"));
        register(new AtmosphereType(FRIGID_AIR, new TraitVector(22, 0, 78, 0, 0, 103), "atmosphere.fieldworks.frigid_air.flavor"));
        register(new AtmosphereType(SALT_SPRAY, new TraitVector(20, 0, 77, 5, 0, 101), "atmosphere.fieldworks.salt_spray.flavor"));
        register(new AtmosphereType(SPORE_AIR, new TraitVector(18, 0, 82, 1, 0, 100), "atmosphere.fieldworks.spore_air.flavor"));
        register(new AtmosphereType(DEEP_AIR, new TraitVector(14, 0, 85, 0, 1, 105), "atmosphere.fieldworks.deep_air.flavor"));
        register(new AtmosphereType(VOID_AIR, new TraitVector(0, 0, 10, 0, 92, 40), "atmosphere.fieldworks.void_air.flavor"));
        register(new AtmosphereType(SULFURIC_GAS, new TraitVector(2, 15, 50, 40, 0, 108), "atmosphere.fieldworks.sulfuric_gas.flavor"));
        register(new AtmosphereType(ASHEN_GAS, new TraitVector(3, 0, 87, 8, 2, 100), "atmosphere.fieldworks.ashen_gas.flavor"));
        register(new AtmosphereType(BASALTIC_GAS, new TraitVector(2, 0, 88, 4, 0, 112), "atmosphere.fieldworks.basaltic_gas.flavor"));
        register(new AtmosphereType(SCORCH_GAS, new TraitVector(1, 12, 80, 2, 0, 96), "atmosphere.fieldworks.scorch_gas.flavor"));
        register(new AtmosphereType(FUNGAL_GAS, new TraitVector(6, 8, 86, 0, 0, 102), "atmosphere.fieldworks.fungal_gas.flavor"));
        register(new AtmosphereType(TUNED_VOID, new TraitVector(0, 0, 40, 0, 50, 70), "atmosphere.fieldworks.tuned_void.flavor"));
    }

    private AtmosphereRegistry() {}
}
