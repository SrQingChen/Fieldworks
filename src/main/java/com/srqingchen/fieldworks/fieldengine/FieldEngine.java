package com.srqingchen.fieldworks.fieldengine;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.atmosphere.AtmosphereRegistry;
import com.srqingchen.fieldworks.api.atmosphere.TraitVector;
import com.srqingchen.fieldworks.api.field.FieldChangedEvent;
import com.srqingchen.fieldworks.api.field.FieldRegistry;
import com.srqingchen.fieldworks.atmosphere.BiomeAtmospheres;
import com.srqingchen.fieldworks.atmosphere.BiomeCensus;
import com.srqingchen.fieldworks.atmosphere.VegetationIndex;
import com.srqingchen.fieldworks.carbon.CarbonState;
import com.srqingchen.fieldworks.config.FWConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 场引擎(§6 定案)：四层结构 / 零每 tick 遍历 / 预算制更新 / 读写分离。
 *
 * <ul>
 *   <li>L0 静态层：群系基础表({@link BiomeAtmospheres})，启动加载 + 数据包 reload，零成本</li>
 *   <li>L1 实例层：活跃实例轮转聚合(§6.3)，预算内摊销</li>
 *   <li>L2 全局层：碳压({@link CarbonState})，每 Minecraft 日结算一次</li>
 *   <li>L3 人工场层：包络覆盖({@link #OVERRIDES})，事件驱动无轮询</li>
 * </ul>
 *
 * 机器读场走 {@link #snapshot} 三级缓存(O(1))，永不轮询(§3.3)。
 */
public final class FieldEngine {
    static final Logger LOGGER = Fieldworks.LOGGER;

    private static MinecraftServer server;
    private static EngineSavedData data;
    private static final Object DATA_LOCK = new Object();

    /** L3 人工场包络覆盖钩子(EnvelopeManager 注册)。返回 Float.NaN 表示不覆盖。 */
    public interface ValueOverride {
        float override(ServerLevel level, BlockPos pos, Identifier field, float naturalValue);
    }

    public static final CopyOnWriteArraySet<ValueOverride> OVERRIDES = new CopyOnWriteArraySet<>();

    /** 包络气氛覆盖(生态穹顶等)：返回 null 表示不覆盖。 */
    public interface AtmosphereOverride {
        AtmosphereSample override(ServerLevel level, BlockPos pos);
    }

    public record AtmosphereSample(Identifier type, TraitVector traits) {}

    public static final CopyOnWriteArraySet<AtmosphereOverride> ATMO_OVERRIDES = new CopyOnWriteArraySet<>();

    /** 场订阅者(§6.6 订阅推送)：机器注册自身坐标的快照变更。 */
    public interface Subscriber {
        void onFieldChanged(FieldChangedEvent event);
    }

    private static final Map<SubKey, CopyOnWriteArraySet<Subscriber>> SUBSCRIPTIONS = new ConcurrentHashMap<>();

    private record SubKey(ResourceKey<Level> dim, long chunk) {}

    // ── 三级读缓存(§6.5) ──
    private static final Map<ResourceKey<Level>, Long2ObjectMap<FieldSnapshot>> CHUNK_CACHE = new ConcurrentHashMap<>();

    // ── 活跃实例轮转队列(§6.3) ──
    private static final ArrayDeque<AtmosphereInstance> RING = new ArrayDeque<>();

    private static ExecutorService ASYNC;
    private static long tickCount;
    private static long lastDay = -1;

    private FieldEngine() {}

    public static EngineSavedData data() {
        synchronized (DATA_LOCK) {
            if (data == null && server != null) {
                data = EngineSavedData.get(server);
            }
            return data;
        }
    }

    // ═══════════════ 生命周期 ═══════════════

    public static void onServerStarted(ServerStartedEvent event) {
        server = event.getServer();
        synchronized (DATA_LOCK) {
            data = EngineSavedData.get(server);
        }
        FWFields.init();
        ASYNC = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r, "fieldworks-engine");
            t.setDaemon(true);
            return t;
        });
        LOGGER.info("[Fieldworks] 场引擎启动：{} 个群系实例，cp={}", data.instances.size(), data.carbonCp);
    }

    public static void onServerStopping(ServerStoppingEvent event) {
        if (ASYNC != null) {
            ASYNC.shutdown();
            ASYNC = null;
        }
        CHUNK_CACHE.clear();
        SUBSCRIPTIONS.clear();
        RING.clear();
        server = null;
        synchronized (DATA_LOCK) {
            data = null;
        }
    }

    // ═══════════════ 主 tick：预算制(§6.1) ═══════════════

    public static void onServerTick(ServerTickEvent.Pre event) {
        MinecraftServer srv = event.getServer();
        tickCount++;
        // 全局层：每 Minecraft 日结算一次(§6.2 L2)
        ServerLevel overworld = srv.overworld();
        long day = overworld.getOverworldClockTime() / 24000L;
        if (day != lastDay) {
            if (lastDay >= 0) {
                CarbonState.settleDay(day);
                invalidateAll(overworld);
            }
            lastDay = day;
        }
        // 实例层：预算内轮转聚合
        long budgetNanos = (long) (FWConfig.ENGINE_BUDGET_MS.get().doubleValue() * 1_000_000);
        long start = System.nanoTime();
        EngineSavedData d = data();
        if (d == null) return;
        while (!RING.isEmpty() && System.nanoTime() - start < budgetNanos) {
            AtmosphereInstance inst = RING.pollFirst();
            if (inst == null) break;
            try {
                aggregate(d, inst, srv);
            } catch (Exception e) {
                LOGGER.error("[Fieldworks] 实例聚合异常 biome={}", inst.biome.identifier(), e);
            }
            RING.addLast(inst);
        }
        BiomeCensus.tick(srv);
    }

    /** 单实例聚合(§4.3/§4.6)：氧恢复 / FG 再生 / CR 沉降 / 动态扩散。微秒级。 */
    private static void aggregate(EngineSavedData d, AtmosphereInstance inst, MinecraftServer srv) {
        ServerLevel level = srv.getLevel(inst.dimension);
        if (level == null || inst.loadedChunks.isEmpty()) return;
        TraitVector base = inst.baseTraits();
        float viLoaded = inst.scannedChunks > 0 ? inst.viSum : inst.viEstimate() * inst.loadedChunks.size();
        float area = Math.max(1, inst.loadedChunks.size() + Math.max(0, inst.scannedChunks - inst.loadedChunks.size()));
        // 氧恢复(§4.3)：k × [VI/A + 估计] × 饱和抑制；速度平滑变慢
        float oxFloor = base.ox();
        float oxCeil = Math.max(oxFloor, 40f);
        float saturation = oxCeil - oxFloor <= 0.01f ? 0 : Math.max(0, (oxCeil - Math.max(0, oxFloor + inst.dynOx)) / (oxCeil - oxFloor));
        float regen = (0.02f * (viLoaded / Math.max(1, area) + inst.viEstimate() * 0.3f) / 100f + 0.005f) * saturation;
        inst.dynOx = Math.min(oxCeil - oxFloor, inst.dynOx + regen);
        // FG 再生(富沼缓升)
        if (inst.atmosphereType.equals(AtmosphereRegistry.MARSH_GAS)) {
            inst.dynFg = Math.min(6f, inst.dynFg + 0.004f);
        }
        // CR 自然沉降(极慢，准单向 §4.2)
        inst.dynCr = Math.max(0, inst.dynCr - 0.001f);
        // 动态扩散(§4.6)：活跃实例间 0.5% × Δ × 边界权重
        for (long nid : inst.neighbors) {
            AtmosphereInstance n = d.instances.get(nid);
            if (n == null || n.loadedChunks.isEmpty()) continue;
            float dOx = (n.dynOx + n.baseTraits().ox()) - (inst.dynOx + oxFloor);
            float flux = 0.005f * dOx / Math.max(1, inst.neighbors.size());
            inst.dynOx += flux;
            n.dynOx -= flux * 0.5f;
        }
        d.setDirty();
        // 缓存失效 + 事件推送(§6.6)
        invalidateInstance(level, inst);
    }

    // ═══════════════ 区块事件 ═══════════════

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getChunk() instanceof LevelChunk chunk)) return;
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        EngineSavedData d = data();
        if (d == null) return;
        ChunkPos cp = chunk.getPos();
        long clong = cp.pack();
        var map = d.chunkMap(level.dimension());
        long instId;
        if (map.containsKey(clong)) {
            instId = map.get(clong);
        } else {
            instId = registerChunk(d, level, chunk);
        }
        AtmosphereInstance inst = d.instances.get(instId);
        if (inst != null) {
            inst.loadedChunks.add(clong);
            if (!RING.contains(inst)) RING.addLast(inst);
        }
        // 植被指数异步统计(§6.4)：运行时零遍历，首次进入后台线程
        if (ASYNC != null && !VegetationIndex.isScanned(level.dimension(), clong)) {
            var stableChunk = chunk;
            ASYNC.submit(() -> {
                float vi = VegetationIndex.scan(stableChunk);
                VegetationIndex.submitResult(server, level.dimension(), clong, instId, vi);
            });
        }
    }

    /** 区块首次登记：归属实例(邻接同群系实例合并)、建立邻接图、哈希扰动初始化(§4.5/§6.3)。 */
    private static long registerChunk(EngineSavedData d, ServerLevel level, LevelChunk chunk) {
        ChunkPos cp = chunk.getPos();
        Holder<Biome> biomeHolder = level.getBiome(cp.getMiddleBlockPosition(64));
        var biomeKey = biomeHolder.unwrapKey().orElse(null);
        if (biomeKey == null) return -1;
        var map = d.chunkMap(level.dimension());
        // 邻接同群系实例合并
        long found = -1;
        for (int i = 0; i < 4; i++) {
            ChunkPos n = new ChunkPos(cp.x() + (i == 0 ? -1 : i == 1 ? 1 : 0), cp.z() + (i == 2 ? -1 : i == 3 ? 1 : 0));
            long nid = map.getOrDefault(n.pack(), -1);
            if (nid >= 0) {
                AtmosphereInstance ni = d.instances.get(nid);
                if (ni != null && ni.biome.equals(biomeKey)) {
                    found = nid;
                    break;
                }
            }
        }
        Identifier atmo = BiomeAtmospheres.atmosphereOf(biomeKey.identifier(), biomeHolder);
        AtmosphereInstance inst;
        if (found >= 0) {
            inst = d.instances.get(found);
        } else {
            // 新实例：确定性哈希扰动(世界种子 × 群系ID × 实例编号，§4.2)
            long seed = level.getSeed();
            Random rnd = new Random(seed ^ biomeKey.identifier().toString().hashCode() * 31L + d.nextInstanceId);
            float jitter = 0.9f + rnd.nextFloat() * 0.2f;
            inst = new AtmosphereInstance(d.newInstanceId(), level.dimension(), biomeKey, atmo, jitter, 0, 0, 0, 0, 0);
            d.instances.put(inst.id, inst);
        }
        map.put(cp.pack(), inst.id);
        // 邻接图(双向)
        for (int i = 0; i < 4; i++) {
            ChunkPos n = new ChunkPos(cp.x() + (i == 0 ? -1 : i == 1 ? 1 : 0), cp.z() + (i == 2 ? -1 : i == 3 ? 1 : 0));
            long nid = map.getOrDefault(n.pack(), -1);
            if (nid >= 0 && nid != inst.id) {
                inst.neighbors.add(nid);
                AtmosphereInstance ni = d.instances.get(nid);
                if (ni != null) ni.neighbors.add(inst.id);
            }
        }
        d.setDirty();
        // 未知群系 → 标签普查(§4.5)
        BiomeCensus.maybeQueueCensus(level.dimension(), biomeKey, chunk);
        return inst.id;
    }

    public static void onChunkUnload(ChunkEvent.Unload event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) return;
        EngineSavedData d = data();
        if (d == null) return;
        long clong = event.getChunk().getPos().pack();
        var map = d.chunkMap(level.dimension());
        long instId = map.getOrDefault(clong, -1);
        if (instId >= 0) {
            AtmosphereInstance inst = d.instances.get(instId);
            if (inst != null) {
                inst.loadedChunks.remove(clong);
                if (inst.loadedChunks.isEmpty()) {
                    RING.remove(inst);   // 移出活跃集，状态保留(§6.3)
                }
            }
        }
        CHUNK_CACHE.computeIfPresent(level.dimension(), (k, m) -> {
            m.remove(clong);
            return m;
        });
    }

    // ═══════════════ 方块事件：VI 增量(§6.4) ═══════════════

    public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) return;
        float dv = VegetationIndex.deltaValue(event.getPlacedBlock());
        if (dv != 0) VegetationIndex.applyDelta(level, event.getPos(), dv);
    }

    public static void onBlockBroken(BreakBlockEvent event) {
        if (event.getLevel().isClientSide() || !(event.getLevel() instanceof ServerLevel level)) return;
        float dv = VegetationIndex.deltaValue(event.getState());
        if (dv != 0) VegetationIndex.applyDelta(level, event.getPos(), -dv);
    }

    // ═══════════════ 采样 API(§6.5 三级缓存) ═══════════════

    public static FieldSnapshot snapshot(ServerLevel level, BlockPos pos) {
        long clong = ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4);
        Long2ObjectMap<FieldSnapshot> cache = CHUNK_CACHE.computeIfAbsent(level.dimension(), k -> new Long2ObjectOpenHashMap<>());
        FieldSnapshot snap = cache.get(clong);
        if (snap != null) return snap;
        snap = buildSnapshot(level, clong);
        cache.put(clong, snap);
        return snap;
    }

    public static float sample(ServerLevel level, BlockPos pos, Identifier field) {
        return snapshot(level, pos).value(field);
    }

    /** 组装快照：L0 基础值 + L1 实例动态 + L2 全局修正 + L3 包络最大值覆盖(§6.5)。 */
    private static FieldSnapshot buildSnapshot(ServerLevel level, long clong) {
        ChunkPos cp = ChunkPos.unpack(clong);
        BlockPos center = cp.getMiddleBlockPosition(64);
        Holder<Biome> biomeHolder = level.getBiome(center);
        var biomeKey = biomeHolder.unwrapKey().orElse(null);
        EngineSavedData d = data();
        float[] values = new float[FieldRegistry.size()];
        long gameTime = level.getGameTime();

        // 气氛场(实例特征值，含扰动与动态)
        Identifier atmoType = BiomeAtmospheres.atmosphereOf(
                biomeKey != null ? biomeKey.identifier() : Identifier.withDefaultNamespace("plains"), biomeHolder);
        TraitVector traits = AtmosphereRegistry.get(atmoType) != null ? AtmosphereRegistry.get(atmoType).baseline() : TraitVector.AMBIENT;
        if (biomeKey != null && d != null) {
            long instId = d.chunkMap(level.dimension()).getOrDefault(clong, -1);
            AtmosphereInstance inst = d.instances.get(instId);
            if (inst != null) {
                traits = inst.effectiveTraits();
            } else {
                traits = jitterTraits(level, atmoType, clong);
            }
        }
        // 深层/特殊位置规则(§4.4)
        traits = applyDepthRules(level, center, traits, biomeKey, atmoType, d);
        // 维度基线(附录 A)
        ResourceKey<Level> dim = level.dimension();
        boolean nether = dim == Level.NETHER;
        boolean end = dim == Level.END;
        if (nether) {
            atmoType = netherAtmosphere(level, center, atmoType, biomeKey, d);
            traits = AtmosphereRegistry.get(atmoType).baseline().lerp(traits, 0.3f);
        } else if (end) {
            atmoType = AtmosphereRegistry.TUNED_VOID;
            traits = AtmosphereRegistry.get(atmoType).baseline();
        }
        // 包络气氛覆盖(生态穹顶)
        for (AtmosphereOverride o : ATMO_OVERRIDES) {
            AtmosphereSample s = o.override(level, center);
            if (s != null) {
                atmoType = s.type();
                traits = s.traits();
                break;
            }
        }

        // ── 自然场计算 ──
        int sea = level.getSeaLevel();
        // 温度(℃)：群系基线 × 碳倍率 + 昼夜 + 深/高修正 + 熔炉余温(由 ContinuousFiring 注册为 override)
        float temp = BiomeAtmospheres.baseTemperatureC(biomeHolder);
        if (nether) temp = 80f + (temp + 20f) * 0.2f;
        if (end) temp = -10f;
        temp *= CarbonState.tempMultiplier();
        if (FWConfig.SEASONAL_DRIFT.get()) {
            temp += 5f * Math.sin(level.getOverworldClockTime() / 24000f / 8f * Math.PI * 2);
        }
        // 常燃熔炉余温(§12.5)：+2℃/台，最多叠加到 +5℃
        temp += com.srqingchen.fieldworks.furnace.ContinuousFiringManager.wasteHeat(level, center);
        if (center.getY() < 0) temp += 2f;                              // 深层地热
        else if (center.getY() > sea + 48) temp -= (center.getY() - sea - 48) * 0.03f;
        // 湿度(%RH)：群系基线 + 静态扩散(§4.6 查询时修正) + 雨
        float humid = BiomeAtmospheres.baseHumidity(biomeHolder, atmoType);
        humid += staticDiffusion(d, level, clong, humid, true);
        if (FWConfig.RAIN_HUMIDITY.get() && level.isRaining() && level.canSeeSky(center)) {
            humid += 20f;
            temp -= 3f;
        }
        humid = Math.max(0, Math.min(100, humid));
        // 光照：max(方块光, 天空光)
        int light = Math.max(level.getBrightness(LightLayer.BLOCK, center), level.getBrightness(LightLayer.SKY, center));
        // 磁场(§12.4)：基岩层 y<0 从 0.1T 线性升至 0.5T
        float magnetic;
        if (center.getY() <= -32) magnetic = 0.5f;
        else if (center.getY() < 0) magnetic = 0.1f + (-center.getY()) / 32f * 0.4f;
        else magnetic = 0.05f;
        // 引力：月相调制(满月 +15% 朔月 −10%) + 海洋潮汐 ±5%
        float gravity = 1f;
        try {
            var phase = level.environmentAttributes().getValue(
                    net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE, center);
            gravity *= 1f + (phase.index() == 0 ? 0.15f : phase.index() == 4 ? -0.10f : (4 - Math.abs(4 - phase.index())) * 0.0125f);
        } catch (Exception ignored) {}
        if (biomeHolder.is(BiomeTags.IS_OCEAN)) gravity *= 1.05f;
        // 真空(气压 Pa)：天穹层 PR 随高度衰减(≥100 格显著稀薄，§4.1)
        float pressure = traits.pr() * 1000f;   // PR≈100 → 1e5 Pa
        if (!nether) {
            int h = center.getY() - sea;
            if (h > 0) pressure *= Math.pow(0.5, h / 100.0);
            else if (h < 0) pressure += -h * 120f;   // 深层微升
        } else {
            pressure = 1.1e5f;
        }
        pressure = Math.max(1e-4f, Math.min(1.1e5f, pressure));
        // 辐射：天然本底 ~5，深板岩层 +，独居石带由勘探事件抬升(§12.4)
        float radiation = 5f;
        if (center.getY() < -16) radiation += 2f;

        set(values, FWFields.ATMOSPHERE, 100);   // 气氛场值 = 类型 + 特征值(单独通道)
        set(values, FWFields.LIGHT, light);
        set(values, FWFields.TEMPERATURE, clamp(temp, -60, 200));
        set(values, FWFields.HUMIDITY, humid);
        set(values, FWFields.MAGNETIC, magnetic);
        set(values, FWFields.GRAVITY, gravity);
        set(values, FWFields.VACUUM, pressure);
        set(values, FWFields.RADIATION, radiation);
        set(values, FWFields.CARBON, CarbonState.cp());

        // ── L3 人工场覆盖(包络最大值覆盖，§6.5) ──
        if (!OVERRIDES.isEmpty()) {
            for (int i = 0; i < values.length; i++) {
                Identifier fid = FieldRegistry.byOrdinal(i);
                if (fid == null) continue;
                float best = values[i];
                for (ValueOverride o : OVERRIDES) {
                    float v = o.override(level, center, fid, values[i]);
                    if (!Float.isNaN(v)) best = envelopeCombine(fid, values[i], v);
                }
                values[i] = best;
            }
        }
        return new FieldSnapshot(values, atmoType, traits, gameTime);
    }

    /** 人工场与自然场的合成：人工压制自然值(§3.1 人工场量级远超自然)。 */
    private static float envelopeCombine(Identifier field, float natural, float envelope) {
        if (field.equals(FWFields.VACUUM) || field.equals(FWFields.VAC_FIELD)) {
            return Math.min(natural, envelope);         // 真空：取更低气压
        }
        if (field.equals(FWFields.TEMPERATURE)) {
            return natural == 0 ? envelope : envelope;  // 生态穹顶直接定值
        }
        return Math.max(natural, envelope);
    }

    private static TraitVector jitterTraits(ServerLevel level, Identifier atmoType, long clong) {
        var t = AtmosphereRegistry.get(atmoType);
        TraitVector base = t == null ? TraitVector.AMBIENT : t.baseline();
        Random rnd = new Random(level.getSeed() ^ clong * 7919L);
        float j = 0.9f + rnd.nextFloat() * 0.2f;
        return new TraitVector(base.ox() * j, base.fg() * j, base.in() * (2 - j), base.cr() * j, base.vd(), base.pr());
    }

    /** 深层与特殊位置规则(§4.4)：幽邃渗透 / 深暗完全体 / 虚空界面。 */
    private static TraitVector applyDepthRules(ServerLevel level, BlockPos pos, TraitVector traits,
                                               ResourceKey<Biome> biomeKey, Identifier atmoType, EngineSavedData d) {
        if (level.dimension() != Level.OVERWORLD) return traits;
        int y = pos.getY();
        if (y >= 0) return traits;
        var deep = AtmosphereRegistry.get(AtmosphereRegistry.DEEP_AIR).baseline();
        if (biomeKey != null && biomeKey.identifier().getPath().equals("deep_dark")) {
            return deep.lerp(traits, 0.1f).with(0, 8f);   // 完全体 OX 8
        }
        float t = Math.min(1f, -y / 64f);                 // 64 格插值完成
        TraitVector mixed = traits.lerp(deep, t);
        // 基岩下虚空界面(谐振钻贯穿后，§4.4)：由 ResonanceDrill 登记
        if (d != null && y < level.getMinY() + 8 && isVoidInterface(d, pos)) {
            return AtmosphereRegistry.get(AtmosphereRegistry.VOID_AIR).baseline();
        }
        return mixed;
    }

    private static boolean isVoidInterface(EngineSavedData d, BlockPos pos) {
        var list = d.portals.get("void_interface");
        if (list == null) return false;
        for (long[] p : list) {
            if (Math.abs(p[0] - pos.getX()) <= 8 && Math.abs(p[1] - pos.getY()) <= 8 && Math.abs(p[2] - pos.getZ()) <= 8) {
                return true;
            }
        }
        return false;
    }

    /** 下界大气(§4.2)：荒地硫化 / 灵魂沙灰烬 / 玄武岩玄温 / 绯诡菌林 / 熔岩海 ±16 灼流带。 */
    private static Identifier netherAtmosphere(ServerLevel level, BlockPos pos, Identifier fallback,
                                               ResourceKey<Biome> biomeKey, EngineSavedData d) {
        int lavaSea = 31;    // 下界熔岩海面
        if (Math.abs(pos.getY() - lavaSea) <= 16) return AtmosphereRegistry.SCORCH_GAS;
        if (biomeKey == null) return AtmosphereRegistry.SULFURIC_GAS;
        return switch (biomeKey.identifier().getPath()) {
            case "soul_sand_valley" -> AtmosphereRegistry.ASHEN_GAS;
            case "basalt_deltas" -> AtmosphereRegistry.BASALTIC_GAS;
            case "crimson_forest", "warped_forest" -> AtmosphereRegistry.FUNGAL_GAS;
            default -> AtmosphereRegistry.SULFURIC_GAS;
        };
    }

    /** 静态扩散(§4.6)：查询时计算 + 结果缓存(邻接群系基础值全静态)。 */
    private static float staticDiffusion(EngineSavedData d, ServerLevel level, long clong, float selfBase, boolean humidity) {
        if (d == null) return 0;
        long instId = d.chunkMap(level.dimension()).getOrDefault(clong, -1);
        AtmosphereInstance inst = d.instances.get(instId);
        if (inst == null || inst.neighbors.isEmpty()) return 0;
        float delta = 0;
        int n = inst.neighbors.size();
        for (long nid : inst.neighbors) {
            AtmosphereInstance nb = d.instances.get(nid);
            if (nb == null) continue;
            float nbBase = humidity ? humidityOfBiome(level, nb) : 0;
            delta += (nbBase - selfBase) * 0.08f / n;
        }
        return delta;
    }

    private static float humidityOfBiome(ServerLevel level, AtmosphereInstance inst) {
        var lookup = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BIOME);
        var holder = lookup.get(inst.biome).orElse(null);
        if (holder == null) return 45f;
        return BiomeAtmospheres.baseHumidity(holder, inst.atmosphereType);
    }

    // ═══════════════ 缓存失效与事件推送(§6.6) ═══════════════

    public static void invalidate(ServerLevel level, ChunkPos cp) {
        Long2ObjectMap<FieldSnapshot> cache = CHUNK_CACHE.get(level.dimension());
        if (cache != null) {
            FieldSnapshot old = cache.remove(cp.pack());
            if (old != null) notifySubscribers(level, cp, old);
        }
    }

    private static void invalidateInstance(ServerLevel level, AtmosphereInstance inst) {
        Long2ObjectMap<FieldSnapshot> cache = CHUNK_CACHE.get(level.dimension());
        if (cache == null) return;
        var map = data().chunkMap(level.dimension());
        for (long clong : inst.loadedChunks) {
            if (map.get(clong) != inst.id) continue;
            FieldSnapshot old = cache.remove(clong);
            if (old != null) notifySubscribers(level, ChunkPos.unpack(clong), old);
        }
    }

    public static void invalidateAll(ServerLevel level) {
        CHUNK_CACHE.remove(level.dimension());
    }

    private static void notifySubscribers(ServerLevel level, ChunkPos cp, FieldSnapshot oldSnap) {
        var set = SUBSCRIPTIONS.get(new SubKey(level.dimension(), cp.pack()));
        if (set == null || set.isEmpty()) return;
        FieldSnapshot fresh = snapshot(level, cp.getMiddleBlockPosition(64));
        FieldChangedEvent event = new FieldChangedEvent(level, cp.getWorldPosition(), view(fresh));
        for (Subscriber s : set) {
            try {
                s.onFieldChanged(event);
            } catch (Exception e) {
                LOGGER.error("[Fieldworks] 订阅者异常", e);
            }
        }
    }

    private static FieldChangedEvent.FieldSnapshotView view(FieldSnapshot snap) {
        return new FieldChangedEvent.FieldSnapshotView() {
            @Override
            public float value(Identifier fieldId) { return snap.value(fieldId); }

            @Override
            public com.srqingchen.fieldworks.api.atmosphere.AtmosphereType atmosphereType() {
                return snap.atmosphere();
            }

            @Override
            public TraitVector traits() { return snap.traits(); }
        };
    }

    // ═══════════════ 订阅(§6.6) ═══════════════

    public static void subscribe(ServerLevel level, BlockPos pos, Subscriber s) {
        SUBSCRIPTIONS.computeIfAbsent(new SubKey(level.dimension(), ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4)),
                k -> new CopyOnWriteArraySet<>()).add(s);
    }

    public static void unsubscribe(ServerLevel level, BlockPos pos, Subscriber s) {
        var set = SUBSCRIPTIONS.get(new SubKey(level.dimension(), ChunkPos.pack(pos.getX() >> 4, pos.getZ() >> 4)));
        if (set != null) set.remove(s);
    }

    // ── 杂项 ──

    private static void set(float[] arr, Identifier field, float v) {
        int ord = FieldRegistry.ordinal(field);
        if (ord >= 0 && ord < arr.length) arr[ord] = v;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    public static long tick() {
        return tickCount;
    }

    public static int subscriptionCount() {
        return SUBSCRIPTIONS.size();
    }

    public static MinecraftServer server() {
        return server;
    }
}
