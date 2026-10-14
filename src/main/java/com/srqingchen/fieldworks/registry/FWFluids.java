package com.srqingchen.fieldworks.registry;

import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 气体/流体注册(§10.5 全部流体化气体 + 蒸汽/稀硫酸)。
 * 不可放置的纯罐装流体：管道/储罐/机器兼容，无世界形态(§1.3 克制原则)。
 */
public final class FWFluids {
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, Fieldworks.MODID);

    // 名称即流体 id(fieldworks:xxx)
    public static final DeferredHolder<Fluid, GasFluid> COMPRESSED_AIR = register("compressed_air");
    public static final DeferredHolder<Fluid, GasFluid> OXYGEN = register("oxygen");
    public static final DeferredHolder<Fluid, GasFluid> NITROGEN = register("nitrogen");
    public static final DeferredHolder<Fluid, GasFluid> HYDROGEN = register("hydrogen");
    public static final DeferredHolder<Fluid, GasFluid> MARSH_GAS = register("marsh_gas");
    public static final DeferredHolder<Fluid, GasFluid> CARBON_DIOXIDE = register("carbon_dioxide");
    public static final DeferredHolder<Fluid, GasFluid> INERT_GAS = register("inert_gas");
    public static final DeferredHolder<Fluid, GasFluid> SULFURIC_DILUTE = register("sulfuric_dilute");
    public static final DeferredHolder<Fluid, GasFluid> STEAM = register("steam");
    public static final DeferredHolder<Fluid, GasFluid> VOID_ESSENCE = register("void_essence");
    public static final DeferredHolder<Fluid, GasFluid> TUNED_VOID_GAS = register("tuned_void_gas");
    public static final DeferredHolder<Fluid, GasFluid> NETHER_GAS = register("nether_gas");

    private static DeferredHolder<Fluid, GasFluid> register(String name) {
        return FLUIDS.register(name, () -> new GasFluid(name));
    }

    private FWFluids() {}
}
