package com.srqingchen.fieldworks.relocation;

import net.minecraft.resources.Identifier;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** 迁移黑名单注册表(§14.2/§14.3)：默认 + API + 数据包。 */
public final class RelocationBlacklist {
    private static final Set<Identifier> BLACKLIST = ConcurrentHashMap.newKeySet();

    static {
        // 默认黑名单(§14.2)
        BLACKLIST.add(Identifier.withDefaultNamespace("end_portal_frame"));
        BLACKLIST.add(Identifier.withDefaultNamespace("ender_chest"));
        BLACKLIST.add(Identifier.withDefaultNamespace("nether_portal"));
        BLACKLIST.add(Identifier.withDefaultNamespace("end_portal"));
    }

    public static void add(Identifier id) {
        BLACKLIST.add(id);
    }

    public static boolean isBlacklisted(Identifier id) {
        return BLACKLIST.contains(id);
    }

    private RelocationBlacklist() {}
}
