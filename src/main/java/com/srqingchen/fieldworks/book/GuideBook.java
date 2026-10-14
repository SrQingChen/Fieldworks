package com.srqingchen.fieldworks.book;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.config.FWConfig;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * 新手引导(§18)：
 * ① 探测杖合成 → 首章解锁即送《场域学入门》(Patchouli 可选)；
 * ② 首次进入新大气类型 → 聊天栏风味提示("这片森林的空气尝起来很清新"，可关 §8.3)；
 * 只在首次遇到时提示一次；整合包模式可配置开局直发书。
 */
public final class GuideBook {

    /** 探测杖合成 → 送书(§18 获取)。 */
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (event.getCrafting().getItem() != com.srqingchen.fieldworks.registry.FWItems.PROBE_STAFF.get()) return;
        var data = sp.getPersistentData();
        if (data.getBooleanOr("fieldworks_book_given", false)) return;
        data.putBoolean("fieldworks_book_given", true);
        giveBook(sp);
    }

    /** 登录直发(整合包模式 §8.3/附录 B giveBookOnStart)。 */
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!FWConfig.GIVE_BOOK_ON_START.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer sp) || sp.isSpectator()) return;
        if (sp.getPersistentData().getBooleanOr("fieldworks_book_given", false)) return;
        sp.getPersistentData().putBoolean("fieldworks_book_given", true);
        giveBook(sp);
    }

    private static void giveBook(ServerPlayer sp) {
        if (!net.neoforged.fml.ModList.get().isLoaded("patchouli")) return;
        Hooks.give(sp);
    }

    private static final class Hooks {
        static void give(ServerPlayer sp) {
            try {
                var stack = vazkii.patchouli.api.PatchouliAPI.get()
                        .getBookStack(Identifier.fromNamespaceAndPath(Fieldworks.MODID, "manual"));
                if (!stack.isEmpty() && sp.getInventory().add(stack)) {
                    sp.sendSystemMessage(Component.translatable("message.fieldworks.book_given"));
                }
            } catch (Throwable t) {
                Fieldworks.LOGGER.warn("[Fieldworks] Patchouli 手册下发失败", t);
            }
        }
    }

    /** 大气风味提示(§8.3 分钟 0—3)：首次进入新大气类型提示一次。 */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;
        if (!FWConfig.ATMOSPHERE_HINTS.get()) return;
        MinecraftServer srv = sp.level().getServer();
        if (srv == null || sp.level().getGameTime() % 200 != sp.getId() % 200) return;
        if (!(sp.level() instanceof ServerLevel sl)) return;
        var snap = com.srqingchen.fieldworks.fieldengine.FieldEngine.snapshot(sl, sp.blockPosition());
        Identifier atmo = snap.atmosphereType();
        CompoundTag data = sp.getPersistentData();
        String key = "fw_atmo_" + atmo.getPath();
        if (!data.getBooleanOr(key, false)) {
            data.putBoolean(key, true);
            var type = com.srqingchen.fieldworks.api.atmosphere.AtmosphereRegistry.get(atmo);
            if (type != null) {
                sp.sendSystemMessage(Component.literal("§7⌖ ").append(type.displayName())
                        .append(Component.literal(" §8")).append(type.flavor()));
            }
            // 五方水土(§8.5)：探测 5 种不同大气
            int seen = 0;
            for (String k : data.keySet()) {
                if (k.startsWith("fw_atmo_") && data.getBooleanOr(k, false)) seen++;
            }
            if (seen >= 5) {
                com.srqingchen.fieldworks.util.AdvancementHelper.award(sp, "five_airs");
            }
        }
    }

    private GuideBook() {}
}
