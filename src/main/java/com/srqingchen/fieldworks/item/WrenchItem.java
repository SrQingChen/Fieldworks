package com.srqingchen.fieldworks.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** 扳手(§10.3)：旋转机器朝向且拆迁保数据。 */
public class WrenchItem extends Item {
    public WrenchItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.fieldworks.wrench").withStyle(net.minecraft.ChatFormatting.GRAY));
    }
}
