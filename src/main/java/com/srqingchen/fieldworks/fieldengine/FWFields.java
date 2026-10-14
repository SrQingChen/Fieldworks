package com.srqingchen.fieldworks.fieldengine;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.field.FieldDefinition;
import com.srqingchen.fieldworks.api.field.FieldRegistry;
import net.minecraft.resources.Identifier;

/**
 * 内置场注册表条目(§3.1 分类学:五类十六场 + 扩展位)。
 * ordinal 顺序即快照数组下标，注册顺序不可变更。
 */
public final class FWFields {
    // ── 自然场 · 开局四 ──
    public static final Identifier ATMOSPHERE = Fieldworks.id("atmosphere");
    public static final Identifier LIGHT = Fieldworks.id("light");
    public static final Identifier TEMPERATURE = Fieldworks.id("temperature");
    public static final Identifier HUMIDITY = Fieldworks.id("humidity");
    // ── 自然场 · 进阶 ──
    public static final Identifier MAGNETIC = Fieldworks.id("magnetic");
    public static final Identifier GRAVITY = Fieldworks.id("gravity");
    public static final Identifier VACUUM = Fieldworks.id("vacuum");
    public static final Identifier RADIATION = Fieldworks.id("radiation");
    // ── 全局场 ──
    public static final Identifier CARBON = Fieldworks.id("carbon");
    // ── 人工场 ──
    public static final Identifier EM_FIELD = Fieldworks.id("em_field");
    public static final Identifier GRAV_FIELD = Fieldworks.id("grav_field");
    public static final Identifier VAC_FIELD = Fieldworks.id("vac_field");
    public static final Identifier RAD_FIELD = Fieldworks.id("rad_field");
    // ── 虚拟场 ──
    public static final Identifier HARMONIC = Fieldworks.id("harmonic");
    public static final Identifier TAME_FIELD = Fieldworks.id("tame_field");
    public static final Identifier WARP = Fieldworks.id("warp");

    static {
        //                 id            unit    scope           natMin natMax  artMin artMax scale  sync
        FieldRegistry.register(new FieldDefinition(ATMOSPHERE, "", FieldDefinition.FieldScope.BIOME_INSTANCE, 0, 100, null, null, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(LIGHT, "lv", FieldDefinition.FieldScope.POSITION, 0, 15, 0f, 15f, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(TEMPERATURE, "\u2103", FieldDefinition.FieldScope.POSITION, -40, 120, -60f, 200f, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(HUMIDITY, "%RH", FieldDefinition.FieldScope.BIOME_INSTANCE, 0, 100, 0f, 100f, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(MAGNETIC, "T", FieldDefinition.FieldScope.POSITION, 0, 0.5f, 0.1f, 100f, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(GRAVITY, "g", FieldDefinition.FieldScope.POSITION, 0.8f, 1.2f, 0f, 50f, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(VACUUM, "Pa", FieldDefinition.FieldScope.POSITION, 1e-4f, 1.01e5f, 1e-9f, 1e5f, FieldDefinition.DisplayScale.LOG, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(RADIATION, "\u03bcSv/h", FieldDefinition.FieldScope.POSITION, 0, 20, 1f, 1e6f, FieldDefinition.DisplayScale.LOG, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(CARBON, "cp", FieldDefinition.FieldScope.GLOBAL, 0, 1000, null, null, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(EM_FIELD, "T", FieldDefinition.FieldScope.ENVELOPE, 0, 0.5f, 0.1f, 100f, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(GRAV_FIELD, "g", FieldDefinition.FieldScope.ENVELOPE, 0, 0, 0f, 50f, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(VAC_FIELD, "Pa", FieldDefinition.FieldScope.ENVELOPE, 1.01e5f, 1.01e5f, 1e-9f, 1e5f, FieldDefinition.DisplayScale.LOG, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(RAD_FIELD, "\u03bcSv/h", FieldDefinition.FieldScope.ENVELOPE, 0, 0, 1f, 1e6f, FieldDefinition.DisplayScale.LOG, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(HARMONIC, "\u5f8b", FieldDefinition.FieldScope.ENVELOPE, 0, 0, 1f, 12f, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(TAME_FIELD, "\u606f", FieldDefinition.FieldScope.ENVELOPE, 0, 0, 1f, 100f, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
        FieldRegistry.register(new FieldDefinition(WARP, "\u66f2\u7387\u5ea6", FieldDefinition.FieldScope.ENVELOPE, 0, 0, 1f, 9f, FieldDefinition.DisplayScale.LINEAR, FieldDefinition.SyncPolicy.GUI_ONLY));
    }

    public static void init() {
        // 类加载即完成注册；主类构造期显式触碰一次保证顺序。
    }

    private FWFields() {}
}
