package com.srqingchen.fieldworks.api.envelope;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;

/**
 * 人工场包络查询与构建(§19.1 api.envelope)。
 */
public final class EnvelopeAPI {

    /** 查询位置某场的人工包络值(NaN = 无覆盖)。 */
    public static float query(ServerLevel level, BlockPos pos, Identifier field) {
        return com.srqingchen.fieldworks.envelope.EnvelopeManager.queryValue(level, pos, field);
    }

    /** 位置是否处于任一成形包络内。 */
    public static boolean inEnvelope(ServerLevel level, BlockPos pos) {
        return !Float.isNaN(queryAny(level, pos));
    }

    private static float queryAny(ServerLevel level, BlockPos pos) {
        for (var e : com.srqingchen.fieldworks.envelope.EnvelopeManager.all()) {
            if (e.dim.dim().equals(level.dimension().identifier().toString())
                    && e.box.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                return e.strength;
            }
        }
        return Float.NaN;
    }

    private EnvelopeAPI() {}
}
