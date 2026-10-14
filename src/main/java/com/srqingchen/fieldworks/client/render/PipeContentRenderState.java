package com.srqingchen.fieldworks.client.render;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.jspecify.annotations.Nullable;

/** 管道运载内容渲染状态(§T6)：流体色 + 填充率 + 六向连接。 */
public class PipeContentRenderState extends BlockEntityRenderState {
    /** 流体颜色(RGB 0..1)；null = 空载不渲染。 */
    @Nullable
    public float[] contentColor;
    /** 填充率 0..1 → 亮度与不透明度。 */
    public float fill;
    /** 六向连接(索引 = Direction.ordinal)：true 则该向画流体臂。 */
    public final boolean[] connected = new boolean[6];
    /** 流动脉冲相位。 */
    public float phase;
}
