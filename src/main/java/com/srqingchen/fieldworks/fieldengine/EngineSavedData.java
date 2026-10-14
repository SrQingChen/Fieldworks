package com.srqingchen.fieldworks.fieldengine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.srqingchen.fieldworks.Fieldworks;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 场引擎存档(fieldworks_engine.dat，§6.7)。
 * 增量序列化：只有 dirty 的实例进写盘集；写盘频率跟随世界自动保存。
 */
public final class EngineSavedData extends SavedData {

    // ── 实例层 L1 ──
    public final Map<Long, AtmosphereInstance> instances = new HashMap<>();
    /** chunk(dim key) → instance id。dimKey = dimension 字符串。 */
    public final Map<String, Long2LongOpenHashMap> chunkInstance = new HashMap<>();
    public long nextInstanceId = 1;

    // ── 普查归族结果(§4.5) ──
    public final Map<Identifier, Identifier> censusResults = new HashMap<>();

    // ── 传送门登记(§4.6 渗漏) ──
    public final Map<String, List<long[]>> portals = new HashMap<>();

    // ── 全局层 L2(碳氛围场 §5) ──
    public float carbonCp = 200f;
    public final List<Float> carbonHistory = new ArrayList<>();
    public long lastCarbonDay = -1;

    public EngineSavedData() {}

    public Long2LongOpenHashMap chunkMap(ResourceKey<Level> dim) {
        return chunkInstance.computeIfAbsent(dim.identifier().toString(), k -> new Long2LongOpenHashMap());
    }

    public long newInstanceId() {
        return nextInstanceId++;
    }

    // ── Codec ──

