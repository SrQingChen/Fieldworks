package com.srqingchen.fieldworks.atmosphere;

import com.srqingchen.fieldworks.fieldengine.AtmosphereInstance;
import com.srqingchen.fieldworks.fieldengine.EngineSavedData;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * 大气消耗三模式的实例池操作(§7.3)：
 * 消耗型(负值扣池，可恢复项缓慢回填)/排放型(正值入池)/无关型(不触碰)。
 * 数值尺度："单人重度使用数小时才可感知偏移"——战略层资源观(D9)。
 */
public final class AtmosphereInstanceConsume {

    public static void apply(ServerLevel level, BlockPos pos, float dOx, float dFg, float dCr) {
        EngineSavedData d = FieldEngine.data();
        if (d == null) return;
        long clong = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        long instId = d.chunkMap(level.dimension()).getOrDefault(clong, -1);
        AtmosphereInstance inst = d.instances.get(instId);
        if (inst == null) return;
        if (dOx != 0) inst.dynOx = Math.max(-inst.baseTraits().ox() * 0.8f, Math.min(20, inst.dynOx + dOx));
        if (dFg != 0) inst.dynFg = Math.max(-inst.baseTraits().fg() * 0.8f, Math.min(20, inst.dynFg + dFg));
        if (dCr != 0) inst.dynCr = Math.max(0, Math.min(60, inst.dynCr + dCr));
        d.setDirty();
        FieldEngine.invalidate(level, ChunkPos.unpack(clong));
    }

    private AtmosphereInstanceConsume() {}
}
