package com.srqingchen.fieldworks.registry;

import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 创造标签：按五进度线分组(§8.1)。 */
public final class FWCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Fieldworks.MODID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.fieldworks.main"))
                    .icon(() -> new ItemStack(FWItems.PROBE_STAFF.get()))
                    .displayItems((params, output) -> {
                        // 教程书《场域学入门》放标签第一位(新手入口)。
                        // 经注册表查找构造 → 零 Patchouli 类加载耦合；未装 Patchouli 时静默跳过。
                        // 261012: 显式注册取代 book.json creative_tab 注入(EdenProtocol 仅靠搜索页，
                        // 用户实测反馈标签页里必须可见)。
                        var bookItem = BuiltInRegistries.ITEM.getValue(Identifier.parse("patchouli:guide_book"));
                        var bookComp = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse("patchouli:book"));
                        if (bookItem != null && bookComp != null) {
                            ItemStack book = new ItemStack(bookItem);
                            book.set((DataComponentType<Identifier>) (DataComponentType<?>) bookComp,
                                    Identifier.fromNamespaceAndPath(Fieldworks.MODID, "manual"));
                            output.accept(book);
                        }
                        // 工具与部件
                        output.accept(FWItems.PROBE_STAFF.get());
                        output.accept(FWItems.SPECTRUM_MODULE.get());
                        output.accept(FWItems.WRENCH.get());
                        output.accept(FWItems.FIELDPACK_TERMINAL.get());
                        output.accept(FWItems.TIDAL_CALENDAR.get());
                        // 全部物品(含 BlockItem；技术方块投影除外)
                        FWItems.ITEMS.getEntries().stream()
                                .filter(i -> !i.getId().getPath().equals("projection"))
                                .forEach(i -> output.accept(i.get()));
                    })
                    .build());

    private FWCreativeTabs() {}
}
