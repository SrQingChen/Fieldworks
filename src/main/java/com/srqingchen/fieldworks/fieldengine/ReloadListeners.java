package com.srqingchen.fieldworks.fieldengine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.field.FieldDefinition;
import com.srqingchen.fieldworks.api.field.FieldRegistry;
import com.srqingchen.fieldworks.atmosphere.BiomeAtmospheres;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * 数据包分层装载(§19.3)：
 * <ul>
 *   <li>data/fieldworks/field/*.json — 场定义覆盖(单位/区间/同步策略)</li>
 *   <li>data/fieldworks/atmosphere/biome_map.json — 群系→大气强制指定(整合包作者的最终话语权)</li>
 * </ul>
 */
public final class ReloadListeners {

    private record BiomeMapEntry(Map<Identifier, Identifier> map) {
        static final Codec<BiomeMapEntry> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.unboundedMap(Identifier.CODEC, Identifier.CODEC)
                        .optionalFieldOf("biomes", Map.of()).forGetter(BiomeMapEntry::map)
        ).apply(inst, BiomeMapEntry::new));
    }

    private static final class FieldDefListener extends SimpleJsonResourceReloadListener<FieldDefinition> {
        FieldDefListener() {
            super(FieldDefinition.CODEC, new FileToIdConverter("fieldworks/field", ".json"));
        }

        @Override
        protected void apply(Map<Identifier, FieldDefinition> data, net.minecraft.server.packs.resources.ResourceManager rm,
                             net.minecraft.util.profiling.ProfilerFiller profiler) {
            // 覆盖内置场的元数据(ordinal 不变)；新场则注册
            data.forEach((id, def) -> FieldRegistry.register(def));
            Fieldworks.LOGGER.info("[Fieldworks] 场定义装载：{} 项(含覆盖)", data.size());
        }
    }

    private static final class BiomeMapListener extends SimpleJsonResourceReloadListener<BiomeMapEntry> {
        BiomeMapListener() {
            super(BiomeMapEntry.CODEC, new FileToIdConverter("fieldworks/atmosphere", ".json"));
        }

        @Override
        protected void apply(Map<Identifier, BiomeMapEntry> data, net.minecraft.server.packs.resources.ResourceManager rm,
                             net.minecraft.util.profiling.ProfilerFiller profiler) {
            Map<Identifier, Identifier> merged = new HashMap<>();
            data.forEach((id, entry) -> merged.putAll(entry.map()));
            BiomeAtmospheres.applyOverrides(merged, true);
            Fieldworks.LOGGER.info("[Fieldworks] 群系大气映射装载：{} 项强制指定", merged.size());
        }
    }

    public static void register(AddServerReloadListenersEvent event) {
        event.addListener(Fieldworks.id("field_definitions"), new FieldDefListener());
        event.addListener(Fieldworks.id("atmosphere_biome_map"), new BiomeMapListener());
    }

    /** 存档中的普查结果写回静态映射(启动/重载时)。 */
    public static void applyCensusToStatic() {
        EngineSavedData d = FieldEngine.data();
        if (d == null) return;
        d.censusResults.forEach(BiomeAtmospheres::putCensus);
    }
}
