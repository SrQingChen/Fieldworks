package com.srqingchen.fieldworks.fieldengine;

import com.srqingchen.fieldworks.api.atmosphere.AtmosphereType;
import com.srqingchen.fieldworks.api.atmosphere.TraitVector;
import com.srqingchen.fieldworks.api.field.FieldRegistry;
import net.minecraft.resources.Identifier;

/**
 * 区块级场快照(§6.5)：同一区块内 N 台机器共享一份快照。
 * 不可变对象，失效即整体替换。
 */
public final class FieldSnapshot {
    private final float[] values;
    private final Identifier atmosphereType;
    private final TraitVector traits;
    private final long gameTime;

    public FieldSnapshot(float[] values, Identifier atmosphereType, TraitVector traits, long gameTime) {
        this.values = values;
        this.atmosphereType = atmosphereType;
        this.traits = traits;
        this.gameTime = gameTime;
    }

    public float value(Identifier fieldId) {
        int ord = FieldRegistry.ordinal(fieldId);
        return ord < 0 || ord >= values.length ? 0 : values[ord];
    }

    public float value(int ordinal) {
        return ordinal < 0 || ordinal >= values.length ? 0 : values[ordinal];
    }

    public int size() {
        return values.length;
    }

    public Identifier atmosphereType() {
        return atmosphereType;
    }

    public AtmosphereType atmosphere() {
        return com.srqingchen.fieldworks.api.atmosphere.AtmosphereRegistry.get(atmosphereType);
    }

    public TraitVector traits() {
        return traits;
    }

    public long gameTime() {
        return gameTime;
    }
}
