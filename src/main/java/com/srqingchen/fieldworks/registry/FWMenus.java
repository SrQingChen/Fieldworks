package com.srqingchen.fieldworks.registry;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.machine.MachineMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** 菜单类型注册。 */
public final class FWMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Fieldworks.MODID);

    /** 通用机器菜单(buf 携带 pos)。 */
    public static final DeferredHolder<MenuType<?>, MenuType<MachineMenu>> MACHINE =
            MENUS.register("machine", () -> IMenuTypeExtension.create(MachineMenu::new));

    private FWMenus() {}
}
