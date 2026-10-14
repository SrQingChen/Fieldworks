package com.srqingchen.fieldworks.item;

import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import com.srqingchen.fieldworks.fieldengine.FWFields;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** 潮汐日历(§14.1)："满月之夜分选效率 +20%"的排班表(§12.4 月相潮汐预报)。 */
public class TidalCalendarItem extends Item {
    public TidalCalendarItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        var phase = sl.environmentAttributes().getValue(
                net.minecraft.world.attribute.EnvironmentAttributes.MOON_PHASE, player.blockPosition());
        float gravity = FieldEngine.sample(sl, player.blockPosition(), FWFields.GRAVITY);
        sp.sendSystemMessage(Component.translatable("message.fieldworks.tidal_calendar",
                phase.name(), String.format("%.2f", gravity),
                phase.name().equals("FULL_MOON") ? "§6+20%" : "§7--"));
        return InteractionResult.CONSUME;
    }
}
