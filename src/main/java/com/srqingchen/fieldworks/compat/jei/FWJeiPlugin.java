package com.srqingchen.fieldworks.compat.jei;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.recipe.MachineRecipe;
import com.srqingchen.fieldworks.api.recipe.MachineRecipeFamily;
import com.srqingchen.fieldworks.client.gui.MachineScreen;
import com.srqingchen.fieldworks.client.gui.widget.FWWidgets;
import com.srqingchen.fieldworks.machine.MachineLayout;
import com.srqingchen.fieldworks.machine.MachineSpec;
import com.srqingchen.fieldworks.machine.MachineSpecs;
import com.srqingchen.fieldworks.registry.FWBlocks;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.drawable.IDrawableAnimated;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI 联动 v2(261013 规划 T3，对标 AE2 InscriberRecipeCategory/TransformCategory)：
 * <ul>
 *   <li><b>每个配方族一个类目</b>(电解族 4 机一类目，其余机器各自成族)——与注册表
 *       RecipeType 一一对应({@link MachineRecipeFamily})</li>
 *   <li>槽位一律 JEI 标准槽框/标准流体渲染器({@code setStandardSlotBackground} 等，
 *       物品与框零错位；AE2 同款)</li>
 *   <li>布局镜像机器 GUI 模板：物品族=左能量井+横排输入+箭头+右列输出；
 *       流体族=左入计量槽+BI 双向箭头+右出计量槽(多)+底部物品行——与
 *       {@link MachineLayout} TANK/WIDE 排布同构</li>
 *   <li>配方源 = JEI 客户端同步表(空集回落本地 RecipeManager)</li>
 * </ul>
 */
@JeiPlugin
public class FWJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Fieldworks.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        for (MachineRecipeFamily fam : MachineRecipeFamily.all()) {
            registration.addRecipeCategories(new FamilyCategory(registration.getJeiHelpers().getGuiHelper(), fam));
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (MachineRecipeFamily fam : MachineRecipeFamily.all()) {
            var recipes = new ArrayList<RecipeHolder<MachineRecipe>>();
            try {
                recipes.addAll(mezz.jei.common.Internal.getClientSyncedRecipes().byType(fam.type()));
            } catch (Throwable ignored) {
                // JEI 内部 API 不可用时回落
            }
            if (recipes.isEmpty()) {
                var level = net.minecraft.client.Minecraft.getInstance().level;
                if (level != null && level.recipeAccess() instanceof net.minecraft.world.item.crafting.RecipeManager rm) {
                    recipes.addAll(rm.recipeMap().byType(fam.type()));
                }
            }
            if (!recipes.isEmpty()) {
                registration.addRecipes(RecipeType.createFromVanilla(fam.type()), recipes);
            }
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (MachineRecipeFamily fam : MachineRecipeFamily.all()) {
            var category = RecipeType.createFromVanilla(fam.type());
            for (var machineId : fam.machines) {
                var block = BuiltInRegistries.BLOCK.getValue(machineId);
                if (block != null && FWBlocks.BLOCKS.getEntries().stream()
                        .anyMatch(b -> b.getId().equals(machineId))) {
                    registration.addRecipeCatalyst(new ItemStack(block), category);
                }
            }
        }
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // 窗外部件排除区(Fit 徽章左上外侧 + 升级面板右外侧)：JEI 配方面板避让(AE2 同协议)
        registration.addGenericGuiContainerHandler(MachineScreen.class,
                new mezz.jei.api.gui.handlers.IGuiContainerHandler<MachineScreen>() {
                    @Override
                    public List<net.minecraft.client.renderer.Rect2i> getGuiExtraAreas(MachineScreen s) {
                        List<net.minecraft.client.renderer.Rect2i> areas = new ArrayList<>();
                        areas.add(s.fitBadgeArea());
                        var upg = s.upgradePanelArea();
                        if (upg != null) areas.add(upg);
                        return areas;
                    }
                });
    }

    // ═══════════ 配方族类目 ═══════════

    public static class FamilyCategory implements IRecipeCategory<RecipeHolder<MachineRecipe>> {
        private final MachineRecipeFamily family;
        private final RecipeType<RecipeHolder<MachineRecipe>> type;
        private final IDrawable icon;
        private final IDrawableAnimated arrow;
        private final boolean fluidMode;
        /** 部件台 3×3 网格布局(261014 T5；9 输入槽机器)。 */
        private final boolean gridMode;

