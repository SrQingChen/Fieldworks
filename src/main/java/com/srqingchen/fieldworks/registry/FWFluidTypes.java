package com.srqingchen.fieldworks.registry;

import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * FluidType 注册(26.1.2 不可省略)：每个气体一个类型，颜色驱动 UI/管道气泡可读性(§10.5)。
 */
public final class FWFluidTypes {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, Fieldworks.MODID);

    private record Def(int color, String desc) {}

    private static final Map<String, Def> DEFS = new HashMap<>();

    private static void def(String name, int color) {
        DEFS.put(name, new Def(color, "fluid_type.fieldworks." + name));
        TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId(DEFS.get(name).desc())
                .rarity(Rarity.COMMON)
                .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)));
    }

    static {
        def("compressed_air", 0xFFB8C9D9);
        def("oxygen", 0xFF6EC6FF);          // 冷青蓝
        def("nitrogen", 0xFF9FB8AD);
        def("hydrogen", 0xFFE5F5A0);        // 浅黄绿
        def("marsh_gas", 0xFF7FA05A);       // 沼绿
        def("carbon_dioxide", 0xFF8A8A8A);
        def("inert_gas", 0xFFC9A7E3);       // 惰紫
        def("sulfuric_dilute", 0xFFC8B55A); // 硫浊黄
        def("steam", 0xFFD7E4EC);
        def("void_essence", 0xFF2B1E3D);    // 虚深紫
        def("tuned_void_gas", 0xFF6D55C9);  // 调谐紫
        def("nether_gas", 0xFFB84A2B);      // 下界焰橙
    }

    /** GasFluid 构造取类型用(注册顺序保证)。 */
    public static FluidType byId(String name) {
        for (var e : TYPES.getEntries()) {
            if (e.getId().getPath().equals(name)) return e.get();
        }
        return TYPES.getEntries().iterator().next().get();
    }

    public static int colorOf(FluidType type) {
        var key = NeoForgeRegistries.FLUID_TYPES.getResourceKey(type);
        if (key.isEmpty()) return 0xFFB8C9D9;
        Def d = DEFS.get(key.get().identifier().getPath());
        return d == null ? 0xFFB8C9D9 : d.color();
    }

    private FWFluidTypes() {}
}
