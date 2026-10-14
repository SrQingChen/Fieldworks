package com.srqingchen.fieldworks.giant;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 巨物结构定义(§13.1，data/fieldworks/giant_structures/&lt;variant&gt;.json)：
 * 蓝图投影与自动搭建的数据源(JSON 按需加载并缓存)。
 */
public record GiantStructure(List<Placed> blocks, int[] controller, int[] size) {

    public record Placed(int x, int y, int z, Block block) {
        public BlockPos at(BlockPos origin) {
            return origin.offset(x, y, z);
        }
    }

    public static final com.mojang.serialization.Codec<Placed> PLACED_CODEC = RecordCodecBuilder.create(inst -> inst.group(
            com.mojang.serialization.Codec.INT.fieldOf("x").forGetter(Placed::x),
            com.mojang.serialization.Codec.INT.fieldOf("y").forGetter(Placed::y),
            com.mojang.serialization.Codec.INT.fieldOf("z").forGetter(Placed::z),
            Identifier.CODEC.fieldOf("block").forGetter(p -> BuiltInRegistries.BLOCK.getKey(p.block()))
    ).apply(inst, (x, y, z, id) -> new Placed(x, y, z, BuiltInRegistries.BLOCK.getValue(id))));

    /** controller: 控制器在结构内的偏移(缺省时自动寻找 GiantControllerBlock)。 */
    public static final com.mojang.serialization.Codec<int[]> CONTROLLER_CODEC =
            com.mojang.serialization.Codec.INT.listOf().xmap(
                    l -> l.stream().mapToInt(Integer::intValue).toArray(),
                    a -> java.util.Arrays.stream(a).boxed().toList());

    public static final com.mojang.serialization.Codec<GiantStructure> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            PLACED_CODEC.listOf().fieldOf("blocks").forGetter(GiantStructure::blocks),
            CONTROLLER_CODEC.optionalFieldOf("controller", new int[0]).forGetter(GiantStructure::controller),
            CONTROLLER_CODEC.optionalFieldOf("size", new int[0]).forGetter(GiantStructure::size)
    ).apply(inst, GiantStructure::new));

    private static final Map<String, GiantStructure> CACHE = new ConcurrentHashMap<>();

    /** 原点语义：anchor_min_corner —— 结构 (0,0,0) 对齐锚点。 */
    @Nullable
    public static GiantStructure load(MinecraftServer server, String variant) {
        return CACHE.computeIfAbsent(variant, v -> {
            var id = Identifier.fromNamespaceAndPath(Fieldworks.MODID, "giant_structures/" + v + ".json");
            var resOpt = server.getResourceManager().getResource(id);
            if (resOpt.isEmpty()) {
                Fieldworks.LOGGER.warn("[Fieldworks] 巨物结构 {} 不存在", v);
                return new GiantStructure(List.of(), new int[0], new int[0]);
            }
            try (var is = resOpt.get().open()) {
                return parseStream(is, v);
            } catch (Exception e) {
                Fieldworks.LOGGER.warn("[Fieldworks] 巨物结构 {} 加载失败: {}", v, e.toString());
                return new GiantStructure(List.of(), new int[0], new int[0]);
            }
        });
    }

    /** 从任意资源流解析(客户端书内投影复用；失败返回空结构)。 */
    public static GiantStructure parseStream(java.io.InputStream is, String variant) {
        try {
            var json = JsonParser.parseReader(new InputStreamReader(is));
            var parsed = GiantStructure.CODEC.parse(JsonOps.INSTANCE, json).result();
            return parsed.orElseGet(() -> {
                Fieldworks.LOGGER.warn("[Fieldworks] 巨物结构 {} 解析失败", variant);
                return new GiantStructure(List.of(), new int[0], new int[0]);
            });
        } catch (Exception e) {
            Fieldworks.LOGGER.warn("[Fieldworks] 巨物结构 {} 解析异常: {}", variant, e.toString());
            return new GiantStructure(List.of(), new int[0], new int[0]);
        }
    }

    /** 控制器结构内偏移(声明显式优先，否则找 GiantControllerBlock)。 */
    public int[] controllerOffset() {
        if (controller != null && controller.length == 3) return controller;
        for (Placed p : blocks) {
            if (p.block() instanceof com.srqingchen.fieldworks.block.GiantControllerBlock) {
                return new int[]{p.x(), p.y(), p.z()};
            }
        }
        return new int[]{0, 0, 0};
    }

    /** 建造材料清单(方块→数量，控制器除外：由巨物核心代表)。 */
    public List<Block> materialBlocks() {
        List<Block> list = new ArrayList<>();
        for (Placed p : blocks) {
            if (p.block() instanceof com.srqingchen.fieldworks.block.GiantControllerBlock) continue;
            if (p.block() == Blocks.AIR) continue;
            list.add(p.block());
        }
        return list;
    }

    public boolean hasController() {
        for (Placed p : blocks) {
            if (p.block() instanceof com.srqingchen.fieldworks.block.GiantControllerBlock) return true;
        }
        return false;
    }
}
