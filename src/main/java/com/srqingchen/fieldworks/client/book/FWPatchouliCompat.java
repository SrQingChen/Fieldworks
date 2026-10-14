package com.srqingchen.fieldworks.client.book;

import com.srqingchen.fieldworks.Fieldworks;
import net.neoforged.fml.ModList;

/**
 * Patchouli 软依赖装配(261013 规划 T5)：
 * 注册自定义书页类型 {@code fieldworks:multiblock}(继承官方多方块页，
 * 世界投影换接到正确的 {@link FWMultiblockGhost})。
 * 本类仅在 Patchouli 在场时加载(NoClassDefFoundError 由调用方捕获)。
 */
public final class FWPatchouliCompat {
    public static void init() {
        // 1) 自定义书页类型(官方 ClientBookRegistry.pageTypes 为公开可扩展映射)
        vazkii.patchouli.client.book.ClientBookRegistry.INSTANCE.pageTypes
                .put(Fieldworks.id("multiblock"), com.srqingchen.fieldworks.book.FWPageMultiblock.class);
        // 2) 世界投影处理器(SubmitCustomGeometryEvent 正确相机相对渲染)
        FWMultiblockGhost.register();
        Fieldworks.LOGGER.info("[Fieldworks] Patchouli 书页多方块投影已接管(fieldworks:multiblock)");
    }

    /** 客户端入口安全调用：未装 Patchouli 静默跳过。 */
    public static void initIfPresent() {
        if (!ModList.get().isLoaded("patchouli")) return;
        try {
            init();
        } catch (Throwable t) {
            Fieldworks.LOGGER.warn("[Fieldworks] Patchouli 兼容初始化失败(不影响其他功能): {}", t.toString());
        }
    }

    private FWPatchouliCompat() {}
}
