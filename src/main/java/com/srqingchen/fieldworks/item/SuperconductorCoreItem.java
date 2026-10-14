package com.srqingchen.fieldworks.item;

import com.srqingchen.fieldworks.registry.FWDataComponents;
import com.srqingchen.fieldworks.registry.FWItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * 超导谐律核心(261014 T10 终极饰品，necklace)：
 * <ul>
 *   <li><b>转电盾</b>：受击伤害按 {@link #COST_PER_HP} FE/HP 转为电容耗电抵挡——
 *       电量不足则按比例减伤；<b>完全抵挡</b>时同时清除本次受击新增的所有状态效果</li>
 *   <li><b>谐律增幅</b>：按实际耗电累计，每 {@link #FE_PER_STACK} FE 叠一层随机属性增幅
 *       (攻击+1/攻速+4%/移速+2%/护甲+1/击退抗性+4%)——层数可叠加，每层独立 30s 持续</li>
 *   <li><b>充能</b>：手持右键消耗背包内谐振燃料 +{@link #FUEL_CHARGE} FE；
 *       佩戴时身处强复合场中被动涓流充能</li>
 * </ul>
 */
public class SuperconductorCoreItem extends Item {
    public static final long CAPACITY = 200_000;
    /** 每抵挡 1 HP 消耗的 FE。 */
    public static final double COST_PER_HP = 400;
    /** 每叠一层属性增幅所需的累计耗电 FE。 */
    public static final long FE_PER_STACK = 2000;
    /** 一份谐振燃料的充能量。 */
    public static final long FUEL_CHARGE = 40_000;
    /** 单层增幅持续时间(tick)。 */
    public static final int STACK_DURATION = 600;

    public SuperconductorCoreItem(Properties props) {
        super(props);
    }

    public static long energyOf(ItemStack stack) {
        Long e = stack.get(FWDataComponents.STORED_ENERGY.get());
        return e == null ? 0 : e;
    }

    public static void setEnergy(ItemStack stack, long value) {
        stack.set(FWDataComponents.STORED_ENERGY.get(), Math.max(0, Math.min(CAPACITY, value)));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return (int) Math.round(13.0 * energyOf(stack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x35D8CE;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.fieldworks.superconductor_core.desc1")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("item.fieldworks.superconductor_core.desc2")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("tooltip.fieldworks.core.energy", energyOf(stack), CAPACITY)
                .withStyle(ChatFormatting.GREEN));
        tooltip.accept(Component.translatable("tooltip.fieldworks.core.charge_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    /** 右键：消耗背包内谐振燃料充能。 */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (energyOf(stack) >= CAPACITY) return InteractionResult.PASS;
        int slot = findFuel(player);
        if (slot < 0) return InteractionResult.PASS;
        ItemStack fuel = player.getInventory().getItem(slot);
        fuel.shrink(1);
        if (fuel.isEmpty()) player.getInventory().setItem(slot, ItemStack.EMPTY);
        setEnergy(stack, Math.min(CAPACITY, energyOf(stack) + FUEL_CHARGE));
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.8f, 1.4f);
        player.sendSystemMessage(Component.translatable("message.fieldworks.core.charged", FUEL_CHARGE));
        return InteractionResult.CONSUME;
    }

    private static int findFuel(Player player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(FWItems.RESONANCE_FUEL.get())) return i;
        }
        return -1;
    }

    @Nullable
    public static List<Component> chargeTooltip(long energy) {
        return List.of(Component.translatable("tooltip.fieldworks.core.energy", energy, CAPACITY));
    }
}
