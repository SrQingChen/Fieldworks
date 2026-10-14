package com.srqingchen.fieldworks.item;

import com.srqingchen.fieldworks.api.atmosphere.AtmosphereType;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import com.srqingchen.fieldworks.fieldengine.FieldSnapshot;
import com.srqingchen.fieldworks.fieldengine.FWFields;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * 探测杖(§17 T0，§8.3 分钟 3—6)：潜行右键空气 → 四条简式仪表 + 大气类型名 + 风味描述。
 * 耐久 16 次，耗尽即损坏；P1 光谱仪部件升级后免耐久。
 */
public class ProbeStaffItem extends Item {

    public ProbeStaffItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(level instanceof ServerLevel sl) || !(player instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }
        boolean upgraded = player.getInventory().countItem(
                com.srqingchen.fieldworks.registry.FWItems.SPECTRUM_MODULE.get()) > 0;
        FieldSnapshot snap = FieldEngine.snapshot(sl, player.blockPosition());
        AtmosphereType atmo = snap.atmosphere();
        sp.sendSystemMessage(Component.literal("§b⌖ 场学读数 §7[" + (atmo != null ? atmo.displayName().getString() : "?") + "]"));
        sp.sendSystemMessage(Component.literal(String.format(
                "§7 温 §f%.1f℃ §7 湿 §f%.0f%% §7 光 §f%d §7 压 §f%.1f",
                snap.value(FWFields.TEMPERATURE), snap.value(FWFields.HUMIDITY),
                (int) snap.value(FWFields.LIGHT), snap.value(FWFields.VACUUM) / 1000f)));
        if (atmo != null) {
            sp.sendSystemMessage(Component.literal("§8 ").append(atmo.flavor()));
        }
        com.srqingchen.fieldworks.util.AdvancementHelper.award(sp, "first_reading");
        // 特征值(T1 光谱仪部件后精确显示)
        if (upgraded) {
            var t = snap.traits();
            sp.sendSystemMessage(Component.literal(String.format(
                    "§d⌖ %s", t)));
        } else if (!upgraded) {
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(1, sp, EquipmentSlot.MAINHAND);
            }
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("tooltip.fieldworks.probe_staff").withStyle(net.minecraft.ChatFormatting.DARK_AQUA));
    }
}
