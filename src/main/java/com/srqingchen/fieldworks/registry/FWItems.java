package com.srqingchen.fieldworks.registry;

import com.srqingchen.fieldworks.item.FieldpackTerminalItem;
import com.srqingchen.fieldworks.item.ProbeStaffItem;
import com.srqingchen.fieldworks.item.TidalCalendarItem;
import com.srqingchen.fieldworks.item.WrenchItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 全部物品注册(§9 材料体系 52 项 + 部件 12 + 工具 + 升级芯 + 杂项)。
 */
public final class FWItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(com.srqingchen.fieldworks.Fieldworks.MODID);

    private static DeferredItem<Item> item(String path) {
        return ITEMS.registerSimpleItem(path, () -> new Item.Properties());
    }

    private static DeferredItem<Item> item(String path, Rarity rarity) {
        return ITEMS.registerSimpleItem(path, () -> new Item.Properties().rarity(rarity));
    }

    // ═══════ 工具(§17 探测器分级 T0—T4) ═══════
    public static final DeferredItem<ProbeStaffItem> PROBE_STAFF =
            ITEMS.registerItem("probe_staff", p -> new ProbeStaffItem(p.stacksTo(1).durability(16)));
    public static final DeferredItem<Item> SPECTRUM_MODULE = item("spectrum_module");        // T1 升级部件
    public static final DeferredItem<FieldpackTerminalItem> FIELDPACK_TERMINAL =
            ITEMS.registerItem("fieldpack_terminal", p -> new FieldpackTerminalItem(p.stacksTo(1)));
    public static final DeferredItem<TidalCalendarItem> TIDAL_CALENDAR =
            ITEMS.registerItem("tidal_calendar", p -> new TidalCalendarItem(p.stacksTo(1)));
    public static final DeferredItem<WrenchItem> WRENCH =
            ITEMS.registerItem("wrench", p -> new WrenchItem(p.stacksTo(1)));

    // ═══════ G1 基础加工代(§9.2) ═══════
    public static final DeferredItem<Item> STEEL_INGOT = item("steel_ingot");
    public static final DeferredItem<Item> ALUMINUM_INGOT = item("aluminum_ingot");
    public static final DeferredItem<Item> SILICON_WAFER = item("silicon_wafer");
    public static final DeferredItem<Item> COMPRESSED_AIR = item("compressed_air");          // 开局通用标签物品
    public static final DeferredItem<Item> CARBON_BLACK = item("carbon_black");              // 碳捕获副产(§5.4)

    // ═══════ G2 特种合金代 ═══════
    public static final DeferredItem<Item> STAINLESS_STEEL_INGOT = item("stainless_steel_ingot");
    public static final DeferredItem<Item> PURE_IRON_INGOT = item("pure_iron_ingot");        // 电磁纯铁
    public static final DeferredItem<Item> INVAR_INGOT = item("invar_ingot");
    public static final DeferredItem<Item> PERMALLOY_INGOT = item("permalloy_ingot");
    public static final DeferredItem<Item> MONEL_INGOT = item("monel_ingot");
    public static final DeferredItem<Item> ALUMINUM_BRASS_INGOT = item("aluminum_brass_ingot");

    // ═══════ G3 场学严苛代 ═══════
    public static final DeferredItem<Item> RESONANT_CRYSTAL_RAW = item("resonant_crystal_raw");
    public static final DeferredItem<Item> RESONANT_CRYSTAL_REFINED = item("resonant_crystal_refined");
    public static final DeferredItem<Item> RESONANT_CRYSTAL_ULTRA = item("resonant_crystal_ultra", Rarity.RARE);
    public static final DeferredItem<Item> MAGNETIZED_CORE = item("magnetized_core");
    public static final DeferredItem<Item> FIELD_WIRE = item("field_wire");
    public static final DeferredItem<Item> FLUX_ALLOY_INGOT = item("flux_alloy_ingot");
    public static final DeferredItem<Item> THORIUM_PELLET = item("thorium_pellet");
    public static final DeferredItem<Item> DRY_ICE = item("dry_ice");
    public static final DeferredItem<Item> INERT_CONCENTRATE = item("inert_concentrate");
    public static final DeferredItem<Item> AMETHYST_LENS = item("amethyst_lens");
    public static final DeferredItem<Item> SULFUR = item("sulfur");
    public static final DeferredItem<Item> CORROSION_SALT = item("corrosion_salt");

    // ═══════ G4 虚空调谐代 ═══════
    public static final DeferredItem<Item> VOID_CONDENSATE = item("void_condensate", Rarity.EPIC);
    public static final DeferredItem<Item> TUNED_VOID_ESSENCE = item("tuned_void_essence", Rarity.EPIC);
    public static final DeferredItem<Item> VOID_LENS = item("void_lens", Rarity.EPIC);
    public static final DeferredItem<Item> TUNING_FORK = item("tuning_fork", Rarity.EPIC);
    public static final DeferredItem<Item> TIDAL_RESONATOR = item("tidal_resonator", Rarity.RARE);
    public static final DeferredItem<Item> AMETHYST_RESONATOR = item("amethyst_resonator", Rarity.RARE);

    // ═══════ G5 超导代 ═══════
    public static final DeferredItem<Item> SUPERCONDUCTOR_ALLOY = item("superconductor_alloy", Rarity.EPIC);
    public static final DeferredItem<Item> SUPERCONDUCTOR_RING = item("superconductor_ring", Rarity.EPIC);

    // ═══════ 部件(12 项，部件台产物 §9.2) ═══════
    public static final DeferredItem<Item> COPPER_COIL = item("copper_coil");
    public static final DeferredItem<Item> STEEL_GEAR = item("steel_gear");
    public static final DeferredItem<Item> PISTON_FRAME = item("piston_frame");
    public static final DeferredItem<Item> PRECISION_BEARING = item("precision_bearing");
    public static final DeferredItem<Item> LOGIC_CIRCUIT = item("logic_circuit");
    public static final DeferredItem<Item> SENSOR_CORE = item("sensor_core");
    public static final DeferredItem<Item> NOZZLE_ASSEMBLY = item("nozzle_assembly");
    public static final DeferredItem<Item> DISTILLATION_TRAY = item("distillation_tray");
    public static final DeferredItem<Item> ELECTROMAGNET_CORE = item("electromagnet_core");
    public static final DeferredItem<Item> TURBINE_BLADE = item("turbine_blade");
    public static final DeferredItem<Item> SEAL_FLANGE = item("seal_flange");
    public static final DeferredItem<Item> RESONATOR_SHELL = item("resonator_shell");

    // ═══════ 矿物产物 ═══════
    public static final DeferredItem<Item> RAW_BAUXITE = item("raw_bauxite");
    public static final DeferredItem<Item> RAW_MONAZITE = item("raw_monazite");
    public static final DeferredItem<Item> RESONANT_SHARD = item("resonant_shard");

    // ═══════ 升级芯(§10.4) ═══════
    public static final DeferredItem<Item> UPGRADE_EFFICIENCY_1 = item("upgrade_efficiency_1");
    public static final DeferredItem<Item> UPGRADE_EFFICIENCY_2 = item("upgrade_efficiency_2");
    public static final DeferredItem<Item> UPGRADE_EFFICIENCY_3 = item("upgrade_efficiency_3");
    public static final DeferredItem<Item> UPGRADE_FIELD_CAPACITY = item("upgrade_field_capacity");
    public static final DeferredItem<Item> UPGRADE_RECTIFIER = item("upgrade_rectifier");
    public static final DeferredItem<Item> UPGRADE_ISOLATION = item("upgrade_isolation");
    public static final DeferredItem<Item> UPGRADE_SUPERCONDUCTOR = item("upgrade_superconductor");

    // ═══════ 谐振燃料链(§13.3) ═══════
    public static final DeferredItem<Item> RESONANCE_FUEL = item("resonance_fuel", Rarity.RARE);
    public static final DeferredItem<Item> MERCURY_FULMINATE = item("mercury_fulminate");
    // 律动之书(§12.1)：谐律附魔"等级突破"材料——非教程书(手册见 fieldworks:manual, 261012 澄清)
    public static final DeferredItem<Item> RHYTHMIC_BOOK = ITEMS.registerItem("rhythmic_book",
            p -> new Item(p.rarity(Rarity.EPIC)) {
                @Override
                public void appendHoverText(ItemStack stack, TooltipContext ctx,
                                            net.minecraft.world.item.component.TooltipDisplay display,
                                            java.util.function.Consumer<Component> lines,
                                            net.minecraft.world.item.TooltipFlag flag) {
                    lines.accept(Component.translatable("tooltip.fieldworks.rhythmic_book.clarify")
                            .withStyle(net.minecraft.ChatFormatting.GRAY));
                    lines.accept(Component.translatable("tooltip.fieldworks.rhythmic_book.guide")
                            .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
                }
            });

    // ═══════ 巨物/杂项 ═══════
    public static final DeferredItem<com.srqingchen.fieldworks.item.GiantBlueprintItem> GIANT_BLUEPRINT =
            ITEMS.registerItem("giant_blueprint", p -> new com.srqingchen.fieldworks.item.GiantBlueprintItem(
                    p.stacksTo(1).rarity(Rarity.RARE)));

    // ═══════ 场学饰品(261014 T10，Curios 软依赖；槽位=物品标签 curios:ring 等) ═══════
    public static final DeferredItem<Item> RESONANT_BAND = ITEMS.registerItem("resonant_band",
            p -> new com.srqingchen.fieldworks.item.AccessoryItem(
                    p.stacksTo(1).rarity(Rarity.RARE)
                            .attributes(com.srqingchen.fieldworks.compat.curios.AccessoryEvents.attributesOf("resonant_band")),
                    "item.fieldworks.resonant_band.desc"));
    public static final DeferredItem<Item> MAGNETIC_LENS = ITEMS.registerItem("magnetic_lens",
            p -> new com.srqingchen.fieldworks.item.AccessoryItem(
                    p.stacksTo(1).rarity(Rarity.RARE),
                    "item.fieldworks.magnetic_lens.desc"));
    public static final DeferredItem<Item> FIELDLINE_AMULET = ITEMS.registerItem("fieldline_amulet",
            p -> new com.srqingchen.fieldworks.item.AccessoryItem(
                    p.stacksTo(1).rarity(Rarity.RARE)
                            .attributes(com.srqingchen.fieldworks.compat.curios.AccessoryEvents.attributesOf("fieldline_amulet")),
                    "item.fieldworks.fieldline_amulet.desc"));
    public static final DeferredItem<Item> FLUX_BELT = ITEMS.registerItem("flux_belt",
            p -> new com.srqingchen.fieldworks.item.AccessoryItem(
                    p.stacksTo(1).rarity(Rarity.RARE)
                            .attributes(com.srqingchen.fieldworks.compat.curios.AccessoryEvents.attributesOf("flux_belt")),
                    "item.fieldworks.flux_belt.desc"));
    public static final DeferredItem<Item> TIDAL_CHARM = ITEMS.registerItem("tidal_charm",
            p -> new com.srqingchen.fieldworks.item.AccessoryItem(
                    p.stacksTo(1).rarity(Rarity.RARE)
                            .attributes(com.srqingchen.fieldworks.compat.curios.AccessoryEvents.attributesOf("tidal_charm")),
                    "item.fieldworks.tidal_charm.desc"));
    public static final DeferredItem<Item> VOID_STEP_RING = ITEMS.registerItem("void_step_ring",
            p -> new com.srqingchen.fieldworks.item.AccessoryItem(
                    p.stacksTo(1).rarity(Rarity.EPIC),
                    "item.fieldworks.void_step_ring.desc"));
    /** 终极饰品：伤害转电盾 + 谐律增幅叠加。 */
    public static final DeferredItem<com.srqingchen.fieldworks.item.SuperconductorCoreItem> SUPERCONDUCTOR_CORE =
            ITEMS.registerItem("superconductor_core",
                    p -> new com.srqingchen.fieldworks.item.SuperconductorCoreItem(
                            p.stacksTo(1).rarity(Rarity.EPIC)));

    /** 巨物核心物品(自动注册的 BlockItem；蓝图搭建消耗件)。 */
    public static Item giantCoreItem() {
        return FWBlocks.GIANT_CORE.get().asItem();
    }

    // ═══════ 方块物品(全部方块同名 BlockItem) ═══════
    static {
        FWBlocks.BLOCKS.getEntries().forEach(b ->
                ITEMS.registerSimpleBlockItem(b.getId().getPath(), b));
    }

    private FWItems() {}
}
