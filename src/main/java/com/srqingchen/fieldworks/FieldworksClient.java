package com.srqingchen.fieldworks;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonParser;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.srqingchen.fieldworks.client.gui.EnergyUnit;
import com.srqingchen.fieldworks.client.gui.MachineScreen;
import com.srqingchen.fieldworks.registry.FWMenus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import java.util.Map;

/**
 * 客户端入口：屏幕绑定 + 样式 JSON 装载(AE2 式，§15.1/§15.2)。
 * 样式层：assets/fieldworks/screens/<machine>.json —— 布局/贴图引用/调色板；
 * 资源包换肤零代码(§19.3)。
 */
@Mod(value = Fieldworks.MODID, dist = Dist.CLIENT)
public final class FieldworksClient {

    public FieldworksClient(IEventBus modEventBus, ModContainer container) {
        // 能量显示单位偏好(261014 T4：FE/RF/J 点击切换)
        EnergyUnit.load();
        modEventBus.addListener((RegisterMenuScreensEvent e) ->
                e.register(FWMenus.MACHINE.get(), MachineScreen::new));
        // 管道运载内容色芯 BER(§T6；261014 T2 三族：流体/物品/能量)
        modEventBus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e) -> {
            e.registerBlockEntityRenderer(
                    com.srqingchen.fieldworks.registry.FWBlockEntities.PIPE.get(),
                    com.srqingchen.fieldworks.client.render.PipeContentRenderer::new);
            e.registerBlockEntityRenderer(
                    com.srqingchen.fieldworks.registry.FWBlockEntities.ITEM_PIPE.get(),
                    com.srqingchen.fieldworks.client.render.PipeContentRenderer.ItemPipeRenderer::new);
            e.registerBlockEntityRenderer(
                    com.srqingchen.fieldworks.registry.FWBlockEntities.ENERGY_PIPE.get(),
                    com.srqingchen.fieldworks.client.render.PipeContentRenderer.EnergyPipeRenderer::new);
        });
        // 流体模型注册(26.x 必需：缺失则日志刷 Missing FluidModel 且流体渲染为缺失贴图)
        modEventBus.addListener(this::registerFluidModels);
        // Patchouli 书页多方块投影接管(261013 T5：未装 Patchouli 自动跳过)
        com.srqingchen.fieldworks.client.book.FWPatchouliCompat.initIfPresent();
    }

    /** 为全部气体注册 FluidModel(vanilla water 同款 Unbaked 构造，§10.5)。 */
    private void registerFluidModels(net.neoforged.neoforge.client.event.RegisterFluidModelsEvent event) {
        for (var entry : com.srqingchen.fieldworks.registry.FWFluids.FLUIDS.getEntries()) {
            String name = entry.getId().getPath();
            event.register(new net.minecraft.client.renderer.block.FluidModel.Unbaked(
                    new net.minecraft.client.resources.model.sprite.Material(
                            Fieldworks.id("block/fluid_" + name + "_still")),
                    new net.minecraft.client.resources.model.sprite.Material(
                            Fieldworks.id("block/fluid_" + name + "_flow")),
                    null, null), entry.get());
        }
    }

    /** GUI 贴图绘制管线(26.1.2 RenderPipelines.GUI_TEXTURED)。 */
    public static final RenderPipeline GUI_PIPELINE = RenderPipelines.GUI_TEXTURED;

    /**
     * 样式装载器(§15.2)：assets/fieldworks/screens/*.json。
     * Schema(对标 AE2 condenser.json 精简版)：
     * background.texture / background.srcRect / palette{} / text{}
     */
    public static final class ScreenStyle {
        public String backgroundTexture = "textures/guis/machine.png";
        public int[] srcRect = {0, 0, 176, 166};
        public int progressX = 79, progressY = 35, progressW = 24, progressH = 17;
        public int energyX = 7, energyY = 20, energyW = 12, energyH = 40;
        public int fitX = 97, fitY = 54;
        public Map<String, Integer> palette = Map.of(
                "ACCENT", 0xE8A33D,
                "FIELD", 0x4FC3D9,
                "GOOD", 0x6BCB77,
                "WARN", 0xE8C53D,
                "BAD", 0xE8853D);

        public static final Codec<ScreenStyle> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.STRING.optionalFieldOf("texture", "textures/guis/machine.png").forGetter(s -> s.backgroundTexture),
                Codec.INT.listOf().optionalFieldOf("src_rect", java.util.List.of(0, 0, 176, 166)).forGetter(s -> java.util.List.of(s.srcRect[0], s.srcRect[1], s.srcRect[2], s.srcRect[3])),
                Codec.INT.optionalFieldOf("progress_x", 79).forGetter(s -> s.progressX),
                Codec.INT.optionalFieldOf("progress_y", 35).forGetter(s -> s.progressY),
                Codec.INT.optionalFieldOf("progress_w", 24).forGetter(s -> s.progressW),
                Codec.INT.optionalFieldOf("progress_h", 17).forGetter(s -> s.progressH),
                Codec.INT.optionalFieldOf("energy_x", 7).forGetter(s -> s.energyX),
                Codec.INT.optionalFieldOf("energy_y", 20).forGetter(s -> s.energyY),
                Codec.INT.optionalFieldOf("fit_x", 97).forGetter(s -> s.fitX),
                Codec.INT.optionalFieldOf("fit_y", 54).forGetter(s -> s.fitY),
                Codec.unboundedMap(Codec.STRING, Codec.INT).optionalFieldOf("palette", Map.of("ACCENT", 0xE8A33D)).forGetter(s -> s.palette)
        ).apply(inst, (tex, rect, px, py, pw, ph, ex, ey, fx, fy, pal) -> {
            var s = new ScreenStyle();
            s.backgroundTexture = tex;
            s.srcRect = rect.stream().mapToInt(Integer::intValue).toArray();
            s.progressX = px;
            s.progressY = py;
            s.progressW = pw;
            s.progressH = ph;
            s.energyX = ex;
            s.energyY = ey;
            s.fitX = fx;
            s.fitY = fy;
            s.palette = pal;
            return s;
        }));
    }

    /** 样式缓存(打开 GUI 时按 machine id 懒加载)。 */
    private static final Map<String, ScreenStyle> STYLES = new java.util.concurrent.ConcurrentHashMap<>();

    public static ScreenStyle style(String machineId) {
        return STYLES.computeIfAbsent(machineId, FieldworksClient::loadStyle);
    }

    private static ScreenStyle loadStyle(String machineId) {
        ResourceManager rm = net.minecraft.client.Minecraft.getInstance().getResourceManager();
        var id = Identifier.fromNamespaceAndPath(Fieldworks.MODID, "screens/" + machineId + ".json");
        try (var is = rm.open(id)) {
            var json = JsonParser.parseReader(new java.io.InputStreamReader(is));
            var opt = ScreenStyle.CODEC.parse(JsonOps.INSTANCE, json).result();
            if (opt.isPresent()) return opt.get();
        } catch (Exception ignored) {
            // 回退默认样式(§15.2 样式层缺失不致命)
        }
        return new ScreenStyle();
    }

    /** Blitter(§15.1 对标 AE2)：链式源矩形/目标矩形贴片绘制。 */
    public static final class Blitter {
        private final Identifier texture;
        private final int u, v, w, h, texW, texH;
        private int color = -1;

        private Blitter(Identifier texture, int u, int v, int w, int h, int texW, int texH) {
            this.texture = texture;
            this.u = u;
            this.v = v;
            this.w = w;
            this.h = h;
            this.texW = texW;
            this.texH = texH;
        }

        public static Blitter of(Identifier texture, int u, int v, int w, int h, int texW, int texH) {
            return new Blitter(texture, u, v, w, h, texW, texH);
        }

        public Blitter color(int argb) {
            this.color = argb;
            return this;
        }

        public void blit(GuiGraphicsExtractor g, int x, int y) {
            g.blit(GUI_PIPELINE, texture, x, y, u, v, w, h, texW, texH, color);
        }
    }
}
