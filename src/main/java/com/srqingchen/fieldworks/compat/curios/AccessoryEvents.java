package com.srqingchen.fieldworks.compat.curios;

import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import com.srqingchen.fieldworks.fieldengine.FWFields;
import com.srqingchen.fieldworks.item.SuperconductorCoreItem;
import com.srqingchen.fieldworks.registry.FWItems;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 场学饰品行为(261014 T10)：佩戴效果 + 超导谐律核心的转电盾/谐律增幅。
 * 未装 Curios 时 isEquipped 恒 false——全部行为自然失效，事件零开销空转。
 */
public final class AccessoryEvents {

    /** 单层谐律增幅：属性 + 数量 + 运算。 */
    private record BuffType(Holder<Attribute> attr, double amount, AttributeModifier.Operation op, String label) {}

    private static final BuffType[] BUFF_POOL = {
            new BuffType(Attributes.ATTACK_DAMAGE, 1.0, AttributeModifier.Operation.ADD_VALUE, "attack"),
            new BuffType(Attributes.ATTACK_SPEED, 0.04, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "attack_speed"),
            new BuffType(Attributes.MOVEMENT_SPEED, 0.02, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, "speed"),
            new BuffType(Attributes.ARMOR, 1.0, AttributeModifier.Operation.ADD_VALUE, "armor"),
            new BuffType(Attributes.KNOCKBACK_RESISTANCE, 0.04, AttributeModifier.Operation.ADD_VALUE, "knockback"),
    };

    /** 进行中的护盾状态(同一次受击管线共享)。 */
    private static final Map<UUID, List<MobEffectInstance>> EFFECT_SNAPSHOT = new HashMap<>();
    private static final Map<UUID, Boolean> FULLY_ABSORBED = new HashMap<>();
    /** 谐律增幅累计耗电(FE)。 */
    private static final Map<UUID, Long> SPENT_ACC = new HashMap<>();
    /** 活跃增幅层。 */
    private record BuffStack(Identifier id, BuffType type, long expireAt) {}
    private static final Map<UUID, List<BuffStack>> BUFFS = new HashMap<>();
    /** 虚空步径戒冷却(gameTime)。 */
    private static final Map<UUID, Long> BLINK_CD = new HashMap<>();

    private AccessoryEvents() {}

    // ═══════════ 转电盾管线 ═══════════

    /** 管线起点：快照当前状态效果(供完全抵挡时鉴别"本次受击新增"的效果)。 */
    public static void onIncomingDamage(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;
        var core = equippedCore(player);
        if (core == null) return;
        EFFECT_SNAPSHOT.put(player.getUUID(), List.copyOf(player.getActiveEffects()));
        FULLY_ABSORBED.remove(player.getUUID());
    }

