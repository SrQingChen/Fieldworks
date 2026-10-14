package com.srqingchen.fieldworks.api.field;

import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 场注册表(§19.1 api.field)。内置场由 {@link com.srqingchen.fieldworks.fieldengine.FWFields} 静态注册；
 * 第三方模组可在 mod 构造期调用 {@link #register} 注册新场；
 * 数据包可经 data/fieldworks/field/*.json 覆盖任意内置场的数值区间(§19.3)。
 *
 * <p>注册后的场立即出现在探测器、场谱面板、FieldProfile 条件列(§19.2 用例 1)，
 * 机器与配方对新场零改动生效。</p>
 */
public final class FieldRegistry {
    private static final Map<Identifier, FieldDefinition> FIELDS = new LinkedHashMap<>();
    private static final Map<Identifier, Integer> ORDINALS = new ConcurrentHashMap<>();
    private static final List<Identifier> ORDER = Collections.synchronizedList(new ArrayList<>());
    private static volatile boolean frozen = false;

    /** 注册新场。内置场注册后冻结；第三方注册仍允许(在 mod 构造期调用)。 */
    public static synchronized FieldDefinition register(FieldDefinition def) {
        if (FIELDS.containsKey(def.id())) {
            // 数据包/第三方覆盖：只允许修改数值区间等元数据，ordinal 不变
            FIELDS.put(def.id(), def);
            return def;
        }
        FIELDS.put(def.id(), def);
        ORDER.add(def.id());
        ORDINALS.put(def.id(), ORDER.size() - 1);
        return def;
    }

    public static FieldDefinition get(Identifier id) {
        return FIELDS.get(id);
    }

    public static int ordinal(Identifier id) {
        Integer i = ORDINALS.get(id);
        return i == null ? -1 : i;
    }

    public static Identifier byOrdinal(int ordinal) {
        if (ordinal < 0 || ordinal >= ORDER.size()) return null;
        return ORDER.get(ordinal);
    }

    public static int size() {
        return ORDER.size();
    }

    public static List<Identifier> ids() {
        return List.copyOf(ORDER);
    }

    public static Map<Identifier, FieldDefinition> all() {
        return Collections.unmodifiableMap(FIELDS);
    }

    /** 便捷读取：fieldworks 内部 ID。 */
    public static FieldDefinition fw(String path) {
        return get(Fieldworks.id(path));
    }

    static void freezeBuiltins() {
        frozen = true;
    }
}
