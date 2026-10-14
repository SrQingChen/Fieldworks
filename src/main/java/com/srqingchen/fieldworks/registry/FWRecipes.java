package com.srqingchen.fieldworks.registry;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.recipe.MachineRecipe;
import com.srqingchen.fieldworks.api.recipe.MachineRecipeFamily;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 配方族注册(§7.1 / 261013 规划 T2，对标 AE2 AERecipeTypes：
 * 每个配方族一个 RecipeType + Serializer)。
 * 族清单唯一事实源在 {@link MachineRecipeFamily}；
 * 各族 serializer 复用同一基础 MapCodec，经 forFamily 烙印族引用。
 */
public final class FWRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, Fieldworks.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, Fieldworks.MODID);

    private static final Map<MachineRecipeFamily, DeferredHolder<RecipeType<?>, RecipeType<MachineRecipe>>> TYPES =
            new LinkedHashMap<>();
    private static final Map<MachineRecipeFamily, DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MachineRecipe>>> SERIALIZERS =
            new LinkedHashMap<>();

    static {
        for (MachineRecipeFamily fam : MachineRecipeFamily.all()) {
            TYPES.put(fam, RECIPE_TYPES.register(fam.path,
                    () -> RecipeType.simple(Fieldworks.id(fam.path))));
            SERIALIZERS.put(fam, RECIPE_SERIALIZERS.register(fam.path, () -> new RecipeSerializer<>(
                    MachineRecipe.MAP.forFamily(fam),
                    ByteBufCodecs.fromCodecWithRegistries(MachineRecipe.MAP.forFamily(fam).codec()))));
        }
    }

    @SuppressWarnings("unchecked")
    public static RecipeType<MachineRecipe> typeOf(MachineRecipeFamily fam) {
        return (RecipeType<MachineRecipe>) TYPES.get(fam).get();
    }

    @SuppressWarnings("unchecked")
    public static RecipeSerializer<MachineRecipe> serializerOf(MachineRecipeFamily fam) {
        return (RecipeSerializer<MachineRecipe>) SERIALIZERS.get(fam).get();
    }

    /** family 缺失时的兜底序列化器(首个族；正常路径不触发)。 */
    public static RecipeSerializer<MachineRecipe> serializerOfAny() {
        return serializerOf(MachineRecipeFamily.ELECTROLYSIS);
    }

    /** 按 "type" JSON id 反查族(诊断/审计用)。 */
    public static RecipeType<MachineRecipe> typeById(Identifier typeId) {
        var fam = MachineRecipeFamily.fromId(typeId);
        return fam != null ? typeOf(fam) : null;
    }

    private FWRecipes() {}
}