        // ── 布局常量(镜像机器 GUI；全部与 draw() 槽框坐标一致) ──
        private static final int IN_X = 30, IN_Y_ITEM = 23, IN_Y_FLUID = 64;   // 物品输入(横排)
        private static final int ARROW_X = 88, ARROW_Y = 24;                   // 物品族箭头
        private static final int BI_X = 56, BI_Y = 38;                         // 流体族 BI 箭头(避开三计量槽)
        private static final int OUT_X = 118, OUT_Y0 = 5;                      // 物品输出(物品族右列)
        private static final int OUT_X_FLUID = 90, OUT_Y_FLUID = 64;           // 物品输出(流体族底部行,3 槽 90..146 内)
        private static final int GIN_X = 10, GIN_Y = 10;                       // 流体入计量槽
        private static final int GOUT_X0 = 118, GOUT_STEP = 20, GOUT_Y = 10;   // 流体出计量槽(右对齐)
        private static final int GAUGE_INNER_W = 16, GAUGE_INNER_H = 58;
        // ── 部件台 3×3 网格(JEI_BG_TALL 146×88)；x=24 起避让左侧能量井(qa_jei_layout 实测) ──
        private static final int GRID_X = 24, GRID_Y = 14, GRID_STEP = 19;     // 网格 24..81 × 14..68
        private static final int GRID_ARROW_X = 86, GRID_ARROW_Y = 36;
        private static final int GRID_OUT_X = 116, GRID_OUT_Y = 36;

        public FamilyCategory(IGuiHelper helper, MachineRecipeFamily family) {
            this.family = family;
            this.type = RecipeType.createFromVanilla(family.type());
            var primaryBlock = BuiltInRegistries.BLOCK.getValue(family.primary());
            this.icon = primaryBlock != null
                    ? helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(primaryBlock))
                    : null;
            var arrowFill = helper.drawableBuilder(FWWidgets.ATLAS,
                    FWWidgets.PROG_ARROW_FILL, FWWidgets.PROG_Y, 24, 16)
                    .setTextureSize(512, 512).build();
            this.arrow = helper.createAnimatedDrawable(arrowFill, 40,
                    IDrawableAnimated.StartDirection.LEFT, false);
            MachineSpec spec = MachineSpecs.get(family.primary());
            this.fluidMode = spec != null && spec.tanks() > 0;
            this.gridMode = !fluidMode && spec != null && spec.inputSlots() >= 9;
        }

