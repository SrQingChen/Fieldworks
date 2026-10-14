package com.srqingchen.fieldworks.item;

import com.srqingchen.fieldworks.block.ProjectionBlock;
import com.srqingchen.fieldworks.registry.FWBlocks;
import com.srqingchen.fieldworks.registry.FWDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * 巨物蓝图(§13 蓝图投影搭建)：
 * 右键方块顶面 → 放置投影锚点(消耗蓝图)；
 * 对投影 Shift+右键 → 自动消耗背包材料搭建，缺料聊天栏提示。
 * 变体经 {@link FWDataComponents#GIANT_VARIANT} 数据组件携带。
 */
public class GiantBlueprintItem extends Item {

    public GiantBlueprintItem(Properties props) {
        super(props);
    }

    @Nullable
    public static String variantOf(ItemStack stack) {
        Identifier id = stack.get(FWDataComponents.GIANT_VARIANT.get());
        return id == null ? null : id.getPath();
    }

    @Override
    public Component getName(ItemStack stack) {
        String v = variantOf(stack);
        if (v != null) {
            return Component.translatable("item.fieldworks.giant_blueprint." + v);
        }
        return super.getName(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display,
                                java.util.function.Consumer<Component> tooltip, TooltipFlag flag) {
        String v = variantOf(stack);
        if (v != null) {
            tooltip.accept(Component.translatable("tooltip.fieldworks.blueprint." + v).withStyle(
                    net.minecraft.ChatFormatting.DARK_GRAY));
            tooltip.accept(Component.translatable("tooltip.fieldworks.blueprint.use").withStyle(
                    net.minecraft.ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos target = ctx.getClickedPos().relative(ctx.getClickedFace());
        Player player = ctx.getPlayer();
        ItemStack stack = ctx.getItemInHand();
        String variant = variantOf(stack);
        if (variant == null || player == null) {
            return InteractionResult.PASS;
        }
        if (!level.getBlockState(target).isAir()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // 放置投影锚点
        BlockState state = FWBlocks.PROJECTION.get().defaultBlockState();
        level.setBlock(target, state, 3);
        if (level.getBlockEntity(target) instanceof com.srqingchen.fieldworks.block.ProjectionBlockEntity be) {
            be.begin(variant, player);
        }
        level.playSound(null, target, SoundEvents.BOOK_PAGE_TURN, SoundSource.BLOCKS, 1f, 0.8f);
        stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
