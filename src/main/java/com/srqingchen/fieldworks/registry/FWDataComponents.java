package com.srqingchen.fieldworks.registry;

import com.mojang.serialization.Codec;
import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 数据组件：气体纯度(§10.6 纯度体系，0—100，三档 粗炼<60/精炼60—90/超纯≥90)。
 */
public final class FWDataComponents {
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Fieldworks.MODID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> GAS_PURITY =
            DATA_COMPONENTS.register("gas_purity", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.intRange(0, 100))
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    /** 蓝图巨物变体(§13：fieldworks:giant_variant → resonance_tower/...)。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.minecraft.resources.Identifier>> GIANT_VARIANT =
            DATA_COMPONENTS.register("giant_variant", () -> DataComponentType.<net.minecraft.resources.Identifier>builder()
                    .persistent(net.minecraft.resources.Identifier.CODEC)
                    .networkSynchronized(net.minecraft.resources.Identifier.STREAM_CODEC)
                    .build());

    /** 饰品内置电容(261014 T10：超导谐律核心 等，存 FE)。 */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Long>> STORED_ENERGY =
            DATA_COMPONENTS.register("stored_energy", () -> DataComponentType.<Long>builder()
                    .persistent(Codec.LONG)
                    .networkSynchronized(ByteBufCodecs.VAR_LONG)
                    .build());

    private FWDataComponents() {}
}