    /** 伤害结算前：按电容余量把伤害转为耗电。 */
    public static void onDamagePre(LivingDamageEvent.Pre e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;
        var core = equippedCore(player);
        if (core == null) return;
        float damage = e.getContainer().getNewDamage();
        if (damage <= 0) return;
        long energy = SuperconductorCoreItem.energyOf(core);
        if (energy <= 0) return;

        // 电量换算可抵挡 HP；不足则按比例减伤
        double absorbableHp = energy / SuperconductorCoreItem.COST_PER_HP;
        float absorbed = (float) Math.min(damage, absorbableHp);
        long cost = Math.round(absorbed * SuperconductorCoreItem.COST_PER_HP);
        SuperconductorCoreItem.setEnergy(core, SuperconductorCoreItem.energyOf(core) - cost);
        e.getContainer().setNewDamage(Math.max(0, damage - absorbed));

        boolean full = damage - absorbed <= 0.001f;
        FULLY_ABSORBED.put(player.getUUID(), full);

        // 谐律增幅：按实际耗电累计，每 2000FE 一层(独立持续时间、可无限叠加)
        long acc = SPENT_ACC.getOrDefault(player.getUUID(), 0L) + cost;
        while (acc >= SuperconductorCoreItem.FE_PER_STACK) {
            acc -= SuperconductorCoreItem.FE_PER_STACK;
            grantBuff(player);
        }
        SPENT_ACC.put(player.getUUID(), acc);

        if (absorbed > 0) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE,
                    SoundSource.PLAYERS, 0.7f, 1.6f);
        }
    }

    /** 伤害结算后：完全抵挡 → 清除本次受击新增的全部状态效果(中毒/凋零/失明…)。 */
    public static void onDamagePost(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer player)) return;
        UUID id = player.getUUID();
        var snapshot = EFFECT_SNAPSHOT.remove(id);
        if (snapshot == null || !FULLY_ABSORBED.remove(id, Boolean.TRUE)) return;

        // 快照里没有、现在出现的 = 本次受击带来的效果 → 全部清除
        var current = new ArrayList<>(player.getActiveEffects());
        for (MobEffectInstance now : current) {
            boolean isNew = snapshot.stream().noneMatch(old ->
                    old.getEffect() == now.getEffect());
            if (isNew) {
                player.removeEffect(now.getEffect());
            }
        }
        if (!current.isEmpty()) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_BREAK,
                    SoundSource.PLAYERS, 0.6f, 0.7f);
        }
    }

    private static void grantBuff(ServerPlayer player) {
        BuffType type = BUFF_POOL[player.getRandom().nextInt(BUFF_POOL.length)];
        Identifier modId = Identifier.fromNamespaceAndPath("fieldworks",
                "core_buff_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
        long expireAt = player.level().getGameTime() + SuperconductorCoreItem.STACK_DURATION;
        var attr = player.getAttribute(type.attr());
        if (attr == null) return;
        // 同名增幅允许叠加：每层独立 modifier id + 独立到期时间
        attr.addTransientModifier(new AttributeModifier(modId, type.amount(), type.op()));
        BUFFS.computeIfAbsent(player.getUUID(), k -> new ArrayList<>()).add(new BuffStack(modId, type, expireAt));
        player.sendSystemMessage(net.minecraft.network.chat.Component.translatable(
                "message.fieldworks.core.buff." + type.label()));
    }

    // ═══════════ 每刻效果 ═══════════

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        Player player = e.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer sp)) return;
        long now = player.level().getGameTime();

        // ── 增幅层到期回收 ──
        var buffs = BUFFS.get(sp.getUUID());
        if (buffs != null && !buffs.isEmpty() && now % 10 == 0) {
            buffs.removeIf(b -> {
                if (now >= b.expireAt()) {
                    var attr = sp.getAttribute(b.type().attr());
                    if (attr != null) attr.removeModifier(b.id());
                    return true;
                }
                return false;
            });
        }

        if (!FWCuriosCompat.LOADED || now % 10 != 0) return;   // 佩戴效果 0.5s 节拍

        // ── 谐振指环：急迫 I 常驻 ──
        if (FWCuriosCompat.isEquipped(sp, FWItems.RESONANT_BAND.get())) {
            if (!sp.hasEffect(net.minecraft.world.effect.MobEffects.HASTE)) {
                sp.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.HASTE,
                        160, 0, true, false, false));
            }
        }

        // ── 磁准透镜：4m 磁吸掉落物 ──
        if (FWCuriosCompat.isEquipped(sp, FWItems.MAGNETIC_LENS.get())) {
            attractItems(sp);
        }

        // ── 潮汐坠饰：水下供气 ──
        if (FWCuriosCompat.isEquipped(sp, FWItems.TIDAL_CHARM.get()) && sp.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
            sp.setAirSupply(sp.getMaxAirSupply());
        }

        // ── 虚空步径戒：潜行移动时 3s CD 闪现 8 格 ──
        Vec3 vel = sp.getDeltaMovement();
        if (FWCuriosCompat.isEquipped(sp, FWItems.VOID_STEP_RING.get())
                && sp.isShiftKeyDown() && vel.x * vel.x + vel.z * vel.z > 0.003) {
            tryBlink(sp, now);
        }

        // ── 超导核心：强场被动涓流充能(磁场 ≥0.5T 即人工强场区) ──
        if (now % 20 == 0) {
            var core = equippedCore(sp);
            if (core != null && SuperconductorCoreItem.energyOf(core) < SuperconductorCoreItem.CAPACITY) {
                if (sp.level() instanceof ServerLevel sl) {
                    try {
                        float mag = FieldEngine.snapshot(sl, sp.blockPosition()).value(FWFields.MAGNETIC);
                        if (mag >= 0.5f) {
                            SuperconductorCoreItem.setEnergy(core,
                                    SuperconductorCoreItem.energyOf(core) + 64);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    /** 场线护符：摔落伤害 -25%。 */
    public static void onLivingFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!FWCuriosCompat.LOADED) return;
        if (FWCuriosCompat.isEquipped(sp, FWItems.FIELDLINE_AMULET.get()) && e.getDamageMultiplier() > 0) {
            e.setDamageMultiplier(e.getDamageMultiplier() * 0.75f);
        }
    }

    // ── 工具 ──

    @Nullable
    private static ItemStack equippedCore(ServerPlayer player) {
        return FWCuriosCompat.equippedStack(player, FWItems.SUPERCONDUCTOR_CORE.get());
    }

    private static void attractItems(ServerPlayer sp) {
        var box = sp.getBoundingBox().inflate(4.0);
        for (ItemEntity item : sp.level().getEntitiesOfClass(ItemEntity.class, box)) {
            if (item.hasPickUpDelay() || item.getOwner() != null && item.getOwner() != sp
                    && item.getAge() < 40) {
                continue;
            }
            Vec3 pull = sp.position().add(0, 0.6, 0).subtract(item.position());
            if (pull.lengthSqr() < 0.4) continue;
            item.setDeltaMovement(item.getDeltaMovement().scale(0.4).add(pull.normalize().scale(0.12)));
        }
    }

    private static void tryBlink(ServerPlayer sp, long now) {
        long cd = BLINK_CD.getOrDefault(sp.getUUID(), 0L);
        if (now < cd) return;
        Vec3 look = sp.getLookAngle();
        Vec3 from = sp.getEyePosition();
        var hit = sp.level().clip(new net.minecraft.world.level.ClipContext(
                from, from.add(look.scale(8.0)),
                net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, sp));
        Vec3 dest = hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS
                ? from.add(look.scale(7.5))
                : hit.getLocation().subtract(look.scale(0.8));
        // 落点需有玩家身位空间：不行就逐级回退
        Vec3 target = null;
        for (double step = 0; step <= 3; step++) {
            Vec3 cand = dest.subtract(look.scale(step));
            var bb = sp.getBoundingBox().move(cand.subtract(sp.position()));
            if (sp.level().noCollision(sp, bb)) {
                target = cand;
                break;
            }
        }
        if (target == null || from.distanceTo(target) < 1.5) return;
        sp.level().sendParticles(ParticleTypes.PORTAL, sp.getX(), sp.getY() + 1, sp.getZ(), 24, 0.4, 0.8, 0.4, 0.05);
        sp.teleportTo(target.x, target.y, target.z);
        sp.level().sendParticles(ParticleTypes.REVERSE_PORTAL, target.x, target.y + 1, target.z, 24, 0.4, 0.8, 0.4, 0.05);
        sp.level().playSound(null, sp.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7f, 1.2f);
        BLINK_CD.put(sp.getUUID(), now + 60);   // 3s
    }

    /** 饰品的静态属性修饰(挂物品默认组件；Curios 佩戴时生效 + tooltip 可见)。 */
    public static ItemAttributeModifiers attributesOf(String itemId) {
        var b = ItemAttributeModifiers.builder();
        switch (itemId) {
            case "resonant_band" -> b.add(Attributes.ATTACK_SPEED,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("fieldworks", "resonant_band"),
                            0.03, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.ANY);
            case "fieldline_amulet" -> b.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("fieldworks", "fieldline_amulet"),
                            0.03, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.ANY);
            case "flux_belt" -> b.add(Attributes.LUCK,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("fieldworks", "flux_belt"),
                            1.0, AttributeModifier.Operation.ADD_VALUE), EquipmentSlotGroup.ANY);
            case "tidal_charm" -> b.add(Attributes.MOVEMENT_SPEED,
                    new AttributeModifier(Identifier.fromNamespaceAndPath("fieldworks", "tidal_charm"),
                            0.02, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL), EquipmentSlotGroup.ANY);
            default -> {}
        }
        return b.build();
    }
}
