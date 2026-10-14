package com.srqingchen.fieldworks.atmosphere;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.fieldengine.AtmosphereInstance;
import com.srqingchen.fieldworks.fieldengine.EngineSavedData;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 植被指数 VI(§4.3/§6.4)：
 * VI = 1.0×logs + 0.5×leaves(生成期整棵放置天然与原木相连) + 0.3×短植被 + 2.0×苔/发光浆果。
 *
 * <p>生成期一次性统计(生成线程异步)，运行期增量事件(种树/砍树 O(1) 字典操作)，
 * 从不在主线程遍历方块(§21.1 红线)。</p>
 */
public final class VegetationIndex {
    public static final TagKey<Block> MOSS_LIKE = TagKey.create(Registries.BLOCK, Fieldworks.id("moss_like"));

    private static final Map<String, LongOpenHashSet> SCANNED = new ConcurrentHashMap<>();

    private static String key(ResourceKey<Level> dim) {
        return dim.identifier().toString();
    }

    public static boolean isScanned(ResourceKey<Level> dim, long chunk) {
        return SCANNED.computeIfAbsent(key(dim), k -> new LongOpenHashSet()).contains(chunk);
    }

    /**
     * 后台线程扫描区块(与光照计算同级的既有等待预期，§6.4)。
     * 返回该区块 VI(供实例聚合)。
     */
    public static float scan(LevelChunk chunk) {
        float vi = 0;
        int minY = chunk.getMinY();
        int maxY = chunk.getMaxY();
        // 分层扫：只扫 y<160 且非空区段(原版主世界海面以上极少植被)
        for (int y = minY; y < Math.min(maxY, 200); y++) {
            for (int x = 0; x < 16; x++) {
                for (int z = 0; z < 16; z++) {
                    BlockState s = chunk.getBlockState(new BlockPos(x + (chunk.getPos().getMinBlockX()), y, z + (chunk.getPos().getMinBlockZ())));
                    if (s.isAir()) continue;
                    vi += valueOf(s);
                }
            }
        }
        return vi;
    }

    /** 单方块 VI 贡献(§4.3 公式)。 */
    public static float valueOf(BlockState s) {
        if (s.is(BlockTags.LOGS)) return 1.0f;
        if (s.is(BlockTags.LEAVES)) return 0.5f;
        if (s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.MOSS_CARPET) || s.is(Blocks.CAVE_VINES)
                || s.is(Blocks.CAVE_VINES_PLANT) || s.is(Blocks.BIG_DRIPLEAF)) {
            return 2.0f;
        }
        if (isShortVegetation(s)) return 0.3f;
        return 0;
    }

    private static boolean isShortVegetation(BlockState s) {
        return s.is(BlockTags.CROPS) || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS)
                || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.TALL_GRASS) || s.is(Blocks.SHORT_GRASS)
                || s.is(Blocks.FERN) || s.is(Blocks.LARGE_FERN) || s.is(Blocks.SWEET_BERRY_BUSH);
    }

    /** 放置/破坏事件的 VI 增量(§6.4 运行期)。 */
    public static float deltaValue(BlockState state) {
        float v = valueOf(state);
        return v;
    }

    /** 主线程提交点：异步扫描结果写回引擎状态(原子写回，§6.7)。 */
    public static void submitResult(MinecraftServer server, ResourceKey<Level> dim, long chunk, long instanceId, float vi) {
        SCANNED.computeIfAbsent(key(dim), k -> new LongOpenHashSet()).add(chunk);
        EngineSavedData d = FieldEngine.data();
        if (d == null) return;
        AtmosphereInstance inst = d.instances.get(instanceId);
        if (inst != null) {
            inst.viSum += vi;
            inst.scannedChunks++;
            d.setDirty();
            ServerLevel level = server.getLevel(dim);
            if (level != null) {
                FieldEngine.invalidate(level, net.minecraft.world.level.ChunkPos.unpack(chunk));
            }
        }
    }

    /** 种树/砍树：单方块增量(§6.4)。 */
    public static void applyDelta(ServerLevel level, BlockPos pos, float delta) {
        EngineSavedData d = FieldEngine.data();
        if (d == null) return;
        long clong = net.minecraft.world.level.ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        long instId = d.chunkMap(level.dimension()).getOrDefault(clong, -1);
        AtmosphereInstance inst = d.instances.get(instId);
        if (inst != null) {
            inst.viSum = Math.max(0, inst.viSum + delta);
            d.setDirty();
            FieldEngine.invalidate(level, net.minecraft.world.level.ChunkPos.unpack(clong));
        }
    }

    public static void clearRuntime() {
        SCANNED.clear();
    }
}
