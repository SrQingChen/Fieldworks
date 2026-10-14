package com.srqingchen.fieldworks.command;

import com.mojang.brigadier.CommandDispatcher;
import com.srqingchen.fieldworks.carbon.CarbonState;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import com.srqingchen.fieldworks.fieldengine.FieldSnapshot;
import com.srqingchen.fieldworks.fieldengine.FWFields;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /fieldworks 指令(§21.4)：perf | carbon | field [set] | sample。
 * perf 输出场引擎分项报告(D5 的运维层)。
 */
public final class FWCommands {

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("fieldworks")
                
                .then(Commands.literal("sample").executes(ctx -> {
                    if (ctx.getSource().getPlayerOrException() instanceof ServerPlayer sp) {
                        FieldSnapshot snap = FieldEngine.snapshot(sp.level() instanceof net.minecraft.server.level.ServerLevel srl ? srl : null, sp.blockPosition());
                        StringBuilder sb = new StringBuilder("§b⌖ 场学采样 §7");
                        sb.append(String.format("温%.1f 湿%.0f 光%d 磁%.2f 引%.2f 辐%.0f cp%.0f | %s %s",
                                snap.value(FWFields.TEMPERATURE), snap.value(FWFields.HUMIDITY),
                                (int) snap.value(FWFields.LIGHT), snap.value(FWFields.MAGNETIC),
                                snap.value(FWFields.GRAVITY), snap.value(FWFields.RADIATION),
                                snap.value(FWFields.CARBON), snap.atmosphereType(), snap.traits()));
                        sp.sendSystemMessage(Component.literal(sb.toString()));
                    }
                    return 1;
                }))
                .then(Commands.literal("carbon").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal(
                            String.format("§b[碳压计] §7cp=%.1f TempMult=%.3f 捕获效率=%.2f(平衡点 200)",
                                    CarbonState.cp(), CarbonState.tempMultiplier(), CarbonState.captureEfficiency())), false);
                    return 1;
                }))
                .then(Commands.literal("perf").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(ctx -> {
                    var engine = FieldEngine.data();
                    int active = 0;
                    for (var inst : engine.instances.values()) {
                        if (!inst.loadedChunks.isEmpty()) active++;
                    }
                    final int activeF = active;
                    ctx.getSource().sendSuccess(() -> Component.literal(String.format(
                            "§b[fieldworks perf] §7实例 %d(活跃 %d) 包络 %d 订阅区 %d",
                            engine.instances.size(), activeF,
                            com.srqingchen.fieldworks.envelope.EnvelopeManager.all().size(),
                            FieldEngine.subscriptionCount())), false);
                    return 1;
                }))
                .then(Commands.literal("setcp").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("value", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 1000))
                                .executes(ctx -> {
                                    int v = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "value");
                                    FieldEngine.data().carbonCp = v;
                                    FieldEngine.data().setDirty();
                                    ctx.getSource().sendSuccess(() -> Component.literal("§a[Fieldworks] cp = " + v), true);
                                    return 1;
                                }))));
    }

    private FWCommands() {}
}
