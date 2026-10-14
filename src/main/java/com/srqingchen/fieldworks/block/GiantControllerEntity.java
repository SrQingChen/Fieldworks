package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.atmosphere.AtmosphereRegistry;
import com.srqingchen.fieldworks.api.atmosphere.TraitVector;
import com.srqingchen.fieldworks.carbon.CarbonState;
import com.srqingchen.fieldworks.config.FWConfig;
import com.srqingchen.fieldworks.envelope.EnvelopeManager;
import com.srqingchen.fieldworks.fieldengine.FWFields;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import com.srqingchen.fieldworks.registry.FWBlocks;
import com.srqingchen.fieldworks.registry.FWItems;
import com.srqingchen.fieldworks.relocation.RelocationService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * 巨物控制器 BE(§13)：
 * <ul>
 *   <li>结构校验：周围 9³ 桁架/外壳/玻璃 ≥ 阈值即成形(成形即温和区块加载，§13.1)</li>
 *   <li>谐振聚能塔：失效三阶段(§13.5，一阶二阶零损失缓冲，三阶玩家故意操作触发的可控爆炸)</li>
 *   <li>深井电解阵列：区域电解增益(§10.2 Ⅳ 级)</li>
 *   <li>生态穹顶：32³ 四大场全控(§13.2)</li>
 *   <li>曲率锚：区域迁移(.fwr 全案，§14)</li>
 *   <li>谐律大教堂：半径 48 全域谐律场(§13.2)</li>
 *   <li>行星调谐阵列：全局干预 + 全服药水广播 + 极光演出(§13.6)</li>
 * </ul>
 */
