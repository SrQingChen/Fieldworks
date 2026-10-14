package com.srqingchen.fieldworks.harmonic;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.Set;

/**
 * 谐律风格(V1.2 语义群组，§12.1)：按附魔语义群组偏置——
 * "给武器附魔容易出亡灵杀手类、给工具附魔容易出时运类"的跨物品主题体验。
 * 群组成员表为数据包可改白名单(§19.3)。
 */
public enum HarmonicStyle {
    /** 祸律(杀伐高频)：伤害与敌意主题 ×2。 */
    HARM(Set.of(Enchantments.SHARPNESS, Enchantments.SMITE, Enchantments.BANE_OF_ARTHROPODS,
            Enchantments.KNOCKBACK, Enchantments.FIRE_ASPECT, Enchantments.THORNS, Enchantments.IMPALING)),
    /** 禄律(丰饶泛音)：收获与幸运主题 ×2。 */
    FORTUNE(Set.of(Enchantments.FORTUNE, Enchantments.LOOTING, Enchantments.LUCK_OF_THE_SEA, Enchantments.LURE,
            Enchantments.EFFICIENCY, Enchantments.SILK_TOUCH)),
    /** 庇律(庇护低鸣)：守护与延续主题 ×2。 */
    WARD(Set.of(Enchantments.PROTECTION, Enchantments.FIRE_PROTECTION, Enchantments.BLAST_PROTECTION,
            Enchantments.PROJECTILE_PROTECTION, Enchantments.UNBREAKING, Enchantments.FEATHER_FALLING,
            Enchantments.RESPIRATION, Enchantments.MENDING)),
    /** 无风格：原版均匀。 */
    NONE(Set.of());

    public final Set<ResourceKey<net.minecraft.world.item.enchantment.Enchantment>> group;

    HarmonicStyle(Set<ResourceKey<net.minecraft.world.item.enchantment.Enchantment>> group) {
        this.group = group;
    }
}
