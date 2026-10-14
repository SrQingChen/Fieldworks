package com.srqingchen.fieldworks.atmosphere;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.atmosphere.AtmosphereRegistry;
import com.srqingchen.fieldworks.fieldengine.EngineSavedData;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 未知群系的标签普查归族(§4.5)：任何世界生成类模组的群系进场即自动获得合理场学，零配置兼容。
 *
 * <p>对陌生群系(非原版、无数据包指定)的前 8 个生成区块抽样统计：
 * 木+叶密度 → 森林系；露天水占比(分母只计露天表面，V1.1) > 8% → 湿地系；沙为主 → 干旱系；
 * 冰/雪 → 寒冷系；都不满足 → 常规系。抽样在后台完成，结果写入存档并广播调试日志。</p>
 */
public final class BiomeCensus {
    private static final int SAMPLE_CHUNKS = 8;

    private static final class Pending {
        int scanned;
        float logsLeaves;
        float surfaceTotal;
        float surfaceWater;    // 露天水面(分子)
        float sand;
        float iceSnow;
    }

    private static final Map<Identifier, Pending> PENDING = new ConcurrentHashMap<>();
    private static final Map<Identifier, Queue<LevelChunk>> QUEUE = new ConcurrentHashMap<>();

    /** 区块登记时发现未知群系 → 排队抽样(§4.5)。 */
    public static void maybeQueueCensus(ResourceKey<net.minecraft.world.level.Level> dim,
                                         ResourceKey<Biome> key, LevelChunk chunk) {
        Identifier id = key.identifier();
        if (id.getNamespace().equals("minecraft")) return;    // 原版全表已内置(M0-T3)
        EngineSavedData d = FieldEngine.data();
        if (d == null || d.censusResults.containsKey(id)) return;
        if (BiomeAtmospheres.censusOf(id) != null) return;
        PENDING.computeIfAbsent(id, k -> new Pending());
        Queue<LevelChunk> q = QUEUE.computeIfAbsent(id, k -> new ArrayDeque<>());
        if (q.size() < SAMPLE_CHUNKS) q.add(chunk);
    }

    /** 每 tick 预算内处理普查队列(主线程只读区块数据，量级为 1 区块/tick)。 */
    public static void tick(MinecraftServer server) {
        if (QUEUE.isEmpty()) return;
        var it = QUEUE.entrySet().iterator();
        int budget = 1;
        while (it.hasNext() && budget-- > 0) {
            var e = it.next();
            Queue<LevelChunk> q = e.getValue();
            LevelChunk c = q.poll();
            if (c == null) {
                it.remove();
                continue;
            }
            sampleChunk(e.getKey(), c);
        }
    }

    private static void sampleChunk(Identifier biomeId, LevelChunk chunk) {
        Pending p = PENDING.computeIfAbsent(biomeId, k -> new Pending());
        int minY = chunk.getMinY();
        int baseX = chunk.getPos().getMinBlockX();
        int baseZ = chunk.getPos().getMinBlockZ();
        // 表面切片：WORLD_SURFACE 顶层 ±4(露天表面，V1.1 口径)
        for (int x = 0; x < 16; x += 2) {
            for (int z = 0; z < 16; z += 2) {
                int top = chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE, x, z);
                for (int y = top; y >= top - 4 && y >= minY; y--) {
                    var s = chunk.getBlockState(new BlockPos(baseX + x, y, baseZ + z));
                    if (s.isAir()) continue;
                    p.surfaceTotal++;
                    if (s.is(Blocks.WATER) || s.is(Blocks.ICE)) p.surfaceWater++;
                    if (s.is(BlockTags.SAND) || s.is(Blocks.MUD)) p.sand++;
                    if (s.is(BlockTags.ICE) || s.is(Blocks.SNOW) || s.is(Blocks.SNOW_BLOCK) || s.is(Blocks.POWDER_SNOW)) p.iceSnow++;
                }
            }
        }
        // 植被切片(粗样)：每 32 格抽一层
        for (int y = minY; y < chunk.getMaxY(); y += 32) {
            for (int x = 2; x < 16; x += 4) {
                for (int z = 2; z < 16; z += 4) {
                    var s = chunk.getBlockState(new BlockPos(baseX + x, y, baseZ + z));
                    if (s.is(BlockTags.LOGS) || s.is(BlockTags.LEAVES)) p.logsLeaves++;
                }
            }
        }
        p.scanned++;
        if (p.scanned >= SAMPLE_CHUNKS) {
            conclude(biomeId, p);
        }
    }

    /** 归族判定(§4.5 规则 2，V1.1 露天水口径)。 */
    private static void conclude(Identifier biomeId, Pending p) {
        float vegetationDensity = p.logsLeaves / Math.max(1f, p.scanned * 24f);
        float openWaterRatio = p.surfaceWater / Math.max(1f, p.surfaceTotal);
        Identifier result;
        if (vegetationDensity > 0.25f) {
            result = AtmosphereRegistry.OXYGEN_RICH;
        } else if (openWaterRatio > 0.08f) {
            result = AtmosphereRegistry.SALT_SPRAY;
        } else if (p.sand / Math.max(1f, p.surfaceTotal) > 0.5f) {
            result = AtmosphereRegistry.ARID_AIR;
        } else if (p.iceSnow / Math.max(1f, p.surfaceTotal) > 0.3f) {
            result = AtmosphereRegistry.FRIGID_AIR;
        } else {
            result = AtmosphereRegistry.AMBIENT_AIR;
        }
        EngineSavedData d = FieldEngine.data();
        if (d != null) {
            d.censusResults.put(biomeId, result);
            d.setDirty();
        }
        BiomeAtmospheres.putCensus(biomeId, result);
        PENDING.remove(biomeId);
        Fieldworks.LOGGER.info("[Fieldworks] 群系普查归族: {} -> {} (植被密度 {}, 露天水 {})",
                biomeId, result.getPath(), String.format("%.2f", vegetationDensity), String.format("%.2f", openWaterRatio));
    }

    public static void clearRuntime() {
        PENDING.clear();
        QUEUE.clear();
    }
}
