package com.srqingchen.fieldworks.registry;

import com.srqingchen.fieldworks.block.CaptureBlock;
import com.srqingchen.fieldworks.block.DisplayBlock;
import com.srqingchen.fieldworks.block.GiantCoreBlock;
import com.srqingchen.fieldworks.block.GiantControllerBlock;
import com.srqingchen.fieldworks.block.PipeBlock;
import com.srqingchen.fieldworks.envelope.FieldCouplerBlock;
import com.srqingchen.fieldworks.envelope.FieldConstrainerBlock;
import com.srqingchen.fieldworks.envelope.FieldFrameBlock;
import com.srqingchen.fieldworks.envelope.FieldInjectorBlock;
import com.srqingchen.fieldworks.envelope.FieldStabilizerBlock;
import com.srqingchen.fieldworks.machine.MachineBlock;
import com.srqingchen.fieldworks.machine.MachineSpec;
import com.srqingchen.fieldworks.machine.MachineSpecs;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 全部方块注册(26.1.2 registerBlock 范式：Properties 由注册器先绑定 block id，
 * 供 BlockBehaviour 构造器解析 loot table——已反编译确认)。
 */
public final class FWBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(com.srqingchen.fieldworks.Fieldworks.MODID);

    private static final List<Supplier<MachineBlock>> MACHINE_BLOCKS = new ArrayList<>();

    private static DeferredBlock<MachineBlock> machine(MachineSpec spec) {
        var b = BLOCKS.registerBlock(spec.id().getPath(),
                p -> new MachineBlock(p, spec),
                p -> p.mapColor(MapColor.METAL).strength(2.5f, 6f)
                        .sound(SoundType.COPPER).requiresCorrectToolForDrops());
        MACHINE_BLOCKS.add(b);
        return b;
    }

    // ═══════ P0 ═══════
    public static final DeferredBlock<MachineBlock> STEAM_GENERATOR = machine(MachineSpecs.STEAM_GENERATOR);
    public static final DeferredBlock<MachineBlock> PART_BENCH = machine(MachineSpecs.PART_BENCH);
    public static final DeferredBlock<MachineBlock> ELECTROLYZER_1 = machine(MachineSpecs.ELECTROLYZER_1);
    public static final DeferredBlock<MachineBlock> BLAST_FURNACE = machine(MachineSpecs.BLAST_FURNACE);
    public static final DeferredBlock<MachineBlock> INFINITE_WATER_PUMP = machine(MachineSpecs.INFINITE_WATER_PUMP);

    // ═══════ P1 ═══════
    public static final DeferredBlock<MachineBlock> COMPRESSOR = machine(MachineSpecs.COMPRESSOR);
    public static final DeferredBlock<MachineBlock> DISTILLATION_TOWER = machine(MachineSpecs.DISTILLATION_TOWER);
    public static final DeferredBlock<MachineBlock> GAS_TANK_1 = machine(MachineSpecs.GAS_TANK_1);
    public static final DeferredBlock<MachineBlock> GAS_TANK_4 = machine(MachineSpecs.GAS_TANK_4);
    public static final DeferredBlock<MachineBlock> GAS_TANK_16 = machine(MachineSpecs.GAS_TANK_16);
    public static final DeferredBlock<MachineBlock> BLOWER = machine(MachineSpecs.BLOWER);
    public static final DeferredBlock<MachineBlock> HYDROGEN_SHAFT_FURNACE = machine(MachineSpecs.HYDROGEN_SHAFT_FURNACE);
    public static final DeferredBlock<MachineBlock> STEAM_TURBINE = machine(MachineSpecs.STEAM_TURBINE);
    public static final DeferredBlock<MachineBlock> ASSEMBLER = machine(MachineSpecs.ASSEMBLER);
    public static final DeferredBlock<MachineBlock> CONDENSER = machine(MachineSpecs.CONDENSER);
    public static final DeferredBlock<MachineBlock> RECYCLER = machine(MachineSpecs.RECYCLER);

    // ═══════ P2 ═══════
    public static final DeferredBlock<MachineBlock> MAGNETIZER = machine(MachineSpecs.MAGNETIZER);
    public static final DeferredBlock<MachineBlock> MAGNETIC_SEPARATOR = machine(MachineSpecs.MAGNETIC_SEPARATOR);
    public static final DeferredBlock<MachineBlock> GRAVITY_SEPARATOR = machine(MachineSpecs.GRAVITY_SEPARATOR);
    public static final DeferredBlock<MachineBlock> DEEP_DRILL = machine(MachineSpecs.DEEP_DRILL);
    public static final DeferredBlock<MachineBlock> NEGATIVE_PRESSURE_CATCHER = machine(MachineSpecs.NEGATIVE_PRESSURE_CATCHER);
    public static final DeferredBlock<MachineBlock> IRRADIATION_CHAMBER = machine(MachineSpecs.IRRADIATION_CHAMBER);
    public static final DeferredBlock<MachineBlock> HEAT_EXCHANGER = machine(MachineSpecs.HEAT_EXCHANGER);
    public static final DeferredBlock<MachineBlock> ELECTROLYZER_2 = machine(MachineSpecs.ELECTROLYZER_2);
    public static final DeferredBlock<MachineBlock> ELECTROLYZER_3 = machine(MachineSpecs.ELECTROLYZER_3);

    // ═══════ P3 ═══════
    public static final DeferredBlock<MachineBlock> CARBON_CAPTURER = machine(MachineSpecs.CARBON_CAPTURER);
    public static final DeferredBlock<MachineBlock> CORROSION_TRAP = machine(MachineSpecs.CORROSION_TRAP);
    public static final DeferredBlock<MachineBlock> PLASMA_ELECTROLYSIS = machine(MachineSpecs.PLASMA_ELECTROLYSIS);

    // ═══════ 石制捕获器(P0，空手右键充能，§8.3) ═══════
    public static final DeferredBlock<CaptureBlock> STONE_CATCHER = BLOCKS.registerBlock("stone_catcher",
            CaptureBlock::new, p -> p.mapColor(MapColor.STONE).strength(1.8f, 6f).sound(SoundType.STONE));

    // ═══════ 管道体系(§10.5；261014 T2 三族：气液合并为流体管+物品管+能量管) ═══════
    public static final DeferredBlock<PipeBlock> FLUID_PIPE = BLOCKS.registerBlock("fluid_pipe",
            p -> new PipeBlock(p, PipeBlock.PipeKind.FLUID),
            p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(1.2f, 4f).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<PipeBlock> ITEM_PIPE = BLOCKS.registerBlock("item_pipe",
            p -> new PipeBlock(p, PipeBlock.PipeKind.ITEM),
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(1.2f, 4f).sound(SoundType.COPPER).noOcclusion());
    public static final DeferredBlock<PipeBlock> ENERGY_PIPE = BLOCKS.registerBlock("energy_pipe",
            p -> new PipeBlock(p, PipeBlock.PipeKind.ENERGY),
            p -> p.mapColor(MapColor.COLOR_CYAN).strength(1.2f, 4f).sound(SoundType.COPPER).noOcclusion());

    // ═══════ 人工场五件套(§11.1) ═══════
    public static final DeferredBlock<FieldFrameBlock> FIELD_FRAME = BLOCKS.registerBlock("field_frame",
            FieldFrameBlock::new, p -> p.mapColor(MapColor.COLOR_BLUE).strength(4f, 12f)
                    .sound(SoundType.DEEPSLATE).lightLevel(s -> 3).noOcclusion());
    public static final DeferredBlock<FieldInjectorBlock> FIELD_INJECTOR = BLOCKS.registerBlock("field_injector",
            FieldInjectorBlock::new, p -> p.mapColor(MapColor.COLOR_BLUE).strength(3f, 9f)
                    .sound(SoundType.COPPER).lightLevel(s -> 6).noOcclusion());
    public static final DeferredBlock<FieldCouplerBlock> FIELD_COUPLER = BLOCKS.registerBlock("field_coupler",
            FieldCouplerBlock::new, p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3f, 9f)
                    .sound(SoundType.COPPER).lightLevel(s -> 5).noOcclusion());
    public static final DeferredBlock<FieldConstrainerBlock> FIELD_CONSTRAINER = BLOCKS.registerBlock("field_constrainer",
            FieldConstrainerBlock::new, p -> p.mapColor(MapColor.COLOR_GREEN).strength(3f, 9f)
                    .sound(SoundType.COPPER).lightLevel(s -> 4).noOcclusion());
    public static final DeferredBlock<FieldStabilizerBlock> FIELD_STABILIZER = BLOCKS.registerBlock("field_stabilizer",
            FieldStabilizerBlock::new, p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(3f, 9f)
                    .sound(SoundType.COPPER).lightLevel(s -> 4).noOcclusion());

    // ═══════ 观测/显示设备(§17) ═══════
    public static final DeferredBlock<DisplayBlock> MAGNETOMETER = display("magnetometer");
    public static final DeferredBlock<DisplayBlock> TIDAL_METER = display("tidal_meter");
    public static final DeferredBlock<DisplayBlock> CARBON_GAUGE = display("carbon_gauge");
    public static final DeferredBlock<DisplayBlock> OBSERVATION_LIGHTHOUSE = display("observation_lighthouse");
    public static final DeferredBlock<DisplayBlock> SPECTROMETER = display("spectrometer");

    // ═══════ 特殊工艺方块 ═══════
    public static final DeferredBlock<com.srqingchen.fieldworks.block.ResonantDrillBlock> RESONANT_DRILL =
            BLOCKS.registerBlock("resonant_drill", com.srqingchen.fieldworks.block.ResonantDrillBlock::new,
                    p -> p.mapColor(MapColor.COLOR_PURPLE).strength(6f, 20f).sound(SoundType.NETHERITE_BLOCK)
                            .requiresCorrectToolForDrops().lightLevel(s -> 8).noOcclusion());
    public static final DeferredBlock<com.srqingchen.fieldworks.block.CrystalFarmBlock> CRYSTAL_FARM =
            BLOCKS.registerBlock("crystal_farm", com.srqingchen.fieldworks.block.CrystalFarmBlock::new,
                    p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2.5f, 8f).sound(SoundType.AMETHYST).noOcclusion());
    public static final DeferredBlock<com.srqingchen.fieldworks.block.HarmonicChamberBlock> HARMONIC_CHAMBER =
            BLOCKS.registerBlock("harmonic_chamber", com.srqingchen.fieldworks.block.HarmonicChamberBlock::new,
                    p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3f, 9f).sound(SoundType.AMETHYST)
                            .lightLevel(s -> 5).noOcclusion());
    public static final DeferredBlock<com.srqingchen.fieldworks.block.HarmonicPodBlock> HARMONIC_POD =
            BLOCKS.registerBlock("harmonic_enchant_pod", com.srqingchen.fieldworks.block.HarmonicPodBlock::new,
                    p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3f, 9f).sound(SoundType.AMETHYST)
                            .lightLevel(s -> 7).noOcclusion());
    public static final DeferredBlock<com.srqingchen.fieldworks.block.TameGeneratorBlock> TAME_GENERATOR =
            BLOCKS.registerBlock("tame_generator", com.srqingchen.fieldworks.block.TameGeneratorBlock::new,
                    p -> p.mapColor(MapColor.COLOR_GREEN).strength(3f, 9f).sound(SoundType.COPPER)
                            .lightLevel(s -> 5).noOcclusion());
    public static final DeferredBlock<com.srqingchen.fieldworks.block.PhaseBridgeBlock> PHASE_BRIDGE =
            BLOCKS.registerBlock("phase_item_bridge", com.srqingchen.fieldworks.block.PhaseBridgeBlock::new,
                    p -> p.mapColor(MapColor.COLOR_MAGENTA).strength(3f, 9f).sound(SoundType.COPPER)
                            .lightLevel(s -> 4).noOcclusion());
    public static final DeferredBlock<com.srqingchen.fieldworks.block.SkyPlatformBlock> SKY_PLATFORM =
            BLOCKS.registerBlock("sky_platform", com.srqingchen.fieldworks.block.SkyPlatformBlock::new,
                    p -> p.mapColor(MapColor.METAL).strength(3f, 12f).sound(SoundType.DEEPSLATE_BRICKS).noOcclusion());

    // ═══════ 巨物体系(§13) ═══════
    public static final DeferredBlock<Block> REINFORCED_TRUSS = BLOCKS.registerBlock("reinforced_truss",
            Block::new, p -> p.mapColor(MapColor.COLOR_GRAY).strength(8f, 24f)
                    .sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<Block> STEEL_SHELL = BLOCKS.registerBlock("steel_shell",
            Block::new, p -> p.mapColor(MapColor.COLOR_LIGHT_GRAY).strength(8f, 24f)
                    .sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().noOcclusion());
    public static final DeferredBlock<Block> OBSERVATION_GLASS = BLOCKS.registerBlock("observation_glass",
            Block::new, p -> p.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(4f, 24f)
                    .sound(SoundType.GLASS).noOcclusion());
    public static final DeferredBlock<Block> STEEL_CASING = BLOCKS.registerBlock("steel_casing",
            Block::new, p -> p.mapColor(MapColor.METAL).strength(5f, 12f)
                    .sound(SoundType.COPPER).requiresCorrectToolForDrops());
    public static final DeferredBlock<GiantCoreBlock> GIANT_CORE = BLOCKS.registerBlock("giant_core",
            GiantCoreBlock::new, p -> p.mapColor(MapColor.GOLD).strength(10f, 30f)
                    .sound(SoundType.NETHERITE_BLOCK).requiresCorrectToolForDrops().lightLevel(s -> 10).noOcclusion());
    public static final DeferredBlock<Block> AMETHYST_PLATE = BLOCKS.registerBlock("amethyst_plate",
            Block::new, p -> p.mapColor(MapColor.COLOR_PURPLE).strength(3f, 9f)
                    .sound(SoundType.AMETHYST).lightLevel(s -> 6).noOcclusion());

    // ═══════ 蓝图投影锚点(§13 技术方块，不出创造栏) ═══════
    public static final DeferredBlock<com.srqingchen.fieldworks.block.ProjectionBlock> PROJECTION =
            BLOCKS.registerBlock("projection", com.srqingchen.fieldworks.block.ProjectionBlock::new,
                    p -> p.mapColor(MapColor.METAL).strength(0.5f, 4f).noLootTable()
                            .noOcclusion().noCollision());

    /** 巨物变体名 → 控制器方块(蓝图系统)。 */
    public static net.minecraft.world.level.block.state.BlockState GIANT_CONTROLLER_OF(String variant) {
        return switch (variant) {
            case "resonance_tower" -> RESONANCE_TOWER.get().defaultBlockState();
            case "deep_electrolysis_array" -> DEEP_ELECTROLYSIS_ARRAY.get().defaultBlockState();
            case "eco_dome" -> ECO_DOME.get().defaultBlockState();
            case "warp_anchor" -> WARP_ANCHOR.get().defaultBlockState();
            case "harmonic_cathedral" -> HARMONIC_CATHEDRAL.get().defaultBlockState();
            case "planetary_array" -> PLANETARY_ARRAY.get().defaultBlockState();
            default -> null;
        };
    }

    // 巨物控制器 ×6(§13.2)
    public static final DeferredBlock<GiantControllerBlock> RESONANCE_TOWER = giant("resonance_tower", 0);
    public static final DeferredBlock<GiantControllerBlock> DEEP_ELECTROLYSIS_ARRAY = giant("deep_electrolysis_array", 1);
    public static final DeferredBlock<GiantControllerBlock> ECO_DOME = giant("eco_dome", 2);
    public static final DeferredBlock<GiantControllerBlock> WARP_ANCHOR = giant("warp_anchor", 3);
    public static final DeferredBlock<GiantControllerBlock> HARMONIC_CATHEDRAL = giant("harmonic_cathedral", 4);
    public static final DeferredBlock<GiantControllerBlock> PLANETARY_ARRAY = giant("planetary_array", 5);

    private static DeferredBlock<GiantControllerBlock> giant(String path, int variant) {
        return BLOCKS.registerBlock(path, p -> new GiantControllerBlock(p, variant),
                p -> p.mapColor(MapColor.GOLD).strength(10f, 30f).sound(SoundType.NETHERITE_BLOCK)
                        .requiresCorrectToolForDrops().lightLevel(s -> 12).noOcclusion());
    }

    // ═══════ 矿物(§9.2 克制：仅 3 新矿) ═══════
    public static final DeferredBlock<DropExperienceBlock> BAUXITE_ORE = BLOCKS.registerBlock("bauxite_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 0), p),
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(3f, 6f).requiresCorrectToolForDrops().sound(SoundType.STONE));
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_BAUXITE_ORE = BLOCKS.registerBlock("deepslate_bauxite_ore",
            p -> new DropExperienceBlock(UniformInt.of(0, 0), p),
            p -> p.mapColor(MapColor.COLOR_ORANGE).strength(4.5f, 6f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<DropExperienceBlock> MONAZITE_ORE = BLOCKS.registerBlock("monazite_ore",
            p -> new DropExperienceBlock(UniformInt.of(2, 5), p),
            p -> p.mapColor(MapColor.COLOR_YELLOW).strength(5f, 9f).requiresCorrectToolForDrops().sound(SoundType.DEEPSLATE));
    public static final DeferredBlock<DropExperienceBlock> RESONANT_CLUSTER = BLOCKS.registerBlock("resonant_cluster",
            p -> new DropExperienceBlock(UniformInt.of(2, 6), p),
            p -> p.mapColor(MapColor.COLOR_PURPLE).strength(2f, 6f).requiresCorrectToolForDrops()
                    .sound(SoundType.AMETHYST).lightLevel(s -> 5).noOcclusion());

    private static DeferredBlock<DisplayBlock> display(String path) {
        return BLOCKS.registerBlock(path, p -> new DisplayBlock(p, path),
                p -> p.mapColor(MapColor.METAL).strength(2.5f, 6f).sound(SoundType.COPPER)
                        .noOcclusion().lightLevel(s -> 3));
    }

    /** BE 注册用：全部机器方块快照。 */
    public static Block[] machineBlocks() {
        return MACHINE_BLOCKS.stream().map(Supplier::get).toArray(Block[]::new);
    }

    private FWBlocks() {}
}