    private record InstanceRecord(long id, String dim, String biome, String atmo, float jitter,
                                   float dynOx, float dynFg, float dynCr, float viSum, int scanned,
                                   List<Long> neighbors) {
        static final Codec<InstanceRecord> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.LONG.fieldOf("id").forGetter(InstanceRecord::id),
                Codec.STRING.fieldOf("dim").forGetter(InstanceRecord::dim),
                Codec.STRING.fieldOf("biome").forGetter(InstanceRecord::biome),
                Codec.STRING.fieldOf("atmo").forGetter(InstanceRecord::atmo),
                Codec.FLOAT.optionalFieldOf("jitter", 1f).forGetter(InstanceRecord::jitter),
                Codec.FLOAT.optionalFieldOf("dyn_ox", 0f).forGetter(InstanceRecord::dynOx),
                Codec.FLOAT.optionalFieldOf("dyn_fg", 0f).forGetter(InstanceRecord::dynFg),
                Codec.FLOAT.optionalFieldOf("dyn_cr", 0f).forGetter(InstanceRecord::dynCr),
                Codec.FLOAT.optionalFieldOf("vi", 0f).forGetter(InstanceRecord::viSum),
                Codec.INT.optionalFieldOf("chunks", 0).forGetter(InstanceRecord::scanned),
                Codec.list(Codec.LONG).optionalFieldOf("neighbors", List.of()).forGetter(InstanceRecord::neighbors)
        ).apply(inst, InstanceRecord::new));
    }

    private record ChunkAssign(String dim, List<long[]> pairs) {
        static final Codec<ChunkAssign> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("dim").forGetter(ChunkAssign::dim),
                Codec.list(Codec.LONG.listOf()).fieldOf("pairs").forGetter(c ->
                        c.pairs.stream().map(p -> List.of(p[0], p[1])).toList())
        ).apply(inst, (dim, lists) -> new ChunkAssign(dim,
                lists.stream().map(l -> new long[]{l.get(0), l.get(1)}).toList())));
    }

    private record PortalRecord(String dim, List<long[]> positions) {
        static final Codec<PortalRecord> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.fieldOf("dim").forGetter(PortalRecord::dim),
                Codec.list(Codec.LONG.listOf()).fieldOf("pos").forGetter(p ->
                        p.positions.stream().map(l -> List.of(l[0], l[1], l[2])).toList())
        ).apply(inst, (dim, lists) -> new PortalRecord(dim,
                lists.stream().map(l -> new long[]{l.get(0), l.get(1), l.get(2)}).toList())));
    }

    static final Codec<EngineSavedData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.list(InstanceRecord.CODEC).optionalFieldOf("instances", List.of())
                    .forGetter(d -> d.instances.values().stream().map(i -> new InstanceRecord(
                            i.id, i.dimension.identifier().toString(), i.biome.identifier().toString(),
                            i.atmosphereType.toString(), i.jitterScale, i.dynOx, i.dynFg, i.dynCr,
                            i.viSum, i.scannedChunks, boxList(i.neighbors)))
                            .toList()),
            Codec.list(ChunkAssign.CODEC).optionalFieldOf("chunks", List.of())
                    .forGetter(d -> d.chunkInstance.entrySet().stream().map(e -> {
                        List<long[]> pairs = new ArrayList<>();
                        e.getValue().long2LongEntrySet().forEach(en -> pairs.add(new long[]{en.getLongKey(), en.getLongValue()}));
                        return new ChunkAssign(e.getKey(), pairs);
                    }).toList()),
            Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC).optionalFieldOf("census", Map.of())
                    .forGetter(d -> d.censusResults),
            Codec.list(Codec.FLOAT).optionalFieldOf("carbon_history", List.of())
                    .forGetter(d -> List.copyOf(d.carbonHistory)),
            Codec.FLOAT.optionalFieldOf("carbon_cp", 200f).forGetter(d -> d.carbonCp),
            Codec.LONG.optionalFieldOf("carbon_day", -1L).forGetter(d -> d.lastCarbonDay),
            Codec.LONG.optionalFieldOf("next_instance", 1L).forGetter(d -> d.nextInstanceId),
            Codec.list(PortalRecord.CODEC).optionalFieldOf("portals", List.of())
                    .forGetter(d -> d.portals.entrySet().stream().map(e -> new PortalRecord(e.getKey(), e.getValue())).toList())
    ).apply(inst, EngineSavedData::new));

    private static List<Long> boxList(LongOpenHashSet set) {
        List<Long> out = new ArrayList<>(set.size());
        set.forEach(out::add);
        return out;
    }

    private EngineSavedData(List<InstanceRecord> instances, List<ChunkAssign> chunks,
                            Map<Identifier, Identifier> census, List<Float> carbonHistory,
                            float carbonCp, long carbonDay, long nextInstance, List<PortalRecord> portals) {
        for (InstanceRecord r : instances) {
            ResourceKey<Level> dim = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, Identifier.parse(r.dim()));
            ResourceKey<net.minecraft.world.level.biome.Biome> biome =
                    ResourceKey.create(net.minecraft.core.registries.Registries.BIOME, Identifier.parse(r.biome()));
            var i = new AtmosphereInstance(r.id(), dim, biome, Identifier.parse(r.atmo()), r.jitter(),
                    r.dynOx(), r.dynFg(), r.dynCr(), r.viSum(), r.scanned());
            for (long n : r.neighbors()) i.neighbors.add(n);
            this.instances.put(r.id(), i);
            this.nextInstanceId = Math.max(this.nextInstanceId, r.id() + 1);
        }
        for (ChunkAssign c : chunks) {
            Long2LongOpenHashMap m = new Long2LongOpenHashMap();
            for (long[] p : c.pairs()) m.put(p[0], p[1]);
            this.chunkInstance.put(c.dim(), m);
        }
        this.censusResults.putAll(census);
        this.carbonHistory.addAll(carbonHistory);
        this.carbonCp = carbonCp;
        this.lastCarbonDay = carbonDay;
        this.nextInstanceId = Math.max(this.nextInstanceId, nextInstance);
        for (PortalRecord p : portals) this.portals.put(p.dim(), new ArrayList<>(p.positions()));
    }

    public static final SavedDataType<EngineSavedData> TYPE = new SavedDataType<>(
            Fieldworks.id("engine"), EngineSavedData::new, CODEC);

    public static EngineSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    /** AddServerReloadListenersEvent 钩子：数据包重载时刷新静态映射(§19.3)。 */
    public static void onReloadListeners(net.neoforged.neoforge.event.AddServerReloadListenersEvent event) {
        ReloadListeners.applyCensusToStatic();
    }
}
