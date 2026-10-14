package com.srqingchen.fieldworks.compat.curios;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Curios 软依赖桥(261014 T10)：
 * 全部 Curios 类引用只出现在本类方法体内——{@link #LOADED} 为假时这些方法不会被调用，
 * 未安装 Curios 的实例不触发类加载、不崩溃。
 * 槽位绑定走数据标签(data/curios/tags/item/{ring,necklace,belt,charm}.json)，零代码耦合。
 */
public final class FWCuriosCompat {
    public static final boolean LOADED = net.neoforged.fml.ModList.get().isLoaded("curios");

    private FWCuriosCompat() {}

    /** 玩家是否在任意饰品槽佩戴了该物品。 */
    public static boolean isEquipped(Player player, Item item) {
        return equippedStack(player, item) != null;
    }

    /** 佩戴中的该物品堆(用于改写能量组件等)；未佩戴 null。 */
    @Nullable
    public static ItemStack equippedStack(Player player, Item item) {
        if (!LOADED) return null;
        var handler = top.theillusivec4.curios.api.CuriosApi.getCuriosInventoryOrNull(player);
        if (handler == null) return null;
        var result = handler.findFirstCurio(item);
        return result.map(r -> r.stack()).orElse(null);
    }
}