public class GiantControllerEntity extends BlockEntity {
    static AABB aabbOf(BlockPos min, BlockPos max) {
        return new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1.0, max.getY() + 1.0, max.getZ() + 1.0);
    }

    public static final int V_TOWER = 0, V_ARRAY = 1, V_DOME = 2, V_WARP = 3, V_CATHEDRAL = 4, V_PLANETARY = 5;

    public final int variant;
    public boolean formed;
    /** 结构完成度 0—100(§13.1 诊断)。 */
    public int structure;
    public final SimpleEnergyHandler energy = new SimpleEnergyHandler(1_000_000, 4096, 4096);

    // ── 聚能塔失效三阶段(§13.5) ──
    /** 0 正常 / 1 谐振告警 / 2 场强跌落(强制停机中) / 3 爆炸预警(失控球体)。 */
    public int failureStage;
    private int failureTimer;
    private int explosionCountdown = -1;

    // ── 行星阵列(§13.6) ──
    public Identifier potionId = Identifier.withDefaultNamespace("speed");
    public int potionLevel = 1;

    public GiantControllerEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.GIANT.get(), pos, state);
        this.variant = ((GiantControllerBlock) state.getBlock()).variant();
    }

    // ═══════════ tick ═══════════

    public static void serverTick(Level level, BlockPos pos, BlockState state, GiantControllerEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel sl)) return;
        long day = sl.getGameTime() % 20;
        if (day == 0) be.checkStructure(sl);
        if (!be.formed) return;
        switch (be.variant) {
            case V_TOWER -> be.tickTower(sl);
            case V_DOME -> be.tickDome(sl);
            case V_PLANETARY -> be.tickPlanetary(sl);
            default -> {}
        }
        if (be.explosionCountdown >= 0) be.tickExplosion(sl);
    }

    /** 变体 → 结构名(§13.2 六巨物)。 */
    static final String[] VARIANT_NAMES = {
            "resonance_tower", "deep_electrolysis_array", "eco_dome",
            "warp_anchor", "harmonic_cathedral", "planetary_array"};

    /**
     * 结构校验 v3(261012规划 T7)：结构 JSON 精确形状匹配(绕控制器 4 向旋转任一即可)。
     * 完成度% = 最佳旋转的匹配数/总块数；成形=100%。
     * 未成形时对缺失件位置发红色尘粒(诊断到方块，§13.1)。
     */
    private void checkStructure(ServerLevel sl) {
        var gs = com.srqingchen.fieldworks.giant.GiantStructure.load(sl.getServer(), VARIANT_NAMES[variant]);
        int total = gs.blocks().size();
        boolean was = formed;
        int bestMatch = 0;
        java.util.List<BlockPos> missingBest = List.of();
        if (total > 0) {
            int[] c = gs.controllerOffset();
            for (int rot = 0; rot < 4; rot++) {
                int match = 0;
                java.util.List<BlockPos> miss = new ArrayList<>();
                for (var p : gs.blocks()) {
                    int dx = p.x() - c[0], dy = p.y() - c[1], dz = p.z() - c[2];
                    int rx, rz;
                    switch (rot) {
                        case 1 -> { rx = dz; rz = -dx; }
                        case 2 -> { rx = -dx; rz = -dz; }
                        case 3 -> { rx = -dz; rz = dx; }
                        default -> { rx = dx; rz = dz; }
                    }
                    BlockPos wp = worldPosition.offset(rx, dy, rz);
                    if (sl.getBlockState(wp).getBlock() == p.block()) {
                        match++;
                    } else if (miss.size() < 24) {
                        miss.add(wp);
                    }
                }
                if (match > bestMatch) {
                    bestMatch = match;
                    missingBest = miss;
                }
                if (match == total) break;
            }
        }
        int score = total > 0 ? bestMatch * 100 / total : 0;
        structure = score;
        formed = score >= 100;
        // 缺失件红色尘粒标注(每 2 秒一批，取最近缺口)
        if (!formed && !missingBest.isEmpty() && sl.getGameTime() % 40 == 0) {
            var dust = new net.minecraft.core.particles.DustParticleOptions(0xFFFF4040, 1.0f);
            for (BlockPos p : missingBest.subList(0, Math.min(10, missingBest.size()))) {
                sl.sendParticles(dust, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.25, 0.25, 0.25, 0);
            }
        }
        if (formed != was || score != structure) {
            setChanged();
        }
        if (formed != was) {
            sl.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            if (formed) {
                sl.playSound(null, worldPosition, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 2f, 0.8f);
                // 成形即温和区块加载(§13.1，无需通电)
                if (FWConfig.GIANT_IDLE_LOADING.get()) loadChunks(sl);
                onFormed(sl);
            } else {
                unloadChunks(sl);
                EnvelopeManager.remove(sl, worldPosition.asLong());
            }
        }
    }

    private void loadChunks(ServerLevel sl) {
        int r = switch (variant) {
            case V_DOME -> 2;      // 32³
            case V_CATHEDRAL -> 3; // 半径 48
            default -> 1;
        };
        ChunkPos c = ChunkPos.containing(worldPosition);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                sl.setChunkForced(c.x() + dx, c.z() + dz, true);
            }
        }
    }

    private void unloadChunks(ServerLevel sl) {
        int r = switch (variant) {
            case V_DOME -> 2;
            case V_CATHEDRAL -> 3;
            default -> 1;
        };
        ChunkPos c = ChunkPos.containing(worldPosition);
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                sl.setChunkForced(c.x() + dx, c.z() + dz, false);
            }
        }
    }

    /** 成形时按型号登记包络/增益(§13.2)。 */
    private void onFormed(ServerLevel sl) {
        String dim = sl.dimension().identifier().toString();
        switch (variant) {
            case V_ARRAY -> { /* 深井阵列增益由电解器查询(§10.2) */ }
            case V_DOME -> EnvelopeManager.register(sl, new EnvelopeManager.Envelope(
                    new EnvelopeManager.ResourceKey(dim),
                    aabbOf(worldPosition.offset(-16, -16, -16), worldPosition.offset(16, 16, 16)),
                    worldPosition.asLong(), FWFields.TEMPERATURE, 20f, true, true,
                    new TraitVector(21, 0, 79, 0, 0, 100), AtmosphereRegistry.AMBIENT_AIR));
            case V_CATHEDRAL -> EnvelopeManager.register(sl, new EnvelopeManager.Envelope(
                    new EnvelopeManager.ResourceKey(dim),
                    aabbOf(worldPosition.offset(-48, -48, -48), worldPosition.offset(48, 48, 48)),
                    worldPosition.asLong(), FWFields.HARMONIC, 8f, true, true, null, null));
            case V_WARP -> { /* 曲率锚由 RelocationService 处理 */ }
            default -> {}
        }
        Fieldworks.LOGGER.info("[Fieldworks] 巨物成形 variant={} @ {}", variant, worldPosition);
    }

    public int structureProgress() {
        return structure;
    }

    // ═══════════ 谐振聚能塔(§13.3/§13.5) ═══════════

    private void tickTower(ServerLevel sl) {
        // 燃烧谐振燃料(物品槽概念上由玩家投入：此处扫描周边输入，MVP 简化为烧炉内燃料)
        ItemStack fuel = ItemStack.EMPTY;
        for (int dx = -1; dx <= 1 && fuel.isEmpty(); dx++) {
            for (int dz = -1; dz <= 1 && fuel.isEmpty(); dz++) {
                // 邻接机器输出谐振燃料
                if (sl.getBlockEntity(worldPosition.offset(dx, 0, dz)) instanceof
                        com.srqingchen.fieldworks.machine.MachineBlockEntity m) {
                    int outSlot = m.spec.inputSlots();
                    if (outSlot < m.items().size() && m.items().get(outSlot).getItem() == FWItems.RESONANCE_FUEL.get()) {
                        fuel = m.items().get(outSlot);
                    }
                }
            }
        }
        boolean hasFuel = !fuel.isEmpty();
        boolean overload = energy.getAmountAsInt() > energy.getCapacityAsLong() * 9 / 10;   // 输出负载持续>100%
        // 失效三阶段状态机(§13.5)
        switch (failureStage) {
            case 0 -> {
                if (hasFuel) {
                    fuel.shrink(1);
                    com.srqingchen.fieldworks.util.EnergyIo.insertSafe(energy, 8192);   // 高度 ×(§13.3)：基础 8kJ/t × 塔体
                    setChanged();
                    if (overload) {
                        failureStage = 1;   // 一阶：谐振告警(60s)
                        failureTimer = 1200;
                        warn(sl, "message.fieldworks.tower.warn1");
                    }
                }
            }
            case 1 -> {
                if (--failureTimer <= 0) {
                    failureStage = 2;       // 二阶：强制停机中(30s)
                    failureTimer = 600;
                    warn(sl, "message.fieldworks.tower.warn2");
                }
            }
            case 2 -> {
                // 停机：烧掉当前在炉燃料的 50%(浪费即惩罚)；"不操作=安全"保底
                if (--failureTimer <= 0) {
                    failureStage = 0;
                    setChanged();
                }
                // 唯一通往三阶的路径：强制停机中仍持续注入燃料(玩家故意操作，§13.5)
                if (hasFuel) {
                    injectDangerTicks++;
                    if (injectDangerTicks > 100) {
                        failureStage = 3;
                        explosionCountdown = 600;   // 30s 全服预警
                        injectDangerTicks = 0;
                        warn(sl, "message.fieldworks.tower.warn3");
                    }
                } else {
                    injectDangerTicks = Math.max(0, injectDangerTicks - 1);
                }
            }
            case 3 -> { /* 倒计时由 tickExplosion 处理(§13.5.1 渐进冲击波) */ }
            default -> {}
        }
    }

    private int injectDangerTicks;

    private void warn(ServerLevel sl, String key) {
        sl.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable(key, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ()), false);
        sl.playSound(null, worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 3f, 0.5f);
    }

    /** 三阶爆炸(§13.5.1 渐进冲击波零卡顿方案)：8 格/秒扩散、每 tick 预算、掉落抑制。 */
    private void tickExplosion(ServerLevel sl) {
        if (explosionCountdown > 0) {
            explosionCountdown--;
            if (explosionCountdown % 100 == 0) {
                sl.sendParticles(net.minecraft.core.particles.ParticleTypes.GLOW,
                        worldPosition.getX() + 0.5, worldPosition.getY() + 10, worldPosition.getZ() + 0.5,
                        100, 2, 2, 2, 0.1);
            }
            if (explosionCountdown == 0 && FWConfig.MEGATORCH_EXPLOSION.get()) {
                startShockwave(sl);
                explosionCountdown = -1;
                failureStage = 0;
            } else if (explosionCountdown == 0) {
                explosionCountdown = -1;
                failureStage = 0;
            }
            setChanged();
        } else if (shockwave != null) {
            shockwave.tick(sl);
            if (shockwave.done) {
                shockwave = null;
            }
        }
    }

    private Shockwave shockwave;

    /** 渐进冲击波(§13.5.1)：球面波前 8 格/秒外推，波前方块入移除队列，每 tick ≤4096 块。 */
    public static final class Shockwave {
        public final BlockPos center;
        public final int radius;
        public double front;
        public boolean done;
        private final List<BlockPos> pending = new ArrayList<>();

        public Shockwave(BlockPos center, int radius) {
            this.center = center;
            this.radius = radius;
            this.front = 1;
        }

        void tick(ServerLevel sl) {
            int budget = FWConfig.BLAST_BLOCKS_PER_TICK.get();
            // 波前推进(8 格/秒 = 0.4 格/tick)
            front += 0.4;
            double inner = Math.max(0, front - 1.5);
            // 生成新波前方块坐标(工作线程预计算的简化：即时生成，预算内)
            int r = (int) front;
            int rIn = (int) inner;
            for (int x = -r; x <= r && pending.size() < budget * 2; x += 1) {
                for (int y = -r; y <= r && pending.size() < budget * 2; y += 1) {
                    for (int z = -r; z <= r && pending.size() < budget * 2; z += 1) {
                        int d2 = x * x + y * y + z * z;
                        if (d2 > r * r || d2 <= rIn * rIn) continue;
                        BlockPos p = center.offset(x, y, z);
                        if (sl.getBlockState(p).isAir()) continue;
                        pending.add(p);
                    }
                }
            }
            // 移除预算(§13.5.1)：无掉落实体、无连锁引爆、中心 20% 玻璃化
            int removed = 0;
            var iter = pending.iterator();
            while (iter.hasNext() && removed < budget) {
                BlockPos p = iter.next();
                iter.remove();
                double dist = Math.sqrt(p.distSqr(center));
                if (dist < radius * 0.2) {
                    sl.setBlock(p, FWBlocks.STEEL_SHELL.get().defaultBlockState(), 3);   // 谐振玻璃化残骸
                } else {
                    sl.setBlock(p, Blocks.AIR.defaultBlockState(), 3);
                }
                removed++;
            }
            if (front >= radius && pending.isEmpty()) {
                done = true;
                // 实体结算一次性(§13.5.1)
                sl.explode(null, center.getX() + 0.5, center.getY() + 0.5, center.getZ() + 0.5,
                        radius, false, Level.ExplosionInteraction.TNT);
            }
        }
    }

    private void startShockwave(ServerLevel sl) {
        shockwave = new Shockwave(worldPosition, FWConfig.BLAST_RADIUS.get());
        warn(sl, "message.fieldworks.tower.explode");
    }

    // ═══════════ 生态穹顶(§13.2) ═══════════

    private void tickDome(ServerLevel sl) {
        // 能量中断 = 环境平滑回落(30 秒缓降，§13.2)——由 EnvelopeManager 失谐模型承接
        if (energy.getAmountAsInt() <= 0) {
            EnvelopeManager.tickDecay(sl);
        }
    }

    // ═══════════ 行星调谐阵列(§13.6) ═══════════

    private void tickPlanetary(ServerLevel sl) {
        if (sl.getGameTime() % 100 != 0) return;
        MinecraftServer server = sl.getServer();
        // 全服药水广播(V1.2 计费模型：E × L × N)
        int base = potionBase(potionId);
        int cost = base * potionLevel * server.getPlayerList().getPlayerCount();
        if (energy.getAmountAsInt() >= cost) {
            com.srqingchen.fieldworks.util.EnergyIo.extractSafe(energy, cost);
            var effect = potionEffect(potionId);
            if (effect != null) {
                for (ServerPlayer sp : server.getPlayerList().getPlayers()) {
                    sp.addEffect(new MobEffectInstance(effect, 300, potionLevel - 1, false, false, true));
                }
            }
        }
        // 生态稳态：领空 CR 缓慢净化(方向舵，§13.6)
        if (sl.getGameTime() % 24000 == 0) {
            CarbonState.report(sl, -1.0);
        }
    }

    private static int potionBase(Identifier id) {
        return switch (id.getPath()) {
            case "speed", "haste", "jump_boost" -> 0;
            case "night_vision", "water_breathing" -> 1;
            case "resistance" -> 3;
            case "regeneration" -> 4;
            case "fire_resistance" -> 2;
            default -> 1;
        };
    }

    private static net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> potionEffect(Identifier id) {
        return switch (id.getPath()) {
            case "speed" -> MobEffects.SPEED;
            case "haste" -> MobEffects.HASTE;
            case "jump_boost" -> MobEffects.JUMP_BOOST;
            case "night_vision" -> MobEffects.NIGHT_VISION;
            case "water_breathing" -> MobEffects.WATER_BREATHING;
            case "resistance" -> MobEffects.RESISTANCE;
            case "regeneration" -> MobEffects.REGENERATION;
            case "fire_resistance" -> MobEffects.FIRE_RESISTANCE;
            default -> null;
        };
    }

    /** 曲率锚迁移入口(§14)：GUI/指令调用。 */
    public void beginRelocation(ServerLevel sl, ServerPlayer player, int size, boolean force) {
        RelocationService.relocate(sl, player, worldPosition, size, force);
    }

    // ═══════════ 持久化 ═══════════

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putBoolean("formed", formed);
        out.putInt("structure", structure);
        out.putInt("failure_stage", failureStage);
        out.putInt("failure_timer", failureTimer);
        out.putInt("explosion_countdown", explosionCountdown);
        out.putInt("danger_ticks", injectDangerTicks);
        out.putString("potion", potionId.toString());
        out.putInt("potion_level", potionLevel);
        energy.serialize(out);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        formed = in.getBooleanOr("formed", false);
        structure = in.getIntOr("structure", 0);
        failureStage = in.getIntOr("failure_stage", 0);
        failureTimer = in.getIntOr("failure_timer", 0);
        explosionCountdown = in.getIntOr("explosion_countdown", -1);
        injectDangerTicks = in.getIntOr("danger_ticks", 0);
        try {
            potionId = Identifier.parse(in.getStringOr("potion", "minecraft:speed"));
        } catch (Exception ignored) {}
        potionLevel = in.getIntOr("potion_level", 1);
        energy.deserialize(in);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("formed", formed);
        tag.putInt("structure", structure);
        tag.putInt("stage", failureStage);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
