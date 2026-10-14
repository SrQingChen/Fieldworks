package com.srqingchen.fieldworks.api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.atmosphere.TraitVector;
import com.srqingchen.fieldworks.api.field.FieldDefinition;
import com.srqingchen.fieldworks.api.field.FieldRegistry;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import com.srqingchen.fieldworks.fieldengine.FieldSnapshot;
import com.srqingchen.fieldworks.fieldengine.FWFields;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Optional;

/**
 * 场档案(§7.1)：配方的"环境需求"声明。数据驱动，JEI 可显示。
 *
 * <p>效率公式(§7.2，D4 软门槛)：
 * 单项适配 f_i = max(0, 1 − |v − ideal| / tolerance × penalty)
 * 环境适配率 Fit = clamp( Π f_i^weight_i , efficiency_floor , 1.0 )
 * 执行速度 ×= Fit；能耗 ×= (1 + (1−Fit) × 0.5)。</p>
 */
public record FieldProfile(
        Optional<Identifier> atmosphere,
        Optional<Float> minOx,
        List<FieldCondition> fields,
        float efficiencyFloor) {

    public static final FieldProfile EMPTY = new FieldProfile(Optional.empty(), Optional.empty(), List.of(), 0.6f);

    public static final Codec<FieldProfile> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Identifier.CODEC.optionalFieldOf("require").forGetter(FieldProfile::atmosphere),
            Codec.FLOAT.optionalFieldOf("min_ox").forGetter(FieldProfile::minOx),
            Codec.list(FieldCondition.CODEC).optionalFieldOf("fields", List.of()).forGetter(FieldProfile::fields),
            Codec.FLOAT.optionalFieldOf("efficiency_floor", 0.6f).forGetter(FieldProfile::efficiencyFloor)
    ).apply(inst, FieldProfile::new));

    /** 适配结果：Fit ∈ [floor,1]；gate=false 表示硬门槛未达(仅人工场类)。 */
    public record FitResult(float fit, boolean gate, String blockedReason) {
        public static final FitResult FULL = new FitResult(1f, true, "");
    }

    /** 在指定位置计算环境适配率(§7.2)。 */
    public FitResult evaluate(ServerLevel level, BlockPos pos) {
        FieldSnapshot snap = FieldEngine.snapshot(level, pos);
        // 气氛类型硬门槛(原料性要求，§7.2)
        if (atmosphere.isPresent() && !snap.atmosphereType().equals(atmosphere.get())) {
            return new FitResult(efficiencyFloor, false, "atmosphere");
        }
        TraitVector traits = snap.traits();
        double product = 1.0;
        boolean gate = true;
        String reason = "";
        // 组分条件(软化为乘子，但低于 0 时归零——原料不足)
        if (minOx.isPresent() && traits.ox() < minOx.get() - 0.001f) {
            return new FitResult(efficiencyFloor, false, "ox");
        }
        for (FieldCondition c : fields) {
            FieldDefinition def = FieldRegistry.get(c.field());
            if (def == null) continue;
            float v = snap.value(c.field());
            // 硬门槛：仅 ENVELOPE scope(人工场)的 min/max(§7.2)
            if (def.scope() == FieldDefinition.FieldScope.ENVELOPE) {
                if (c.min().isPresent() && v < c.min().get() - 0.001f) {
                    gate = false;
                    reason = c.field().getPath();
                    continue;
                }
                if (c.max().isPresent() && v > c.max().get() + 0.001f) {
                    gate = false;
                    reason = c.field().getPath();
                    continue;
                }
            }
            if (c.ideal().isPresent()) {
                float ideal = c.ideal().get();
                float tol = Math.max(0.001f, c.tolerance());
                float f = Math.max(0, 1f - Math.abs(v - ideal) / tol * c.penalty());
                product *= Math.pow(f, c.weight());
            }
        }
        float fit = (float) Math.max(efficiencyFloor, Math.min(1.0, product));
        return new FitResult(fit, gate, reason);
    }
}
