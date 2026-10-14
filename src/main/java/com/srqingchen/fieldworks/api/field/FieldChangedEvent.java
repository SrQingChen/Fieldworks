package com.srqingchen.fieldworks.api.field;

import com.srqingchen.fieldworks.api.atmosphere.AtmosphereType;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.Event;

/**
 * 场快照变更事件(§3.3 / §6.6 订阅推送，无轮询)。
 * 场引擎在聚合完成 / 包络变更 / 全局结算后，对有订阅者的区块派发本事件。
 * 机器不轮询场，只订阅自身位置的快照变更。
 */
public class FieldChangedEvent extends Event {
    private final Level level;
    private final BlockPos pos;
    private final FieldSnapshotView snapshot;

    public FieldChangedEvent(Level level, BlockPos pos, FieldSnapshotView snapshot) {
        this.level = level;
        this.pos = pos;
        this.snapshot = snapshot;
    }

    public Level getLevel() { return level; }
    public BlockPos getPos() { return pos; }
    public FieldSnapshotView getSnapshot() { return snapshot; }

    /** 只读场快照视图：按场 ID 取当前值。 */
    public interface FieldSnapshotView {
        float value(Identifier fieldId);

        /** 气氛场采样(类型 + 特征值六元组)，非气氛场位置返回 null。 */
        AtmosphereType atmosphereType();

        com.srqingchen.fieldworks.api.atmosphere.TraitVector traits();
    }
}
