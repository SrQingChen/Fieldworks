package com.srqingchen.fieldworks.api.atmosphere;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * 特征值六元组(§4.2)。全部 0—100，压强除外(常规 100)。
 * OX 氧势 / FG 燃势 / IN 惰势 / CR 蚀势 / VD 虚势 / PR 压强。
 */
public record TraitVector(float ox, float fg, float in, float cr, float vd, float pr) {

    public static final TraitVector AMBIENT = new TraitVector(21, 0, 79, 0, 0, 100);

    public static final Codec<TraitVector> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.FLOAT.optionalFieldOf("ox", 21f).forGetter(TraitVector::ox),
            Codec.FLOAT.optionalFieldOf("fg", 0f).forGetter(TraitVector::fg),
            Codec.FLOAT.optionalFieldOf("in", 79f).forGetter(TraitVector::in),
            Codec.FLOAT.optionalFieldOf("cr", 0f).forGetter(TraitVector::cr),
            Codec.FLOAT.optionalFieldOf("vd", 0f).forGetter(TraitVector::vd),
            Codec.FLOAT.optionalFieldOf("pr", 100f).forGetter(TraitVector::pr)
    ).apply(inst, TraitVector::new));

    public float get(int index) {
        return switch (index) {
            case 0 -> ox;
            case 1 -> fg;
            case 2 -> in;
            case 3 -> cr;
            case 4 -> vd;
            case 5 -> pr;
            default -> 0;
        };
    }

    public TraitVector with(int index, float value) {
        return new TraitVector(
                index == 0 ? value : ox,
                index == 1 ? value : fg,
                index == 2 ? value : in,
                index == 3 ? value : cr,
                index == 4 ? value : vd,
                index == 5 ? value : pr);
    }

    public TraitVector lerp(TraitVector other, float t) {
        return new TraitVector(
                ox + (other.ox - ox) * t,
                fg + (other.fg - fg) * t,
                in + (other.in - in) * t,
                cr + (other.cr - cr) * t,
                vd + (other.vd - vd) * t,
                pr + (other.pr - pr) * t);
    }

    @Override
    public String toString() {
        return "OX%.0f/FG%.0f/IN%.0f/CR%.0f/VD%.0f/PR%.0f".formatted(ox, fg, in, cr, vd, pr);
    }
}
