package com.srqingchen.fieldworks.api.carbon;

import net.minecraft.server.level.ServerLevel;

/**
 * 碳氛围场来源埋点 API(§5.1 / §19.1 api.carbon)。
 * 第三方模组主动上报(精确)；未接入的模组走启发式近似。
 */
public final class CarbonEvents {

    /**
     * 上报一笔碳排放(正)或碳封存(负)。单位为任意积分：推荐
     * "每燃烧 1 个煤 = 0.01"。数值只做方向与量级的宏观表达(§5.1)。
     */
    public static void report(ServerLevel level, double amount) {
        com.srqingchen.fieldworks.carbon.CarbonState.report(level, amount);
    }

    private CarbonEvents() {}
}
