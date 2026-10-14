package com.srqingchen.fieldworks.machine;

import com.mojang.serialization.MapCodec;
import com.srqingchen.fieldworks.item.WrenchItem;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import com.srqingchen.fieldworks.registry.FWItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * 机器方块(§16.1 朝向体系)：放置时面向玩家的一面为主面板(Facing)，
 * 五面贴图分工；wrench 旋转朝向不改 IO；六面 IO 独立配置；
 * 红石比较器输出(进度/场强/储量，§10.3)。
 */
public class MachineBlock extends BaseEntityBlock {
    public static final EnumProperty<net.minecraft.core.Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** 激活态(§16.1 激活贴图+发光，Thermal 式)。 */
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final MapCodec<MachineBlock> CODEC = simpleCodec(MachineBlock::new);

    private final MachineSpec spec;

    /** 共享 CODEC 用无参构造(规格取默认占位，实际实例经由带 spec 构造)。 */
    public MachineBlock(Properties props) {
        this(props, MachineSpecs.custom("machine_placeholder", 1, MachineSpec.Family.MATERIAL));
    }

    public MachineBlock(Properties props, MachineSpec spec) {
        super(props);
        this.spec = spec;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    public MachineSpec spec() {
        return spec;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public int getLightEmission(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos) {
        // §16.1 激活发光：家族发光语言的方块层
        return Boolean.TRUE.equals(state.getOptionalValue(ACTIVE).orElse(Boolean.FALSE)) ? 6 : 0;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                           Player player, InteractionHand hand, BlockHitResult hit) {
        // wrench 旋转(§10.3)：拆迁保数据、改朝向不改 IO
        if (stack.getItem() == FWItems.WRENCH.get()) {
            if (!level.isClientSide()) {
                level.setBlock(pos, state.setValue(FACING, state.getValue(FACING).getClockWise()), Block.UPDATE_ALL);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.COPPER_BULB_TURN_OFF,
                        net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        // 流体容器(水桶等)：与机器罐交互(注入/舀出)，不打开 GUI、不放水到世界。
        // 客户端与"交互失败"两种情况都返回 SUCCESS——阻断客户端对水桶 use() 的预测执行
        // (2026-10-12 修复"开 UI 同时把水放到指向面"：根因是客户端走到了物品使用阶段)。
        if (stack.getItem() instanceof net.minecraft.world.item.BucketItem
                && level.getBlockEntity(pos) instanceof MachineBlockEntity be && be.spec.tanks() > 0) {
            if (!level.isClientSide()) {
                boolean handled = net.neoforged.neoforge.transfer.fluid.FluidUtil
                        .interactWithFluidHandler(player, hand, level, pos, hit.getDirection());
                if (!handled && player instanceof ServerPlayer sp) {
                    be.openMenu(sp);   // 罐已满/不匹配 → 落回打开 GUI
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // 客户端必须消费：否则客户端会继续预测手中物品的使用(如水桶放置)，服务端校正前会出现"凭空放水"
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be && player instanceof ServerPlayer sp) {
            be.openMenu(sp);
        }
        return InteractionResult.CONSUME;
    }

    // ── 比较器(§10.3) ──

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof MachineBlockEntity be ? be.comparatorSignal() : 0;
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 0;
    }

    // ── BE 生命周期(§6.6 订阅/退订) ──

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(FWBlockEntities.MACHINE.get(), pos, state, spec);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, FWBlockEntities.MACHINE.get(), MachineBlockEntity::serverTick);
    }

}