        @Override
        public RecipeType<RecipeHolder<MachineRecipe>> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return Component.translatable(family.titleKey());
        }

        @Override
        public int getWidth() {
            return FWWidgets.JEI_BG_W;
        }

        @Override
        public int getHeight() {
            return fluidMode || gridMode ? FWWidgets.JEI_BG_TALL_H : FWWidgets.JEI_BG_H;
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeHolder<MachineRecipe> holder, IFocusGroup focuses) {
            MachineRecipe recipe = holder.value();
            int inY = fluidMode ? IN_Y_FLUID : IN_Y_ITEM;

            // ── 物品输入(261014 T5/T6：部件台 3×3 网格；其余横排 ≤3；标准槽框) ──
            var inputs = recipe.inputs();
            if (gridMode) {
                for (int i = 0; i < Math.min(inputs.size(), 9); i++) {
                    var holders = inputs.get(i).ingredient().items().toList();
                    if (holders.isEmpty()) continue;
                    List<ItemStack> stacks = new ArrayList<>();
                    for (var h : holders) stacks.add(new ItemStack(h.value()));
                    builder.addSlot(RecipeIngredientRole.INPUT,
                                    GRID_X + (i % 3) * GRID_STEP, GRID_Y + (i / 3) * GRID_STEP)
                            .setStandardSlotBackground()
                            .addIngredients(VanillaTypes.ITEM_STACK, stacks);
                }
                builder.addSlot(RecipeIngredientRole.OUTPUT, GRID_OUT_X, GRID_OUT_Y)
                        .setOutputSlotBackground()
                        .addItemStack(recipe.outputs().isEmpty()
                                ? ItemStack.EMPTY : recipe.outputs().getFirst().create());
                return;
            }
            for (int i = 0; i < Math.min(inputs.size(), 3); i++) {
                var holders = inputs.get(i).ingredient().items().toList();
                if (holders.isEmpty()) continue;
                List<ItemStack> stacks = new ArrayList<>();
                for (var h : holders) stacks.add(new ItemStack(h.value()));
                builder.addSlot(RecipeIngredientRole.INPUT, IN_X + i * 19, inY)
                        .setStandardSlotBackground()
                        .addIngredients(VanillaTypes.ITEM_STACK, stacks);
            }

            // ── 物品输出(物品族=右列；流体族=底部行避开计量槽列, ≤3；标准输出槽框) ──
            var outputs = recipe.outputs();
            for (int i = 0; i < Math.min(outputs.size(), 3); i++) {
                int ox = fluidMode ? OUT_X_FLUID + i * 19 : OUT_X;
                int oy = fluidMode ? OUT_Y_FLUID : OUT_Y0 + i * 19;
                builder.addSlot(RecipeIngredientRole.OUTPUT, ox, oy)
                        .setOutputSlotBackground()
                        .addItemStack(outputs.get(i).create());
            }

            if (fluidMode) {
                // ── 流体入(左计量槽, JEI 标准流体渲染器填充量=配方量→满槽) ──
                recipe.fluidInput().ifPresent(fin -> {
                    var fluidOpt = BuiltInRegistries.FLUID.get(fin.fluid());
                    if (fluidOpt.isEmpty()) return;
                    Fluid fluid = fluidOpt.get().value();
                    var slot = builder.addSlot(RecipeIngredientRole.INPUT, GIN_X + 1, GIN_Y + 1)
                            .setFluidRenderer(fin.amount(), false, GAUGE_INNER_W, GAUGE_INNER_H)
                            .addIngredients(NeoForgeTypes.FLUID_STACK,
                                    List.of(new FluidStack(fluid, fin.amount())));
                    slot.addRichTooltipCallback((view, tip) ->
                            tip.add(Component.translatable("jei.fieldworks.recipe.fluid_in", fin.amount())));
                });

                // ── 流体出(右对齐计量槽列, ≤3) ──
                var fluidOuts = recipe.fluidOutputs();
                int n = Math.min(fluidOuts.size(), 3);
                for (int i = 0; i < n; i++) {
                    MachineRecipe.FluidTemplate ft = fluidOuts.get(n - 1 - i); // 右对齐：首个在最右
                    int gx = GOUT_X0 - i * GOUT_STEP;
                    var slot = builder.addSlot(RecipeIngredientRole.OUTPUT, gx + 1, GOUT_Y + 1)
                            .setFluidRenderer(ft.amount(), false, GAUGE_INNER_W, GAUGE_INNER_H);
                    FluidStack fs = ft.stack();
                    slot.addIngredients(NeoForgeTypes.FLUID_STACK, List.of(fs));
                    slot.addRichTooltipCallback((view, tip) ->
                            tip.add(Component.translatable("jei.fieldworks.recipe.fluid_out", fs.getAmount())));
                }
            }
        }

        @Override
        public void draw(RecipeHolder<MachineRecipe> holder, IRecipeSlotsView slotsView,
                         GuiGraphicsExtractor g, double mouseX, double mouseY) {
            MachineRecipe recipe = holder.value();
            // 底板
            if (fluidMode || gridMode) {
                FWWidgets.blit(g, 0, 0, FWWidgets.JEI_BG_TALL_X, FWWidgets.JEI_BG_TALL_Y,
                        FWWidgets.JEI_BG_TALL_W, FWWidgets.JEI_BG_TALL_H);
            } else {
                FWWidgets.blit(g, 0, 0, FWWidgets.JEI_BG_X, FWWidgets.JEI_BG_Y,
                        FWWidgets.JEI_BG_W, FWWidgets.JEI_BG_H);
            }
            if (fluidMode) {
                // 流体入/出计量槽外框
                if (recipe.fluidInput().isPresent()) {
                    FWWidgets.blit(g, GIN_X, GIN_Y, FWWidgets.GAUGE_STD_X, FWWidgets.GAUGE_STD_Y, 18, 62);
                }
                var fluidOuts = recipe.fluidOutputs();
                int n = Math.min(fluidOuts.size(), 3);
                for (int i = 0; i < n; i++) {
                    FWWidgets.blit(g, GOUT_X0 - i * GOUT_STEP, GOUT_Y,
                            FWWidgets.GAUGE_STD_X, FWWidgets.GAUGE_STD_Y, 18, 62);
                }
                // BI 双向箭头(机器 TANK 模板同款)
                FWWidgets.blit(g, BI_X, BI_Y, FWWidgets.PROG_BI_BG, FWWidgets.PROG_Y, 16, 6);
                // 能量图标(顶部空区, 避开左计量槽与底部物品行)
                FWWidgets.blit(g, 32, 10, FWWidgets.ICON_ENERGY, FWWidgets.ICON_Y, 18, 18);
            } else if (gridMode) {
                // 部件台网格：能量井(左) + 箭头底帧+动画
                FWWidgets.blit(g, 10, 16, FWWidgets.ENERGY_WELL_X, FWWidgets.ENERGY_Y, 8, 56);
                FWWidgets.blitBottomClipped(g, 10, 16, FWWidgets.ENERGY_FILL_X, FWWidgets.ENERGY_Y, 8, 56, 0.65);
                // 箭头 24×16 旋转 90° 不便——网格用横向箭头但收窄 12×16？直接用箭头于 (74,37)
                FWWidgets.blit(g, GRID_ARROW_X, GRID_ARROW_Y, FWWidgets.PROG_ARROW_BG, FWWidgets.PROG_Y, 24, 16);
                arrow.draw(g, GRID_ARROW_X, GRID_ARROW_Y);
            } else {
                // 能量井(左侧, 机器同款琥珀井)
                FWWidgets.blit(g, 10, 4, FWWidgets.ENERGY_WELL_X, FWWidgets.ENERGY_Y, 8, 56);
                FWWidgets.blitBottomClipped(g, 10, 4, FWWidgets.ENERGY_FILL_X, FWWidgets.ENERGY_Y, 8, 56, 0.65);
                // 箭头底帧+动画
                FWWidgets.blit(g, ARROW_X, ARROW_Y, FWWidgets.PROG_ARROW_BG, FWWidgets.PROG_Y, 24, 16);
                arrow.draw(g, ARROW_X, ARROW_Y);
            }
            // ── 计数配料角标(261014 T6：×N 画在槽右下角) ──
            var font = net.minecraft.client.Minecraft.getInstance().font;
            var inputs = recipe.inputs();
            int max = gridMode ? 9 : 3;
            for (int i = 0; i < Math.min(inputs.size(), max); i++) {
                int count = inputs.get(i).count();
                if (count <= 1) continue;
                int sx = gridMode ? GRID_X + (i % 3) * GRID_STEP : IN_X + i * 19;
                int sy = gridMode ? GRID_Y + (i / 3) * GRID_STEP : (fluidMode ? IN_Y_FLUID : IN_Y_ITEM);
                String label = "×" + count;
                g.text(font, net.minecraft.network.chat.Component.literal(label),
                        sx + 18 - font.width(label), sy + 10, 0xFF1E9E96, true);
            }
        }

        @Override
        public void getTooltip(ITooltipBuilder tooltip, RecipeHolder<MachineRecipe> holder,
                               IRecipeSlotsView slotsView, double mouseX, double mouseY) {
            MachineRecipe recipe = holder.value();
            // 能量区悬停：能耗读数
            boolean overEnergy = fluidMode
                    ? (mouseX >= 32 && mouseX < 50 && mouseY >= 10 && mouseY < 28)
                    : (mouseX >= 10 && mouseX < 18 && mouseY >= 4 && mouseY < 60);
            if (overEnergy) {
                tooltip.add(Component.translatable("jei.fieldworks.recipe.energy", recipe.energy()));
            }
            tooltip.add(Component.translatable("jei.fieldworks.recipe.time", recipe.time() / 20.0));
            // 适用机器(同族多机时逐台列出)
            List<Identifier> machines = recipe.machines();
            if (machines.size() > 1 || !machines.contains(family.primary())) {
                tooltip.add(Component.translatable("jei.fieldworks.recipe.machines"));
                for (var m : machines) {
                    var spec = MachineSpecs.get(m);
                    tooltip.add(Component.literal("⌂ ").append(
                            Component.translatable(spec != null ? spec.langKey() : m.toString())));
                }
            }
        }
    }
}
