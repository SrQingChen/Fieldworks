package com.srqingchen.fieldworks.api.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidInstance;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;
import java.util.Optional;

/**
 * 通用机器配方(§7.1)：一条 JSON 声明 输入/输出/能量/耗时/场档案/可用机器。
 * 通用机器配方记录(§7.1)：一条 JSON 声明 输入/输出/能量/耗时/场档案/可用机器。
 * 261013 T2 起按配方族分型注册(AE2 式)：电解族=fieldworks:electrolysis 等 18 个类型，
 * machines 字段白名单控制同族内的机器分级(数据包可扩，§19.3)。
 *
 * <p>26.1.2 关键约束：产物/流体输出必须用"延迟组件绑定"的类型
 * ({@link ItemStackTemplate} / {@link FluidTemplate})——数据包装载期物品/流体
 * 组件尚未绑定，直接用 ItemStack/FluidStack CODEC 会报
 * "does not have components yet" 而整条配方被丢弃(2026-10-12 修复)。</p>
 *
 * <p>JSON 形态：</p>
 * <pre>{@code
 * {
 *   "type": "fieldworks:electrolysis",  // 261013 T2：按配方族取类型(18 族, 见 MachineRecipeFamily)
 *   "machines": ["fieldworks:electrolyzer_1"],
 *   "inputs": ["fieldworks:compressed_air"],
 *   "outputs": [{"id": "fieldworks:steel_ingot", "count": 1, "chance": 1.0}],
 *   "energy": 800, "time": 200,
 *   "field_profile": { "require": "fieldworks:oxygen_rich", "min_ox": 15,
 *       "fields": [{"field": "fieldworks:temperature", "ideal": 20, "tolerance": 30, "penalty": 0.5, "weight": 1.0}] },
 *   "atmosphere_cost": {"ox": -0.01},
 *   "fluid_input": {"fluid": "minecraft:water", "amount": 200},
 *   "fluid_outputs": [{"id": "fieldworks:oxygen", "amount": 1000}]
 * }
 * }</pre>
 */
