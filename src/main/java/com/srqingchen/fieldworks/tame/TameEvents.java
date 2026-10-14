package com.srqingchen.fieldworks.tame;

import com.srqingchen.fieldworks.block.TameGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 驯息场 × 生物事件层(§12.2)：
 * ≥20 息愤怒消除(实体加入时清 anger) / ≥60 息稀有掉落提升(jackpot ×1.5)。
 */
public final class TameEvents {

    /** ≥60 息：稀有掉落(仅原版稀有条目)×1.5 —— hook LivingDropsEvent 复制稀有掉落。 */
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel sl)) return;
        var pos = event.getEntity().blockPosition();
        float intensity = strongestIntensity(sl, pos);
        if (intensity < 60) return;
        List<ItemEntity> extra = new ArrayList<>();
        for (ItemEntity drop : event.getDrops()) {
            var item = drop.getItem();
            // 原版稀有条目判定(近似：兔子脚/恶魂之泪/不死图腾等)
            if (isRare(item.getItem())) {
                if (sl.getRandom().nextFloat() < 0.5f) {   // ×1.5 等效：50% 概率追加一份
                    ItemEntity copy = new ItemEntity(sl, drop.getX(), drop.getY(), drop.getZ(), item.copy());
                    extra.add(copy);
                }
            }
        }
        event.getDrops().addAll(extra);
    }

    private static boolean isRare(net.minecraft.world.item.Item item) {
        return item == Items.RABBIT_FOOT || item == Items.GHAST_TEAR || item == Items.TOTEM_OF_UNDYING
                || item == Items.SPONGE || item == Items.DRAGON_BREATH || item == Items.HEART_OF_THE_SEA;
    }

    /** 区域最强驯息强度(§12.2 作用半径 = 包络 +8)。 */
    public static float strongestIntensity(ServerLevel level, BlockPos pos) {
        float best = 0;
        BlockPos min = pos.offset(-12, -12, -12);
        BlockPos max = pos.offset(12, 12, 12);
        for (BlockPos p : BlockPos.betweenClosed(min, max)) {
            if (level.getBlockEntity(p) instanceof TameGeneratorBlockEntity tg && tg.intensity > best) {
                best = tg.intensity;
            }
        }
        return best;
    }

    private TameEvents() {}
}
