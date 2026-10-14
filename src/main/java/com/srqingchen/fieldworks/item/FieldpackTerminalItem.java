package com.srqingchen.fieldworks.item;

import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import com.srqingchen.fieldworks.fieldengine.FieldSnapshot;
import com.srqingchen.fieldworks.fieldengine.FWFields;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** 场谱背包终端(§14.1)：背包里查全场学读数(T3 级便携)。 */
public class FieldpackTerminalItem extends Item {
    public FieldpackTerminalItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        FieldSnapshot snap = FieldEngine.snapshot(sl, player.blockPosition());
        sp.sendSystemMessage(Component.literal("§b⌖ 场谱终端 §7" + snap.atmosphereType()));
        sp.sendSystemMessage(Component.literal(String.format(
                "§7温 %.1f℃ 湿 %.0f%% 光 %d 磁 %.2fT 引 %.2fg 辐 %.0fμSv cp %.0f",
                snap.value(FWFields.TEMPERATURE), snap.value(FWFields.HUMIDITY),
                (int) snap.value(FWFields.LIGHT), snap.value(FWFields.MAGNETIC),
                snap.value(FWFields.GRAVITY), snap.value(FWFields.RADIATION),
                snap.value(FWFields.CARBON))));
        sp.sendSystemMessage(Component.literal("§8" + snap.traits()));
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.fieldworks.fieldpack_terminal").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
