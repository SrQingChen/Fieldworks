package com.srqingchen.fieldworks.registry;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.block.CaptureBlockEntity;
import com.srqingchen.fieldworks.block.CrystalFarmBlockEntity;
import com.srqingchen.fieldworks.block.DisplayBlockEntity;
import com.srqingchen.fieldworks.block.EnergyPipeBlockEntity;
import com.srqingchen.fieldworks.block.GiantControllerEntity;
import com.srqingchen.fieldworks.block.HarmonicChamberBlockEntity;
import com.srqingchen.fieldworks.block.HarmonicPodBlockEntity;
import com.srqingchen.fieldworks.block.ItemPipeBlockEntity;
import com.srqingchen.fieldworks.block.PhaseBridgeBlockEntity;
import com.srqingchen.fieldworks.block.PipeBlockEntity;
import com.srqingchen.fieldworks.block.ResonantDrillBlockEntity;
import com.srqingchen.fieldworks.block.TameGeneratorBlockEntity;
import com.srqingchen.fieldworks.envelope.FieldPartBlockEntity;
import com.srqingchen.fieldworks.machine.MachineBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 全部方块实体类型。MACHINE 类型绑定全部 spec 机器方块(构造器从方块自取 spec)。
 */
public final class FWBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, Fieldworks.MODID);

    private static final class MachineTypeBox {
        static volatile BlockEntityType<MachineBlockEntity> instance;
    }

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MachineBlockEntity>> MACHINE =
            BLOCK_ENTITIES.register("machine", () -> {
                var type = new BlockEntityType<MachineBlockEntity>(
                        (pos, state) -> {
                            var spec = ((com.srqingchen.fieldworks.machine.MachineBlock) state.getBlock()).spec();
                            // 无限水泵等特殊机器走子类(261014 T7)：能力/菜单/屏幕仍共用 MACHINE 类型
                            if (spec.mode() == com.srqingchen.fieldworks.machine.MachineSpec.Mode.PUMP) {
                                return new com.srqingchen.fieldworks.machine.InfinitePumpBlockEntity(
                                        MachineTypeBox.instance, pos, state, spec);
                            }
                            return new MachineBlockEntity(MachineTypeBox.instance, pos, state, spec);
                        },
                        FWBlocks.machineBlocks());
                MachineTypeBox.instance = type;
                return type;
            });

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CaptureBlockEntity>> CAPTURE =
            BLOCK_ENTITIES.register("capture", () -> new BlockEntityType<>(
                    CaptureBlockEntity::new, FWBlocks.STONE_CATCHER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipeBlockEntity>> PIPE =
            BLOCK_ENTITIES.register("pipe", () -> new BlockEntityType<>(
                    PipeBlockEntity::new, FWBlocks.FLUID_PIPE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ItemPipeBlockEntity>> ITEM_PIPE =
            BLOCK_ENTITIES.register("item_pipe", () -> new BlockEntityType<>(
                    ItemPipeBlockEntity::new, FWBlocks.ITEM_PIPE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EnergyPipeBlockEntity>> ENERGY_PIPE =
            BLOCK_ENTITIES.register("energy_pipe", () -> new BlockEntityType<>(
                    EnergyPipeBlockEntity::new, FWBlocks.ENERGY_PIPE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FieldPartBlockEntity>> FIELD_PART =
            BLOCK_ENTITIES.register("field_part", () -> new BlockEntityType<>(
                    FieldPartBlockEntity::new, FWBlocks.FIELD_FRAME.get(), FWBlocks.FIELD_INJECTOR.get(),
                    FWBlocks.FIELD_COUPLER.get(), FWBlocks.FIELD_CONSTRAINER.get(), FWBlocks.FIELD_STABILIZER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DisplayBlockEntity>> DISPLAY =
            BLOCK_ENTITIES.register("display", () -> new BlockEntityType<>(
                    DisplayBlockEntity::new, FWBlocks.MAGNETOMETER.get(), FWBlocks.TIDAL_METER.get(),
                    FWBlocks.CARBON_GAUGE.get(), FWBlocks.OBSERVATION_LIGHTHOUSE.get(), FWBlocks.SPECTROMETER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ResonantDrillBlockEntity>> RESONANT_DRILL =
            BLOCK_ENTITIES.register("resonant_drill", () -> new BlockEntityType<>(
                    ResonantDrillBlockEntity::new, FWBlocks.RESONANT_DRILL.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrystalFarmBlockEntity>> CRYSTAL_FARM =
            BLOCK_ENTITIES.register("crystal_farm", () -> new BlockEntityType<>(
                    CrystalFarmBlockEntity::new, FWBlocks.CRYSTAL_FARM.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HarmonicChamberBlockEntity>> HARMONIC_CHAMBER =
            BLOCK_ENTITIES.register("harmonic_chamber", () -> new BlockEntityType<>(
                    HarmonicChamberBlockEntity::new, FWBlocks.HARMONIC_CHAMBER.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HarmonicPodBlockEntity>> HARMONIC_POD =
            BLOCK_ENTITIES.register("harmonic_pod", () -> new BlockEntityType<>(
                    HarmonicPodBlockEntity::new, FWBlocks.HARMONIC_POD.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TameGeneratorBlockEntity>> TAME_GENERATOR =
            BLOCK_ENTITIES.register("tame_generator", () -> new BlockEntityType<>(
                    TameGeneratorBlockEntity::new, FWBlocks.TAME_GENERATOR.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PhaseBridgeBlockEntity>> PHASE_BRIDGE =
            BLOCK_ENTITIES.register("phase_bridge", () -> new BlockEntityType<>(
                    PhaseBridgeBlockEntity::new, FWBlocks.PHASE_BRIDGE.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<com.srqingchen.fieldworks.block.ProjectionBlockEntity>> PROJECTION =
            BLOCK_ENTITIES.register("projection", () -> new BlockEntityType<>(
                    com.srqingchen.fieldworks.block.ProjectionBlockEntity::new, FWBlocks.PROJECTION.get()));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GiantControllerEntity>> GIANT =
            BLOCK_ENTITIES.register("giant", () -> new BlockEntityType<>(
                    GiantControllerEntity::new, FWBlocks.RESONANCE_TOWER.get(), FWBlocks.DEEP_ELECTROLYSIS_ARRAY.get(),
                    FWBlocks.ECO_DOME.get(), FWBlocks.WARP_ANCHOR.get(), FWBlocks.HARMONIC_CATHEDRAL.get(),
                    FWBlocks.PLANETARY_ARRAY.get()));

    private FWBlockEntities() {}
}
