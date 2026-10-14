package com.srqingchen.fieldworks.api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.resources.Identifier;

import java.util.Optional;

/**
 * 单场条件(§7.1)。
 * ideal/tolerance/penalty/weight 表达"越近越快"的软条件；
 * min/max 仅对人工场类条件(ENVELOPE scope)构成硬门槛(§7.2 硬门槛三例外)。
 */
public record FieldCondition(
        Identifier field,
        Optional<Float> ideal,
        Optional<Float> min,
        Optional<Float> max,
        float tolerance,
        float penalty,
        float weight) {

    public static final Codec<FieldCondition> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.fieldOf("field").forGetter(FieldCondition::field),
            Codec.FLOAT.optionalFieldOf("ideal").forGetter(FieldCondition::ideal),
            Codec.FLOAT.optionalFieldOf("min").forGetter(FieldCondition::min),
            Codec.FLOAT.optionalFieldOf("max").forGetter(FieldCondition::max),
            Codec.FLOAT.optionalFieldOf("tolerance", 40f).forGetter(FieldCondition::tolerance),
            Codec.FLOAT.optionalFieldOf("penalty", 0.5f).forGetter(FieldCondition::penalty),
            Codec.FLOAT.optionalFieldOf("weight", 1f).forGetter(FieldCondition::weight)
    ).apply(inst, FieldCondition::new));

    public static FieldCondition idealOf(String field, float ideal, float tolerance, float penalty, float weight) {
        return new FieldCondition(Fieldworks.id(field), Optional.of(ideal), Optional.empty(), Optional.empty(),
                tolerance, penalty, weight);
    }

    public static FieldCondition minOf(String field, float min) {
        return new FieldCondition(Fieldworks.id(field), Optional.empty(), Optional.of(min), Optional.empty(),
                1f, 1f, 0f);
    }
}
