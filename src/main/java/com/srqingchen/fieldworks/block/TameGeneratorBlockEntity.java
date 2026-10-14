package com.srqingchen.fieldworks.block;

import com.srqingchen.fieldworks.registry.FWBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 驯息发生器 BE(§12.2)：
 * ≥20 息 愤怒消除 / ≥40 息 加速成长繁殖 / ≥60 息 稀有掉落提升(事件层) / ≥80 息 驯化共鸣(生命恢复)。
 * 实体列表按 AABB 查询 + 100 tick 周期(§12.2 零每 tick 扫描)。
 */
public class TameGeneratorBlockEntity extends BlockEntity {
    /** 驯息强度(0—100 息，介质供给)。 */
    public float intensity = 40f;
    private static final double RADIUS = 8;

    public TameGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(FWBlockEntities.TAME_GENERATOR.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, TameGeneratorBlockEntity be) {
        if (level.isClientSide() || !(level instanceof ServerLevel sl)) return;
        if (sl.getGameTime() % 100 != (pos.hashCode() & 63)) return;
        if (be.intensity <= 0) return;
        AABB box = new AABB(pos).inflate(RADIUS);
        // ≥40 息：成长加速(幼体 ×2 等效)——age 减半推进
        if (be.intensity >= 40) {
            for (Animal a : sl.getEntitiesOfClass(Animal.class, box, e -> e.isAlive() && e.isBaby())) {
                if (a instanceof AgeableMob mob) {
                    mob.setAge(Math.min(0, mob.getAge() + 100));   // 负 age → 加倍靠近成年
                }
            }
        }
        // ≥80 息：驯化共鸣——驯服生物获得生命恢复(宠物医院，§12.2)
        if (be.intensity >= 80) {
            for (TamableAnimal t : sl.getEntitiesOfClass(TamableAnimal.class, box,
                    e -> e.isAlive() && e.isTame())) {
                t.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 0), t);
            }
        }
    }

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        out.putFloat("intensity", intensity);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        intensity = in.getFloatOr("intensity", 40f);
    }
}
