package com.srqingchen.fieldworks.api.relocation;

import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.ICancellableEvent;

/**
 * 迁移 API 与事件(§14.3 / §19.1 api.relocation)：
 * 第三方模组可注册黑名单方块、在 Pre 事件中 veto、在 Post 后修复自身方块实体数据。
 */
public final class RelocationAPI {

    /** 注册迁移黑名单方块(API 黑名单，§14.2)。 */
    public static void blacklist(Identifier blockId) {
        com.srqingchen.fieldworks.relocation.RelocationBlacklist.add(blockId);
    }

    /** 迁移前事件：可 veto(第三方保护自身方块实体)。 */
    public static class Pre extends Event implements ICancellableEvent {
        public final net.minecraft.resources.Identifier dimension;
        public final net.minecraft.core.BlockPos min;
        public final net.minecraft.core.BlockPos max;

        public Pre(net.minecraft.resources.Identifier dimension, net.minecraft.core.BlockPos min, net.minecraft.core.BlockPos max) {
            this.dimension = dimension;
            this.min = min;
            this.max = max;
        }
    }

    /** 迁移后事件：第三方修复数据。 */
    public static class Post extends Event {
        public final net.minecraft.resources.Identifier dimension;

        public Post(net.minecraft.resources.Identifier dimension) {
            this.dimension = dimension;
        }
    }

    private RelocationAPI() {}
}
