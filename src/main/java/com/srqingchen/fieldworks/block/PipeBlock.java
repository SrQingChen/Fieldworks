package com.srqingchen.fieldworks.block;

import com.mojang.serialization.MapCodec;
import com.srqingchen.fieldworks.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * 管道方块 v3(261014 T2 三族管线)：
 * <ul>
 *   <li>261014 合并：气体管道并入流体管道(气体也是流体，类型锁防混)——{@link PipeKind#FLUID} 一种通吃全部流体</li>
 *   <li>新增 {@link PipeKind#ITEM}(物品) 与 {@link PipeKind#ENERGY}(能量) 两族，几何/连接语义与流体族同构</li>
 *   <li>连接状态进 blockstate：每向 {@link Conn} 三态(none/pipe/machine)；multipart 模型；VoxelShape 动态</li>
 * </ul>
 */
public class PipeBlock extends BaseEntityBlock {
    public static final MapCodec<PipeBlock> CODEC = simpleCodec(p -> new PipeBlock(p, PipeKind.FLUID));

    /** 管道族：决定运载内容与机器连接判定。 */
    public enum PipeKind {
        FLUID("fluid"),
        ITEM("item"),
        ENERGY("energy");

        public final String path;

        PipeKind(String path) {
            this.path = path;
        }

        /** 该族管道是否与该机器互连(§10.5 相连即工作)。 */
        public boolean canConnectMachine(MachineBlockEntity m) {
            return switch (this) {
                case FLUID -> !m.tanks().isEmpty();
                case ITEM -> m.spec.inputSlots() > 0 || m.spec.outputSlots() > 0;
                case ENERGY -> m.getEnergyMax() > 0;
            };
        }
    }

    /** 六向连接态。 */
    public enum Conn implements net.minecraft.util.StringRepresentable {
        NONE("none"), PIPE("pipe"), MACHINE("machine");
        private final String serial;

        Conn(String s) {
            this.serial = s;
        }

        @Override
        public String getSerializedName() {
            return serial;
        }
    }

    public static final EnumProperty<Conn> NORTH = EnumProperty.create("north", Conn.class);
    public static final EnumProperty<Conn> SOUTH = EnumProperty.create("south", Conn.class);
    public static final EnumProperty<Conn> EAST = EnumProperty.create("east", Conn.class);
    public static final EnumProperty<Conn> WEST = EnumProperty.create("west", Conn.class);
    public static final EnumProperty<Conn> UP = EnumProperty.create("up", Conn.class);
    public static final EnumProperty<Conn> DOWN = EnumProperty.create("down", Conn.class);

    public static final Map<Direction, EnumProperty<Conn>> PROP_BY_DIR = Map.of(
            Direction.NORTH, NORTH, Direction.SOUTH, SOUTH, Direction.EAST, EAST,
            Direction.WEST, WEST, Direction.UP, UP, Direction.DOWN, DOWN);

    public final PipeKind kind;
    private final java.util.function.Function<BlockState, VoxelShape> shapes;

    /** 中心 6×6×6 + 6×5×6 臂(Mek small transmitter 同规格)。 */
    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final VoxelShape ARM_N = Block.box(5, 5, 0, 11, 11, 5);
    private static final VoxelShape ARM_S = Block.box(5, 5, 11, 11, 11, 16);
    private static final VoxelShape ARM_E = Block.box(11, 5, 5, 16, 11, 11);
    private static final VoxelShape ARM_W = Block.box(0, 5, 5, 5, 11, 11);
    private static final VoxelShape ARM_U = Block.box(5, 11, 5, 11, 16, 11);
    private static final VoxelShape ARM_D = Block.box(5, 0, 5, 11, 5, 11);
    private static final Map<Direction, VoxelShape> ARM_BY_DIR = Map.of(
            Direction.NORTH, ARM_N, Direction.SOUTH, ARM_S, Direction.EAST, ARM_E,
            Direction.WEST, ARM_W, Direction.UP, ARM_U, Direction.DOWN, ARM_D);
    /** 机器端法兰 8×8(参考文档 §1.4 PULL 法兰观感)。 */
    private static final VoxelShape FL_N = Block.box(4, 4, 0, 12, 12, 2);
    private static final VoxelShape FL_S = Block.box(4, 4, 14, 12, 12, 16);
    private static final VoxelShape FL_E = Block.box(14, 4, 4, 16, 12, 12);
    private static final VoxelShape FL_W = Block.box(0, 4, 4, 2, 12, 12);
    private static final VoxelShape FL_U = Block.box(4, 14, 4, 12, 16, 12);
    private static final VoxelShape FL_D = Block.box(4, 0, 4, 12, 2, 12);
    private static final Map<Direction, VoxelShape> FLANGE_BY_DIR = Map.of(
            Direction.NORTH, FL_N, Direction.SOUTH, FL_S, Direction.EAST, FL_E,
            Direction.WEST, FL_W, Direction.UP, FL_U, Direction.DOWN, FL_D);

    public PipeBlock(Properties props, PipeKind kind) {
        super(props);
        this.kind = kind;
        registerDefaultState(stateDefinition.any()
                .setValue(NORTH, Conn.NONE).setValue(SOUTH, Conn.NONE)
                .setValue(EAST, Conn.NONE).setValue(WEST, Conn.NONE)
                .setValue(UP, Conn.NONE).setValue(DOWN, Conn.NONE));
        this.shapes = getShapeForEachState(state -> {
            VoxelShape shape = CORE;
            for (Map.Entry<Direction, EnumProperty<Conn>> e : PROP_BY_DIR.entrySet()) {
                Conn c = state.getValue(e.getValue());
                if (c != Conn.NONE) {
                    shape = Shapes.or(shape, ARM_BY_DIR.get(e.getKey()));
                    if (c == Conn.MACHINE) {
                        shape = Shapes.or(shape, FLANGE_BY_DIR.get(e.getKey()));
                    }
                }
            }
            return shape;
        });
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    // ── 连接判定(参考文档 §1.3：管-管必连；机器按族能力) ──

    public Conn connectTo(BlockGetter level, BlockPos pos, Direction dir) {
        BlockPos nPos = pos.relative(dir);
        BlockState nState = level.getBlockState(nPos);
        if (nState.getBlock() instanceof PipeBlock other && other.kind == this.kind) {
            return Conn.PIPE;
        }
        if (level.getBlockEntity(nPos) instanceof MachineBlockEntity m && kind.canConnectMachine(m)) {
            return Conn.MACHINE;
        }
        return Conn.NONE;
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext ctx) {
        BlockState state = defaultBlockState();
        for (Map.Entry<Direction, EnumProperty<Conn>> e : PROP_BY_DIR.entrySet()) {
            state = state.setValue(e.getValue(), connectTo(ctx.getLevel(), ctx.getClickedPos(), e.getKey()));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction directionToNeighbour, BlockPos neighbourPos,
                                     BlockState neighbourState, RandomSource random) {
        return state.setValue(PROP_BY_DIR.get(directionToNeighbour),
                connectTo(level, pos, directionToNeighbour));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return this.shapes.apply(state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case FLUID -> new PipeBlockEntity(pos, state);
            case ITEM -> new ItemPipeBlockEntity(pos, state);
            case ENERGY -> new EnergyPipeBlockEntity(pos, state);
        };
    }

    @Override
    @SuppressWarnings("unchecked")
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return switch (kind) {
            case FLUID -> (BlockEntityTicker<T>) createTickerHelper(type,
                    com.srqingchen.fieldworks.registry.FWBlockEntities.PIPE.get(),
                    (BlockEntityTicker<PipeBlockEntity>) (l, p2, s2, be) ->
                            PipeBlockEntity.serverTick((ServerLevel) l, p2, s2, be));
            case ITEM -> (BlockEntityTicker<T>) createTickerHelper(type,
                    com.srqingchen.fieldworks.registry.FWBlockEntities.ITEM_PIPE.get(),
                    (BlockEntityTicker<ItemPipeBlockEntity>) (l, p2, s2, be) ->
                            ItemPipeBlockEntity.serverTick((ServerLevel) l, p2, s2, be));
            case ENERGY -> (BlockEntityTicker<T>) createTickerHelper(type,
                    com.srqingchen.fieldworks.registry.FWBlockEntities.ENERGY_PIPE.get(),
                    (BlockEntityTicker<EnergyPipeBlockEntity>) (l, p2, s2, be) ->
                            EnergyPipeBlockEntity.serverTick((ServerLevel) l, p2, s2, be));
        };
    }

}
