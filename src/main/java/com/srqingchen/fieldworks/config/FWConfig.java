package com.srqingchen.fieldworks.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * 配置项清单(策划稿附录 B)。全部带默认值，管理员可调。
 */
public final class FWConfig {
    private static final ModConfigSpec.Builder B = new ModConfigSpec.Builder();

    // ── 场引擎 ──
    public static final ModConfigSpec.DoubleValue ENGINE_BUDGET_MS = B
            .comment("场引擎每 tick 主线程预算(毫秒)，预算用尽即顺延聚合(§6.1)。")
            .defineInRange("fieldEngine.budgetMs", 0.3, 0.05, 5.0);
    public static final ModConfigSpec.IntValue AGGREGATION_PERIOD_TICKS = B
            .comment("活跃群系实例聚合基准周期(tick，§6.3)。")
            .defineInRange("fieldEngine.aggregationPeriodTicks", 600, 100, 12000);
    public static final ModConfigSpec.BooleanValue ATMOSPHERE_HINTS = B
            .comment("首次进入新大气类型时的聊天栏风味提示(§8.3)。")
            .define("tutorial.atmosphereHints", true);
    public static final ModConfigSpec.BooleanValue FIRST_HINTS = B
            .comment("新手首次机制提示。")
            .define("tutorial.firstHints", true);
    public static final ModConfigSpec.BooleanValue GIVE_BOOK_ON_START = B
            .comment("整合包模式:开局直发《场域学入门》(需 Patchouli)。")
            .define("tutorial.giveBookOnStart", false);

    // ── 碳氛围场 ──
    public static final ModConfigSpec.BooleanValue CARBON_ENABLED = B
            .comment("全局碳氛围场开关(§5)。")
            .define("carbon.enabled", true);
    public static final ModConfigSpec.BooleanValue CARBON_HEURISTIC = B
            .comment("未接入 API 的模组/原版行为走启发式近似埋点。")
            .define("carbon.heuristicReporting", true);
    public static final ModConfigSpec.DoubleValue CARBON_TEMP_CAP = B
            .comment("全局温度倍率封顶(§5.2)。")
            .defineInRange("carbon.tempEffectCap", 0.15, 0.0, 1.0);

    // ── 人工场包络 ──
    public static final ModConfigSpec.DoubleValue ENVELOPE_LEAK_PER_DAY = B
            .comment("无约束器时包络边界每游戏日外溢的场强比例(§11.4)。")
            .defineInRange("envelope.leakPerDay", 0.005, 0.0, 0.5);
    public static final ModConfigSpec.BooleanValue ENVELOPE_DECAY_ON_BLACKOUT = B
            .comment("能量中断时场强按秒跌落(而非瞬间消失)。")
            .define("envelope.decayOnBlackout", true);
    public static final ModConfigSpec.BooleanValue FRAME_CHUNK_LOADING = B
            .comment("供能框架的温和区块加载(§11.1.1)。")
            .define("envelope.frameChunkLoading", true);
    public static final ModConfigSpec.BooleanValue GIANT_IDLE_LOADING = B
            .comment("巨物成形即自带区块加载(§13.1)。")
            .define("envelope.giantIdleLoading", true);

    // ── 谐律场 ──
    public static final ModConfigSpec.IntValue HARMONIC_LEVEL_CAP = B
            .comment("附魔等级扩展封顶(§12.1，默认 VI)。")
            .defineInRange("harmonic.levelCap", 6, 1, 10);
    public static final ModConfigSpec.BooleanValue HARMONIC_YIELD_TO_APO = B
            .comment("检测到 Apotheosis 时自动让位并关闭 >V 部分(§12.1)。")
            .define("harmonic.yieldToApotheosis", true);

    // ── 区域迁移 ──
    public static final ModConfigSpec.IntValue RELOCATION_MAX_SIZE = B
            .comment("曲率锚最大迁移边长(§14.2)。")
            .defineInRange("relocation.maxSize", 48, 8, 64);
    public static final ModConfigSpec.IntValue RELOCATION_KEEP_DAYS = B
            .comment("迁移 .fwr 文件保留游戏日数。")
            .defineInRange("relocation.keepDays", 7, 0, 365);
    public static final ModConfigSpec.BooleanValue RELOCATION_REQUIRE_CONFIRM = B
            .define("relocation.requireConfirmation", true);

    // ── 聚能塔爆炸 ──
    public static final ModConfigSpec.BooleanValue MEGATORCH_EXPLOSION = B
            .comment("聚能塔三阶可控爆炸总开关(§13.5)。")
            .define("balance.megatorchExplosion", true);
    public static final ModConfigSpec.IntValue BLAST_RADIUS = B
            .comment("爆炸半径(格，§13.5.1)。")
            .defineInRange("balance.blastRadius", 64, 8, 128);
    public static final ModConfigSpec.IntValue BLAST_BLOCKS_PER_TICK = B
            .comment("冲击波每 tick 移除方块预算(§13.5.1)。")
            .defineInRange("balance.blastBlocksPerTick", 4096, 64, 65536);

    // ── 其他自然机制(§4.6，全部可关) ──
    public static final ModConfigSpec.BooleanValue SEASONAL_DRIFT = B.define("nature.seasonalDrift", true);
    public static final ModConfigSpec.BooleanValue RAIN_HUMIDITY = B.define("nature.rainHumidity", true);
    public static final ModConfigSpec.BooleanValue FIRE_OXYGEN = B.define("nature.fireOxygen", true);
    public static final ModConfigSpec.BooleanValue THUNDER_EM = B.define("nature.thunderEm", true);
    public static final ModConfigSpec.BooleanValue PORTAL_LEAK = B.define("nature.portalLeak", true);

    // ── 熔炉持续燃烧(§12.5) ──
    public static final ModConfigSpec.BooleanValue FURNACE_CONTINUOUS = B
            .define("furnace.continuousFiring", true);

    // ── 渲染预算(§21.3) ──
    public static final ModConfigSpec.IntValue PARTICLE_CAP = B
            .comment("每玩家环境粒子上限。")
            .defineInRange("performance.particleCap", 200, 0, 2000);
    public static final ModConfigSpec.IntValue BER_DISTANCE_TIER = B
            .comment("BER 动画距离分级(格，0=自动)。")
            .defineInRange("performance.berDistanceTier", 0, 0, 256);

    public static final ModConfigSpec SPEC = B.build();

    private FWConfig() {}
}
