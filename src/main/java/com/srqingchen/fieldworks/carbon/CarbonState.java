package com.srqingchen.fieldworks.carbon;

import com.srqingchen.fieldworks.config.FWConfig;
import com.srqingchen.fieldworks.fieldengine.EngineSavedData;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.event.level.ExplosionEvent;

/**
 * 全局碳氛围场(§5)：全存档唯一，所有模组的行为共同谱写。
 * 数值只做"方向与量级"的宏观表达(启发稿 §3：整体是非常低数量级的变化)。
 */
public final class CarbonState {
    /** 自然平衡点(§5.1)。 */
    public static final float EQUILIBRIUM = 200f;
    public static final float CP_MIN = 0f, CP_MAX = 1000f;

    public static float cp() {
        return data().carbonCp;
    }

    /** 全局温度倍率(§5.2)：TempMult = 1 + clamp((cp−200)/800, −0.1, +0.6) × 0.25，受配置封顶。 */
    public static float tempMultiplier() {
        float cap = FWConfig.CARBON_TEMP_CAP.get().floatValue();
        float base = 1f + Math.max(-0.1f, Math.min(0.6f, (cp() - EQUILIBRIUM) / 800f)) * 0.25f;
        return Math.min(base, 1f + cap);
    }

    /** 碳捕获效率(§5.3)：cp 越高捕获效率越高(污染重→好捕捞)；低于平衡点效率下降。 */
    public static float captureEfficiency() {
        return Math.max(0.25f, Math.min(1.5f, 0.6f + (cp() - EQUILIBRIUM) / 500f));
    }

    /** 来源埋点：本模组机器按配方申报；第三方经 api.carbon.CarbonEvents#report。 */
    public static void report(ServerLevel level, double amount) {
        if (!FWConfig.CARBON_ENABLED.get()) return;
        EngineSavedData d = data();
        d.carbonCp = (float) Math.max(CP_MIN, Math.min(CP_MAX, d.carbonCp + amount));
    }

    /** 自然消减 + 日结算(§5.1)：decay/day = base(0.8cp‰) × (1 + max(0, cp−200)/150)；低于 200 缓慢回升。 */
    public static void settleDay(long day) {
        EngineSavedData d = data();
        if (!FWConfig.CARBON_ENABLED.get()) return;
        float cp = d.carbonCp;
        float decay;
        if (cp > EQUILIBRIUM) {
            decay = 0.0008f * cp * (1f + (cp - EQUILIBRIUM) / 150f);
        } else {
            decay = -(EQUILIBRIUM - cp) * 0.02f;   // 自然态回归
        }
        d.carbonCp = Math.max(CP_MIN, Math.min(CP_MAX, cp - decay));
        d.carbonHistory.add(d.carbonCp);
        if (d.carbonHistory.size() > 100) d.carbonHistory.remove(0);
        d.lastCarbonDay = day;
        d.setDirty();
    }

    /** TNT/爆炸类事件近似计数(§5.1 原版埋点)。 */
    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !FWConfig.CARBON_HEURISTIC.get()) return;
        report(sl, 0.02 * event.getExplosion().radius());
    }

    private static EngineSavedData data() {
        return FieldEngine.data();
    }

    private CarbonState() {}
}