public record MachineRecipe(
        List<Identifier> machines,
        List<InputEntry> inputs,
        List<ItemStackTemplate> outputs,
        List<Float> chances,
        int energy,
        int time,
        FieldProfile profile,
        Optional<AtmosphereCost> atmosphereCost,
        Optional<FluidSpec> fluidInput,
        List<FluidTemplate> fluidOutputs,
        MachineRecipeFamily family) implements Recipe<RecipeInput> {

    /**
     * 计数配料(261014 T6)：一个输入槽位声明 = 成分 × 数量。
     * JSON 兼容两种形态：纯字符串 {@code "minecraft:iron_ingot"}(count=1) 与
     * {@code {"ingredient": "minecraft:iron_ingot", "count": 16}}——
     * 复合配方(如"铁锭×16")无需 16 个槽位，可跨背包堆叠清点。
     */
    public record InputEntry(Ingredient ingredient, int count) {
        public static final Codec<InputEntry> CODEC = Codec.either(
                        Ingredient.CODEC,
                        RecordCodecBuilder.<InputEntry>create(inst -> inst.group(
                                Ingredient.CODEC.fieldOf("ingredient").forGetter(InputEntry::ingredient),
                                Codec.intRange(1, 128).optionalFieldOf("count", 1).forGetter(InputEntry::count)
                        ).apply(inst, InputEntry::new)))
                .xmap(e -> e.left().map(i -> new InputEntry(i, 1)).orElseGet(e.right()::get),
                        entry -> entry.count() <= 1
                                ? com.mojang.datafixers.util.Either.left(entry.ingredient())
                                : com.mojang.datafixers.util.Either.right(entry));

        public boolean test(ItemStack stack) {
            return ingredient.test(stack);
        }
    }

    /** 流体输入声明：放什么流体、多少 mB。 */
    public record FluidSpec(Identifier fluid, int amount) {
        public static final Codec<FluidSpec> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Identifier.CODEC.fieldOf("fluid").forGetter(FluidSpec::fluid),
                Codec.INT.optionalFieldOf("amount", 100).forGetter(FluidSpec::amount)
        ).apply(inst, FluidSpec::new));
    }

    /** 流体输出模板(装载期延迟绑定，运行时 {@link #stack()} 物化)。 */
    public record FluidTemplate(Holder<Fluid> fluid, int amount) {
        public static final Codec<FluidTemplate> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                FluidInstance.FLUID_HOLDER_CODEC.fieldOf("id").forGetter(FluidTemplate::fluid),
                Codec.intRange(1, Integer.MAX_VALUE).fieldOf("amount").forGetter(FluidTemplate::amount)
        ).apply(inst, FluidTemplate::new));

        public FluidStack stack() {
            return new FluidStack(fluid, amount);
        }
    }

    /** 大气消耗三模式(§7.3)：消耗型(负)/排放型(正)/无关型(empty)。 */
    public record AtmosphereCost(float ox, float fg, float cr) {
        public static final Codec<AtmosphereCost> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.FLOAT.optionalFieldOf("ox", 0f).forGetter(AtmosphereCost::ox),
                Codec.FLOAT.optionalFieldOf("fg", 0f).forGetter(AtmosphereCost::fg),
                Codec.FLOAT.optionalFieldOf("cr", 0f).forGetter(AtmosphereCost::cr)
        ).apply(inst, AtmosphereCost::new));
    }

    public static final MapCodecHolder MAP = new MapCodecHolder();

    public static final class MapCodecHolder {
        /** 基础编解码(不含族字段；族由各配方族的 serializer 烙印，AE2 每类一 Recipe 的等价轻量做法)。 */
        public final com.mojang.serialization.MapCodec<MachineRecipe> codec = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Codec.list(Identifier.CODEC).fieldOf("machines").forGetter(MachineRecipe::machines),
                Codec.list(InputEntry.CODEC).optionalFieldOf("inputs", List.of()).forGetter(MachineRecipe::inputs),
                Codec.list(ItemStackTemplate.CODEC).optionalFieldOf("outputs", List.of()).forGetter(MachineRecipe::outputs),
                Codec.list(Codec.FLOAT).optionalFieldOf("chances", List.of()).forGetter(MachineRecipe::chances),
                Codec.INT.optionalFieldOf("energy", 200).forGetter(MachineRecipe::energy),
                Codec.INT.optionalFieldOf("time", 200).forGetter(MachineRecipe::time),
                FieldProfile.CODEC.optionalFieldOf("field_profile", FieldProfile.EMPTY).forGetter(MachineRecipe::profile),
                AtmosphereCost.CODEC.optionalFieldOf("atmosphere_cost").forGetter(MachineRecipe::atmosphereCost),
                FluidSpec.CODEC.optionalFieldOf("fluid_input").forGetter(MachineRecipe::fluidInput),
                Codec.list(FluidTemplate.CODEC).optionalFieldOf("fluid_outputs", List.of()).forGetter(MachineRecipe::fluidOutputs)
        ).apply(inst, MachineRecipe::base));

        /** 族烙印编解码：解码出的配方带上族引用(编码时族字段不参与，镜像恒等)。 */
        public com.mojang.serialization.MapCodec<MachineRecipe> forFamily(MachineRecipeFamily fam) {
            return codec.xmap(r -> r.withFamily(fam), r -> r);
        }

        /** 完整同步(含场档案与流体)：经 map codec 的注册表感知编码，JEI 客户端可拿到全部信息。 */
        public final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> stream =
                ByteBufCodecs.fromCodecWithRegistries(codec.codec());
    }

    /** 反序列化占位构造(family 由 forFamily 烙印)。 */
    static MachineRecipe base(List<Identifier> machines, List<InputEntry> inputs, List<ItemStackTemplate> outputs,
                              List<Float> chances, int energy, int time, FieldProfile profile,
                              Optional<AtmosphereCost> atmosphereCost, Optional<FluidSpec> fluidInput,
                              List<FluidTemplate> fluidOutputs) {
        return new MachineRecipe(machines, inputs, outputs, chances, energy, time, profile,
                atmosphereCost, fluidInput, fluidOutputs, null);
    }

    MachineRecipe withFamily(MachineRecipeFamily fam) {
        return new MachineRecipe(machines, inputs, outputs, chances, energy, time, profile,
                atmosphereCost, fluidInput, fluidOutputs, fam);
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        // 机器配方匹配在 BlockEntity 侧手工执行(配方集按 machine id 预筛)
        return false;
    }

    @Override
    public String group() {
        return "fieldworks";
    }

    @Override
    public ItemStack assemble(RecipeInput input) {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.getFirst().create();
    }

    @Override
    public net.minecraft.world.item.crafting.PlacementInfo placementInfo() {
        return net.minecraft.world.item.crafting.PlacementInfo.create(
                inputs.stream().map(InputEntry::ingredient).toList());
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeBookCategory recipeBookCategory() {
        return new net.minecraft.world.item.crafting.RecipeBookCategory();
    }

    @Override
    public boolean isSpecial() { return true; }

    @Override
    public boolean showNotification() { return false; }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeSerializer<? extends Recipe<RecipeInput>> getSerializer() {
        return family != null ? family.serializer()
                : (RecipeSerializer) com.srqingchen.fieldworks.registry.FWRecipes.serializerOfAny();
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public RecipeType<? extends Recipe<RecipeInput>> getType() {
        return family != null ? (RecipeType) family.type()
                : MachineRecipeFamily.ELECTROLYSIS.type();
    }

    public boolean fitsMachine(Identifier machineId) {
        return machines.contains(machineId);
    }
}
