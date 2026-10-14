package com.srqingchen.fieldworks.envelope;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.atmosphere.TraitVector;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 人工场包络管理(§6.5 L3 / §11)：框架成形时登记 AABB 包络(空间索引为
 * "框架数量少"的线性扫描——§6.5 R-tree 的等价简化)，事件驱动增删，零轮询。
 *
 * <p>包络值合成规则(§11)：多输入器同场型 → 耦合增强表叠加；异场型 → 多场并置。
 * 场引擎在采样时以 override 挂钩查询本表。</p>
 */
public final class EnvelopeManager {
    /** 已成形包络。 */
    public static final class Envelope {
        public final ResourceKey dim;
        public final AABB box;
        public final long framePos;         // 框架锚点(去重/失效)
        public final Identifier field;      // fieldworks:em_field …
        public float strength;              // 当前场强(受失谐跌落)
        public float configuredStrength;    // 成形时标定强度
        public boolean constrained;         // 约束器在场
        public boolean stabilized;          // 稳定器在场(上限 +20%，§11.1)
        /** 气氛包络(生态穹顶四大场全控，§13.2)：null 表示非气氛包络。 */
        @Nullable
        public final TraitVector domeTraits;
        @Nullable
        public final Identifier domeAtmosphere;
        /** 泄露(§11.4)：无约束器时边界 4 格邻域获得 0.5%/日等效外溢。 */
        public boolean leaking;

        public Envelope(ResourceKey dim, AABB box, long framePos, Identifier field, float strength,
                 boolean constrained, boolean stabilized,
                 @Nullable TraitVector domeTraits, @Nullable Identifier domeAtmosphere) {
            this.dim = dim;
            this.box = box;
            this.framePos = framePos;
            this.field = field;
            this.configuredStrength = strength;
            this.strength = strength;
            this.constrained = constrained;
            this.stabilized = stabilized;
            this.domeTraits = domeTraits;
            this.domeAtmosphere = domeAtmosphere;
            this.leaking = !constrained;
        }
    }

    public record ResourceKey(String dim) {}

    private static final List<Envelope> ENVELOPES = new ObjectArrayList<>();

    public static void register(ServerLevel level, Envelope env) {
        ENVELOPES.add(env);
        invalidate(level, env);
        Fieldworks.LOGGER.info("[Fieldworks] 包络成形：{} @ {} 强度 {}", env.field.getPath(), env.box, env.strength);
    }

    public static boolean remove(ServerLevel level, long framePos) {
        boolean removed = ENVELOPES.removeIf(e -> e.framePos == framePos);
        if (removed) invalidateAll(level);
        return removed;
    }

    public static void invalidateAll(ServerLevel level) {
        FieldEngine.invalidateAll(level);
    }

    private static void invalidate(ServerLevel level, Envelope env) {
        int minX = (int) env.box.minX >> 4, maxX = (int) env.box.maxX >> 4;
        int minZ = (int) env.box.minZ >> 4, maxZ = (int) env.box.maxZ >> 4;
        for (int cx = minX; cx <= maxX; cx++) {
            for (int cz = minZ; cz <= maxZ; cz++) {
                FieldEngine.invalidate(level, new ChunkPos(cx, cz));
            }
        }
    }

    /** 查询位置最强包络值(NaN = 无覆盖)。 */
    public static float queryValue(ServerLevel level, BlockPos pos, Identifier field) {
        float best = Float.NaN;
        for (Envelope e : ENVELOPES) {
            if (!e.dim.dim().equals(level.dimension().identifier().toString())) continue;
            if (!e.field.equals(field)) continue;
            if (!e.box.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) continue;
            if (Float.isNaN(best) || envelopeBetter(field, e.strength, best)) best = e.strength;
        }
        return best;
    }

    private static boolean envelopeBetter(Identifier field, float a, float b) {
        if (field.getPath().endsWith("vac_field")) return a < b;   // 真空取更低
        return a > b;
    }

    /** 气氛覆盖(生态穹顶)。 */
    public static FieldEngine.AtmosphereSample queryAtmosphere(ServerLevel level, BlockPos pos) {
        for (Envelope e : ENVELOPES) {
            if (e.domeAtmosphere == null || e.domeTraits == null) continue;
            if (!e.dim.dim().equals(level.dimension().identifier().toString())) continue;
            if (e.box.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                return new FieldEngine.AtmosphereSample(e.domeAtmosphere, e.domeTraits);
            }
        }
        return null;
    }

    /** 引擎 override 挂钩(§6.5 L3)。 */
    public static void install() {
        FieldEngine.OVERRIDES.add((level, pos, field, natural) -> {
            float v = queryValue(level, pos, field);
            return v;
        });
        FieldEngine.ATMO_OVERRIDES.add(EnvelopeManager::queryAtmosphere);
    }

    /** 失谐跌落(§11.4)：能量中断按秒跌落，稳定器延缓 3 倍。 */
    public static void tickDecay(ServerLevel level) {
        for (Envelope e : ENVELOPES) {
            if (e.strength < e.configuredStrength) {
                float rate = e.configuredStrength * 0.01f * (e.stabilized ? 1f / 3f : 1f);
                e.strength = Math.min(e.configuredStrength, e.strength + rate);
                if (e.strength == e.configuredStrength) {
                    invalidate(level, e);
                }
            }
        }
    }

    public static List<Envelope> all() {
        return ENVELOPES;
    }
}
