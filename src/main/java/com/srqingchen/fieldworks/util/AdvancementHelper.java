package com.srqingchen.fieldworks.util;

import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * 进度代码授予(§8.5)：impossible 触发器类进度经本助手授予，
 * 条件名统一为 "granted"(与生成器约定一致)。
 */
public final class AdvancementHelper {

    public static void award(ServerPlayer sp, String path) {
        AdvancementHolder holder = sp.level().getServer().getAdvancements().get(Fieldworks.id(path));
        if (holder != null) {
            sp.getAdvancements().award(holder, "granted");
        }
    }

    /** 授予范围内最近玩家(机器侧事件无明确归属者时使用，§8.5)。 */
    public static void awardNearby(ServerLevel level, net.minecraft.core.BlockPos pos, double radius, String path) {
        ServerPlayer best = null;
        double bestDist = radius * radius;
        for (ServerPlayer sp : level.players()) {
            double d = sp.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
            if (d < bestDist) {
                bestDist = d;
                best = sp;
            }
        }
        if (best != null) {
            award(best, path);
        }
    }

    private AdvancementHelper() {}
}
