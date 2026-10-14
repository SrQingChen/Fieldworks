package com.srqingchen.fieldworks.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import java.util.function.Consumer;

/**
 * 场学饰品基类(261014 T10)：
 * 普通物品 + 槽位标签(curios:ring 等)即可佩戴；行为效果在
 * {@code compat.curios.AccessoryEvents} 按物品分派(未装 Curios 时无槽位、效果自然不生效)。
 */
public class AccessoryItem extends Item {
    /** tooltip 描述键(item.fieldworks.&lt;id&gt;.desc)。 */
    public final String descKey;

    public AccessoryItem(Properties props, String descKey) {
        super(props);
        this.descKey = descKey;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext ctx, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable(descKey).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.accept(Component.translatable("tooltip.fieldworks.accessory.slot_hint")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
