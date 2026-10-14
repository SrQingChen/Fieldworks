package com.srqingchen.fieldworks.registry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * 不可放置的纯罐装气体：只存在于 FluidStack / 管道 / 储罐 / 配方中。
 * 无世界方块形态、无流动逻辑(§1.3 克制：场导管只输气体介质)。
 */
public class GasFluid extends Fluid {
    private final String typeName;

    public GasFluid(String typeName) {
        this.typeName = typeName;
    }

    @Override
    public FluidType getFluidType() {
        // 懒解析：注册期未绑定，运行期查找(§26.1.2 注册顺序)
        var key = net.minecraft.resources.Identifier.fromNamespaceAndPath(
                com.srqingchen.fieldworks.Fieldworks.MODID, typeName);
        return net.neoforged.neoforge.registries.NeoForgeRegistries.FLUID_TYPES.getValue(key);
    }

    @Override
    public Item getBucket() {
        return net.minecraft.world.item.Items.AIR;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return false;
    }

    @Override
    protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState fluidState) {
        return Vec3.ZERO;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return Integer.MAX_VALUE;
    }

    @Override
    protected float getExplosionResistance() {
        return 100f;
    }

    @Override
    public float getHeight(FluidState fluidState, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public float getOwnHeight(FluidState fluidState) {
        return 0;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState fluidState) {
        return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState fluidState) {
        return true;
    }

    @Override
    public int getAmount(FluidState fluidState) {
        return 8;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        return net.minecraft.world.phys.shapes.Shapes.empty();
    }
}
