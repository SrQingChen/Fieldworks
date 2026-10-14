package com.srqingchen.fieldworks.fieldengine;

import com.srqingchen.fieldworks.api.atmosphere.AtmosphereRegistry;
import com.srqingchen.fieldworks.api.atmosphere.TraitVector;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * 群系实例(§0/§6.3)：同一群系 ID 在世界中一片连续的独立区域。
 * 实例携带动态特征值(OX/FG/CR)、植被聚合、邻接图(§4.6 扩散用)。
 */
public final class AtmosphereInstance {
    public final long id;
    public final ResourceKey<Level> dimension;
    public final ResourceKey<Biome> biome;
    public final Identifier atmosphereType;
    /** 实例哈希扰动 ±10%(§4.2：同群系不同实例特征值有差异)。 */
    public final float jitterScale;
    /** 动态特征值：以"基线×jitter"为出发点漂移(§4.3 氧恢复 / §7.3 消耗排放)。 */
    public float dynOx, dynFg, dynCr;
    /** 植被聚合(§6.4)：已扫描区块 VI 累计 + 区块数；未加载用 VI_est。 */
    public float viSum;
    public int scannedChunks;
    public final LongOpenHashSet loadedChunks = new LongOpenHashSet();
    /** 邻接实例 id 集合(邻接图在区块登记时建立，§6.3)。 */
    public final LongOpenHashSet neighbors = new LongOpenHashSet();

    public AtmosphereInstance(long id, ResourceKey<Level> dimension, ResourceKey<Biome> biome,
                              Identifier atmosphereType, float jitterScale,
                              float dynOx, float dynFg, float dynCr, float viSum, int scannedChunks) {
        this.id = id;
        this.dimension = dimension;
        this.biome = biome;
        this.atmosphereType = atmosphereType;
        this.jitterScale = jitterScale;
        this.dynOx = dynOx;
        this.dynFg = dynFg;
        this.dynCr = dynCr;
        this.viSum = viSum;
        this.scannedChunks = scannedChunks;
    }

    /** 该实例当前的"基线 + 扰动"特征值(不含动态漂移)。 */
    public TraitVector baseTraits() {
        var t = AtmosphereRegistry.get(atmosphereType);
        TraitVector base = t == null ? TraitVector.AMBIENT : t.baseline();
        return new TraitVector(
                clamp01h(base.ox() * jitterScale, 0, 40),
                clamp01h(base.fg() * jitterScale, 0, 25),
                clamp01h(base.in() * jitterScale, 0, 100),
                clamp01h(base.cr() * jitterScale, 0, 60),
                base.vd(),
                base.pr() * (2 - jitterScale));
    }

    /** 有效特征值 = 基线×扰动 + 动态漂移。 */
    public TraitVector effectiveTraits() {
        TraitVector b = baseTraits();
        return new TraitVector(
                Math.max(0, b.ox() + dynOx),
                Math.max(0, b.fg() + dynFg),
                Math.max(0, Math.min(100, b.in() - dynOx * 0.5f)),
                Math.max(0, b.cr() + dynCr),
                b.vd(),
                b.pr());
    }

    /** 群系级植被指数估计(§4.3)：VI_est 按 atmosphere family 静态估算。 */
    public float viEstimate() {
        if (scannedChunks > 0 && loadedChunks.size() + scannedChunks > 0) {
            return viSum / scannedChunks;
        }
        return switch (atmosphereType.getPath()) {
            case "oxygen_rich" -> 900f;
            case "marsh_gas" -> 300f;
            case "spore_air" -> 500f;
            case "ambient_air" -> 120f;
            default -> 40f;
        };
    }

    private static float clamp01h(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
