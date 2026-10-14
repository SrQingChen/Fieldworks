package com.srqingchen.fieldworks.api.field;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;

/**
 * 场定义(策划稿 §3.2)。一切影响配方效率与机器行为的环境量的统称。
 * 场是注册表对象：第三方经 {@link FieldRegistry#register} 注册新场后，
 * 机器与配方通过 ID 引用即可零改动生效(§19.2 验收用例 1)。
 *
 * @param id             fieldworks:harmonic
 * @param unit           单位符号："律" / "T" / "g" / "Pa" / "μSv/h" / "cp" / "%RH" / "℃" / ""
 * @param scope          采样作用域
 * @param naturalMin     自然存在区间(供探测器标尺)
 * @param naturalMax     自然存在区间上界
 * @param artificialMin  人工可营造区间(可空 = 该场不可人工营造)
 * @param artificialMax  人工区间上界
 * @param displayScale   显示刻度(真空场对数)
 * @param sync           客户端同步策略(§21.2 性能红线)
 */
public record FieldDefinition(
        Identifier id,
        String unit,
        FieldScope scope,
        float naturalMin,
        float naturalMax,
        Float artificialMin,
        Float artificialMax,
        DisplayScale displayScale,
        SyncPolicy sync) {

    public enum FieldScope implements StringRepresentable {
        GLOBAL, BIOME_INSTANCE, POSITION, ENVELOPE;

        @Override
        public String getSerializedName() { return name().toLowerCase(); }
    }

    public enum DisplayScale implements StringRepresentable {
        LINEAR, LOG;

        @Override
        public String getSerializedName() { return name().toLowerCase(); }
    }

    /** 场数值同步策略(§3.2)：性能第一。 */
    public enum SyncPolicy implements StringRepresentable {
        NEVER, GUI_ONLY, NEAR_PLAYER;

        @Override
        public String getSerializedName() { return name().toLowerCase(); }
    }

    private static final com.mojang.serialization.MapCodec<Float> ART_MIN = Codec.FLOAT.optionalFieldOf("artificial_min").xmap(o -> o.orElse(null), java.util.Optional::ofNullable);
    private static final com.mojang.serialization.MapCodec<Float> ART_MAX = Codec.FLOAT.optionalFieldOf("artificial_max").xmap(o -> o.orElse(null), java.util.Optional::ofNullable);

    public static final Codec<FieldDefinition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.fieldOf("id").forGetter(FieldDefinition::id),
            Codec.STRING.optionalFieldOf("unit", "").forGetter(FieldDefinition::unit),
            StringRepresentable.fromEnum(FieldScope::values).optionalFieldOf("scope", FieldScope.POSITION).forGetter(FieldDefinition::scope),
            Codec.FLOAT.optionalFieldOf("natural_min", 0f).forGetter(FieldDefinition::naturalMin),
            Codec.FLOAT.optionalFieldOf("natural_max", 100f).forGetter(FieldDefinition::naturalMax),
            ART_MIN.forGetter(FieldDefinition::artificialMin),
            ART_MAX.forGetter(FieldDefinition::artificialMax),
            StringRepresentable.fromEnum(DisplayScale::values).optionalFieldOf("display", DisplayScale.LINEAR).forGetter(FieldDefinition::displayScale),
            StringRepresentable.fromEnum(SyncPolicy::values).optionalFieldOf("sync", SyncPolicy.GUI_ONLY).forGetter(FieldDefinition::sync)
    ).apply(inst, FieldDefinition::new));

    public boolean artificial() {
        return artificialMin != null && artificialMax != null;
    }

    /** 对数刻度显示值(真空场等跨多个数量级的场)。 */
    public double displayValue(double raw) {
        if (displayScale == DisplayScale.LOG) {
            return raw <= 0 ? 0 : Math.log10(Math.max(raw, 1e-9)) + 9;
        }
        return raw;
    }
}
