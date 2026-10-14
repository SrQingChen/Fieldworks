package com.srqingchen.fieldworks;

import com.srqingchen.fieldworks.carbon.CarbonState;
import com.srqingchen.fieldworks.command.FWCommands;
import com.srqingchen.fieldworks.config.FWConfig;
import com.srqingchen.fieldworks.envelope.EnvelopeManager;
import com.srqingchen.fieldworks.furnace.ContinuousFiringManager;
import com.srqingchen.fieldworks.harmonic.HarmonicEvents;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import com.srqingchen.fieldworks.registry.FWBlocks;
import com.srqingchen.fieldworks.registry.FWCreativeTabs;
import com.srqingchen.fieldworks.registry.FWDataComponents;
import com.srqingchen.fieldworks.registry.FWFluids;
import com.srqingchen.fieldworks.registry.FWFluidTypes;
import com.srqingchen.fieldworks.registry.FWItems;
import com.srqingchen.fieldworks.registry.FWMenus;
import com.srqingchen.fieldworks.registry.FWRecipes;
import com.srqingchen.fieldworks.tame.TameEvents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 场域工艺 Fieldworks 主入口。
 * 场引擎(§6)是全局单例 {@link com.srqingchen.fieldworks.fieldengine.FieldEngine}。
 */
@Mod(Fieldworks.MODID)
public final class Fieldworks {
    public static final String MODID = "fieldworks";
    public static final Logger LOGGER = LoggerFactory.getLogger("Fieldworks");

    public Fieldworks(IEventBus modEventBus, ModContainer modContainer) {
        // ── 注册表(顺序：blocks → items → BEs，其余任意) ──
        FWBlocks.BLOCKS.register(modEventBus);
        FWFluidTypes.TYPES.register(modEventBus);
        FWFluids.FLUIDS.register(modEventBus);
        FWBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        FWItems.ITEMS.register(modEventBus);
        FWMenus.MENUS.register(modEventBus);
        FWCreativeTabs.TABS.register(modEventBus);
        FWRecipes.RECIPE_TYPES.register(modEventBus);
        FWRecipes.RECIPE_SERIALIZERS.register(modEventBus);
        FWDataComponents.DATA_COMPONENTS.register(modEventBus);

        // ── MOD bus 事件 ──
        modEventBus.addListener(this::registerCapabilities);
        modEventBus.addListener(com.srqingchen.fieldworks.network.FWPackets::register);

        // ── 服务端逻辑(Game bus) ──
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.fieldengine.FieldEngine::onServerTick);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.fieldengine.FieldEngine::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.fieldengine.FieldEngine::onChunkUnload);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.fieldengine.FieldEngine::onBlockPlaced);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.fieldengine.FieldEngine::onBlockBroken);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.fieldengine.FieldEngine::onServerStarted);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.fieldengine.FieldEngine::onServerStopping);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.fieldengine.EngineSavedData::onReloadListeners);
        NeoForge.EVENT_BUS.addListener(ReloadListeners0::register);

        NeoForge.EVENT_BUS.addListener(CarbonState::onExplosion);
        NeoForge.EVENT_BUS.addListener(ContinuousFiringManager::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(ContinuousFiringManager::onServerTick);
        NeoForge.EVENT_BUS.addListener(HarmonicEvents::onEnchantLevelSet);
        NeoForge.EVENT_BUS.addListener(HarmonicEvents::onPlayerEnchantItem);
        NeoForge.EVENT_BUS.addListener(HarmonicEvents::onAnvilUpdate);
        NeoForge.EVENT_BUS.addListener(TameEvents::onLivingDrops);
        NeoForge.EVENT_BUS.addListener(FWCommands::register);
        // 场学饰品(261014 T10：Curios 软依赖，未装时各处理器自然空转)
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.compat.curios.AccessoryEvents::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.compat.curios.AccessoryEvents::onDamagePre);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.compat.curios.AccessoryEvents::onDamagePost);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.compat.curios.AccessoryEvents::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.compat.curios.AccessoryEvents::onLivingFall);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.book.GuideBook::onItemCrafted);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.book.GuideBook::onPlayerLoggedIn);
        NeoForge.EVENT_BUS.addListener(com.srqingchen.fieldworks.book.GuideBook::onPlayerTick);

        // 人工场包络 override 挂钩(§6.5 L3)
        EnvelopeManager.install();

        modContainer.registerConfig(ModConfig.Type.COMMON, FWConfig.SPEC);
        LOGGER.info("[Fieldworks] 场域工艺装载完毕 — 大地即将显谱。");
        if (net.neoforged.fml.ModList.get().isLoaded("patchouli")) {
            LOGGER.info("[Fieldworks] Patchouli 就绪：《场域学入门》(patchouli:guide_book) 可获取 —— " +
                    "创造栏 fieldworks 标签末尾/搜索「场域学」；合成=木板环+书+纸；首次合成探测杖附赠。" +
                    "快捷命令: /give @s patchouli:guide_book[patchouli:book=fieldworks:manual]");
        } else {
            LOGGER.warn("[Fieldworks] 未检测到 Patchouli：游戏内手册不可用(软依赖，需 patchouli-neoforge-26.1-94.jar)。");
        }
    }

    /** 能力注册(§15.1 对标 AE2 现代范式)：能量 + 流体经新 transfer API。 */
    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        // 全部工艺机器：能量 + 流体
        event.registerBlockEntity(Capabilities.Energy.BLOCK, FWBlockEntities.MACHINE.get(),
                (be, side) -> be.energy != null ? be.energy : null);
        event.registerBlockEntity(Capabilities.Fluid.BLOCK, FWBlockEntities.MACHINE.get(),
                (be, side) -> be.tanks.isEmpty() ? null : be.tanks.get(0));
        // 巨物：能量
        event.registerBlockEntity(Capabilities.Energy.BLOCK, FWBlockEntities.GIANT.get(),
                (be, side) -> be.energy);
        // 场输入器：能量
        event.registerBlockEntity(Capabilities.Energy.BLOCK, FWBlockEntities.FIELD_PART.get(),
                (be, side) -> be.energy);
    }

    /** 数据包重载监听(§19.3)。 */
    static final class ReloadListeners0 {
        static void register(net.neoforged.neoforge.event.AddServerReloadListenersEvent event) {
            com.srqingchen.fieldworks.fieldengine.ReloadListeners.register(event);
        }
    }

    public static net.minecraft.resources.Identifier id(String path) {
        return net.minecraft.resources.Identifier.fromNamespaceAndPath(MODID, path);
    }
}
