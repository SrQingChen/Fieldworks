package com.srqingchen.fieldworks.api.harmonic;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * 附魔规则注入白名单(§19.1 api.harmonic)：
 * 第三方可将自定义附魔加入谐律风格群组(数据包 equivalent：见 EnchantmentGroupDatapack)。
 */
public final class EnchantmentResonanceAPI {

    /** 查询位置谐律上下文(风格 + 强度)。 */
    public static com.srqingchen.fieldworks.harmonic.HarmonicEvents.Context context(ServerLevel level, BlockPos pos) {
        return com.srqingchen.fieldworks.harmonic.HarmonicEvents.context(level, pos);
    }

    /** 将附魔注入某风格群组(注册期调用)。 */
    public static void addToGroup(com.srqingchen.fieldworks.harmonic.HarmonicStyle style,
                                  net.minecraft.resources.ResourceKey<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        // 群组为不可变集合(Set.of)；注入通过包装实现——MVP 提供数据包路径(data/fieldworks/harmonic/groups.json 预留)
        throw new UnsupportedOperationException("使用数据包 fieldworks:harmonic_groups 注入(§19.3)");
    }

    private EnchantmentResonanceAPI() {}
}
