package com.srqingchen.fieldworks.furnace;

import com.srqingchen.fieldworks.config.FWConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 原版熔炉持续燃烧模式(§12.5)：
 * 手持打火石右键 → 进入持续燃烧(方块标记)；当前燃料烧完自动续燃(空转保温)；
 * 玩家放入原料 → 自动解除交还原版；不加速不省料，纯 QoL + 4 格余温(+2℃，§12.5)。
 * 技术锚点：BE 附加数据 + 交互事件 + 燃烧结束续燃(AT 公开字段)；无 coremod。
 */
public final class ContinuousFiringManager {
    private static final Map<String, BlockPos> TRACKED = new HashMap<>();

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide()) return;
        if (!FWConfig.FURNACE_CONTINUOUS.get()) return;
        Player player = event.getEntity();
        if (player.getItemInHand(InteractionHand.MAIN_HAND).getItem() != Items.FLINT_AND_STEEL
                && player.getItemInHand(InteractionHand.OFF_HAND).getItem() != Items.FLINT_AND_STEEL) {
            return;
        }
        var state = event.getLevel().getBlockState(event.getPos());
        if (!state.is(Blocks.FURNACE)) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        String key = key(sl, event.getPos());
        if (TRACKED.containsKey(key)) {
            TRACKED.remove(key);
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.translatable("message.fieldworks.furnace.off"));
            }
        } else {
            TRACKED.put(key, event.getPos().immutable());
            if (player instanceof ServerPlayer sp) {
                sp.sendSystemMessage(Component.translatable("message.fieldworks.furnace.on"));
            }
            sl.playSound(null, event.getPos(), SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 0.7f, 1.0f);
        }
        event.setCanceled(true);
    }

    public static void onServerTick(ServerTickEvent.Post event) {
        if (TRACKED.isEmpty() || event.getServer().overworld().getGameTime() % 20 != 0) return;
        Iterator<Map.Entry<String, BlockPos>> it = TRACKED.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            String[] parts = e.getKey().split("\\|");
            var level = event.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION, net.minecraft.resources.Identifier.parse(parts[0])));
            if (level == null) {
                it.remove();
                continue;
            }
            BlockPos pos = e.getValue();
            if (!(level.getBlockEntity(pos) instanceof AbstractFurnaceBlockEntity furnace)) {
                it.remove();
                continue;
            }
            if (!furnace.getBlockState().is(Blocks.FURNACE)) {
                it.remove();
                continue;
            }
            // 玩家放入原料 → 自动解除(交还原版，§12.5 规则 3)
            if (!furnace.getItem(0).isEmpty()) {
                it.remove();
                continue;
            }
            // 燃料烧完且无待烧物 → 自动续燃(空转保温)
            if (furnace.litTimeRemaining <= 0 && !furnace.getItem(1).isEmpty()) {
                var fuel = furnace.getItem(1);
                int burn = fuel.getBurnTime(null, level.fuelValues());
                if (burn > 0) {
                    furnace.litTimeRemaining = burn;
                    furnace.litTotalTime = burn;
                    var remainder = fuel.getCraftingRemainder();
                    fuel.shrink(1);
                    if (fuel.isEmpty()) furnace.setItem(1, remainder != null ? remainder.create() : net.minecraft.world.item.ItemStack.EMPTY);
                    furnace.setChanged();
                }
            }
        }
    }

    /** 余温锚点(§12.5)：持续燃烧的熔炉向周围 4 格提供 +2℃(最多 4 台叠加到 +5℃)。 */
    public static float wasteHeat(ServerLevel level, BlockPos pos) {
        if (!FWConfig.FURNACE_CONTINUOUS.get()) return 0;
        float heat = 0;
        for (var e : TRACKED.entrySet()) {
            BlockPos p = e.getValue();
            if (p.distSqr(pos) <= 16 && level.getBlockState(p).is(Blocks.FURNACE)) {
                heat += 2f;
            }
        }
        return Math.min(5f, heat);
    }

    private static String key(ServerLevel level, BlockPos pos) {
        return level.dimension().identifier() + "|" + pos.asLong();
    }

    private ContinuousFiringManager() {}
}
