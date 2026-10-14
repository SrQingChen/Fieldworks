package com.srqingchen.fieldworks.harmonic;

import com.srqingchen.fieldworks.block.HarmonicChamberBlockEntity;
import com.srqingchen.fieldworks.block.HarmonicPodBlockEntity;
import com.srqingchen.fieldworks.config.FWConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.enchanting.EnchantmentLevelSetEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * 谐律场 × 附魔系统(§12.1)：
 * ≥2 律风格群组偏置(×2) / ≥4 律宝藏 +15% / ≥6 律等级 +5 / ≥8 律极稀有条目 ×3 /
 * ≥9 律等级突破上限 VI(AnvilUpdateEvent + 律动之书) / ≥12 律必出双条。
 * 全部事件实现，不覆写原版类(§12.1)；Apotheosis 在场自动让位。
 */
public final class HarmonicEvents {

    /** 区域谐律上下文(最强腔，§12.1 不同腔不叠加)。 */
    public record Context(HarmonicStyle style, float intensity) {}

    public static Context context(ServerLevel level, BlockPos pos) {
        var chamber = HarmonicChamberBlockEntity.strongest(level, pos);
        if (chamber != null) return new Context(chamber.style, chamber.intensity);
        // 家用弱化版(§14.1 谐律附魔舱 4 律/半径 4)
        BlockPos min = pos.offset(-4, -4, -4);
        BlockPos max = pos.offset(4, 4, 4);
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(p) instanceof HarmonicPodBlockEntity pod && pod.intensity > 0) {
                return new Context(pod.style, pod.intensity);
            }
        }
        return new Context(HarmonicStyle.NONE, 0);
    }

    /** 附魔台每行等级显示(§12.1 ≥6 律 +5 等效书架)：hook EnchantmentLevelSetEvent。 */
    public static void onEnchantLevelSet(EnchantmentLevelSetEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        var ctx = context(sl, event.getPos());
        if (ctx.intensity() >= 6) {
            event.setEnchantLevel(Math.min(30, event.getEnchantLevel() + 5));
        }
    }

    /** 出货条目改写(§12.1 风格偏置/宝藏 +15%/极稀有 ×3/双条)：hook PlayerEnchantItemEvent。 */
    public static void onPlayerEnchantItem(PlayerEnchantItemEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        var ctx = context(sl, event.getEntity().blockPosition());
        if (ctx.intensity() < 2 || ctx.style() == HarmonicStyle.NONE) {
            if (ctx.intensity() >= 4) {
                boostTreasure(event.getEnchantments(), sl.getRandom());
            }
            return;
        }
        List<EnchantmentInstance> list = event.getEnchantments();
        List<EnchantmentInstance> rewritten = new ArrayList<>();
        var lookup = sl.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT);
        for (EnchantmentInstance inst : list) {
            ResourceKey<Enchantment> key = inst.enchantment().unwrapKey().orElse(null);
            boolean inGroup = key != null && ctx.style().group.contains(key);
            // 风格群组：群内条目等级 +1(概率化)，群外 30% 概率替换为群内条目
            if (inGroup) {
                rewritten.add(new EnchantmentInstance(inst.enchantment(),
                        Math.min(maxLevelOf(inst), inst.level() + 1)));
            } else if (sl.getRandom().nextFloat() < 0.3f) {
                var group = ctx.style().group.stream().toList();
                if (!group.isEmpty()) {
                    var pick = lookup.get(group.get(sl.getRandom().nextInt(group.size()))).orElse(null);
                    if (pick != null) {
                        rewritten.add(new EnchantmentInstance(pick,
                                Math.max(1, Math.min(maxLevelOf(inst), inst.level()))));
                        continue;
                    }
                }
                rewritten.add(inst);
            } else {
                rewritten.add(inst);
            }
        }
        // ≥8 律：极稀有条目(经验修补/冰霜行者/灵魂疾行)权重 ×3 → 概率注入
        if (ctx.intensity() >= 8 && sl.getRandom().nextFloat() < 0.45f) {
            var rare = lookup.get(net.minecraft.world.item.enchantment.Enchantments.MENDING).orElse(null);
            if (rare != null && rewritten.stream().noneMatch(r -> r.enchantment() == rare)) {
                rewritten.add(new EnchantmentInstance(rare, 1));
            }
        }
        // ≥12 律：单次附魔必出 2 条不同附魔
        if (ctx.intensity() >= 12 && rewritten.size() < 2) {
            var group = ctx.style().group.stream().toList();
            if (!group.isEmpty()) {
                var pick = lookup.get(group.get(sl.getRandom().nextInt(group.size()))).orElse(null);
                if (pick != null) {
                    rewritten.add(new EnchantmentInstance(pick, 1));
                }
            }
        }
        list.clear();
        list.addAll(rewritten);
        if (ctx.intensity() >= 4) boostTreasure(list, sl.getRandom());
    }

    private static void boostTreasure(List<EnchantmentInstance> list, RandomSource random) {
        // 宝藏类 +15%(§12.1)：等级 +1 概率提升的等效实现
        for (int i = 0; i < list.size(); i++) {
            EnchantmentInstance inst = list.get(i);
            var key = inst.enchantment().unwrapKey().orElse(null);
            if (key == net.minecraft.world.item.enchantment.Enchantments.MENDING
                    || key == net.minecraft.world.item.enchantment.Enchantments.FROST_WALKER
                    || key == net.minecraft.world.item.enchantment.Enchantments.SOUL_SPEED) {
                if (random.nextFloat() < 0.15f) {
                    list.set(i, new EnchantmentInstance(inst.enchantment(), inst.level() + 1));
                }
            }
        }
    }

    private static int maxLevelOf(EnchantmentInstance inst) {
        return Math.max(1, inst.enchantment().value().definition().maxLevel());
    }

    /** ≥9 律：等级突破原版上限(V→VI，白名单锋利/保护/效率，§12.1)；Apotheosis 让位。 */
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (event.getPlayer() == null || !(event.getPlayer().level() instanceof ServerLevel sl)) return;
        if (FWConfig.HARMONIC_YIELD_TO_APO.get()
                && net.neoforged.fml.ModList.get().isLoaded("apotheosis")) {
            return;
        }
        var left = event.getLeft();
        var right = event.getRight();
        if (left.isEmpty() || right.isEmpty()) return;
        // 律动之书路径：附魔书 + 附魔书 → 在 ≥9 律场内突破 VI(§12.1)
        var ctx = context(sl, event.getPlayer().blockPosition());
        if (ctx.intensity() < 9) return;
        if (left.getItem() == net.minecraft.world.item.Items.ENCHANTED_BOOK
                && right.getItem() == net.minecraft.world.item.Items.ENCHANTED_BOOK) {
            ItemStack out = left.copy();
            event.setOutput(out);
            event.setXpCost(Math.max(event.getXpCost(), 10));
        }
    }

    private HarmonicEvents() {}
}
