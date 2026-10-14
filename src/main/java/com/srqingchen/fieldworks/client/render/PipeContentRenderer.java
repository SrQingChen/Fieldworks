package com.srqingchen.fieldworks.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.srqingchen.fieldworks.block.PipeBlock;
import com.srqingchen.fieldworks.block.PipeBlockEntity;
import com.srqingchen.fieldworks.registry.FWFluidTypes;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * 管道运载内容色芯 BER(§T6；261014 T2 扩三族)：
 * 缓冲非空时在中心+连接臂内绘制半透明内容色立方——
 * 流体管=流体色(FWFluidTypes 色表/原版水色)、物品管=琥珀、能量管=青色强脉冲。
 * 满载脉动微光；空载零渲染零开销。三族共用 {@link #submit} 几何。
 */
public class PipeContentRenderer implements BlockEntityRenderer<PipeBlockEntity, PipeContentRenderState> {

    /** 物品管内容色(琥珀)。 */
    public static final int ITEM_COLOR = 0xFFE8A33D;
    /** 能量管内容色(亮青)。 */
    public static final int ENERGY_COLOR = 0xFF35D8CE;

    public PipeContentRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public PipeContentRenderState createRenderState() {
        return new PipeContentRenderState();
    }

    @Override
    public void extractRenderState(PipeBlockEntity be, PipeContentRenderState state, float partialTicks,
                                   Vec3 cameraPosition, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        if (be.buffer.isEmpty() || be.amount() <= 0) {
            state.contentColor = null;
            return;
        }
        int c = FWFluidTypes.colorOf(be.buffer.getFluid().getFluidType());
        if (be.buffer.getFluid() == Fluids.WATER || be.buffer.getFluid() == Fluids.FLOWING_WATER) {
            c = 0xFF3F76E4;
        }
        fillState(state, be.getBlockState(), c, be.amount() / (float) PipeBlockEntity.CAPACITY, be, partialTicks);
    }

    /** 三族共用的状态填充。 */
    static <T extends BlockEntity> void fillState(PipeContentRenderState state, BlockState blockState,
                                                  int color, float fill, T be, float partialTicks) {
        state.contentColor = new float[]{((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f};
        state.fill = Mth.clamp(fill, 0.08f, 1f);
        state.phase = (be.getLevel() != null ? be.getLevel().getGameTime() : 0) + partialTicks;
        for (Direction dir : Direction.values()) {
            var prop = PipeBlock.PROP_BY_DIR.get(dir);
            state.connected[dir.ordinal()] = blockState.hasProperty(prop)
                    && blockState.getValue(prop) != PipeBlock.Conn.NONE;
        }
    }

    @Override
    public void submit(PipeContentRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                       CameraRenderState camera) {
        submitContent(state, poseStack, nodes);
    }

    /** 三族共用的几何提交。 */
    static void submitContent(PipeContentRenderState state, PoseStack poseStack, SubmitNodeCollector nodes) {
        if (state.contentColor == null) return;
        float pulse = 0.85f + 0.15f * Mth.sin(state.phase * 0.12f);
        float[] col = state.contentColor;
        float alpha = (0.42f + 0.40f * state.fill) * pulse;
        float r = Mth.clamp(col[0] * (0.7f + 0.5f * state.fill) * pulse, 0, 1);
        float g = Mth.clamp(col[1] * (0.7f + 0.5f * state.fill) * pulse, 0, 1);
        float b = Mth.clamp(col[2] * (0.7f + 0.5f * state.fill) * pulse, 0, 1);

        poseStack.pushPose();
        // 中心芯体(5.8..10.2，略小于管道 6×6×6 避免深度冲突)
        box(nodes, poseStack, 5.8f, 5.8f, 5.8f, 10.2f, 10.2f, 10.2f, r, g, b, alpha);
        // 各连接臂芯(贴臂内径 6..10)
        for (Direction dir : Direction.values()) {
            if (!state.connected[dir.ordinal()]) continue;
            switch (dir) {
                case DOWN -> box(nodes, poseStack, 6.0f, 0.0f, 6.0f, 10.0f, 6.0f, 10.0f, r, g, b, alpha);
                case UP -> box(nodes, poseStack, 6.0f, 10.0f, 6.0f, 10.0f, 16.0f, 10.0f, r, g, b, alpha);
                case NORTH -> box(nodes, poseStack, 6.0f, 6.0f, 0.0f, 10.0f, 10.0f, 6.0f, r, g, b, alpha);
                case SOUTH -> box(nodes, poseStack, 6.0f, 6.0f, 10.0f, 10.0f, 10.0f, 16.0f, r, g, b, alpha);
                case WEST -> box(nodes, poseStack, 0.0f, 6.0f, 6.0f, 6.0f, 10.0f, 10.0f, r, g, b, alpha);
                case EAST -> box(nodes, poseStack, 10.0f, 6.0f, 6.0f, 16.0f, 10.0f, 10.0f, r, g, b, alpha);
            }
        }
        poseStack.popPose();
    }

    private static void box(SubmitNodeCollector nodes, PoseStack poseStack,
                            float x0, float y0, float z0, float x1, float y1, float z1,
                            float r, float g, float b, float a) {
        nodes.submitCustomGeometry(poseStack, FWRenderTypes.PIPE_CONTENT, (pose, consumer) -> {
            quad(consumer, pose, x0, y0, z0, x1, y0, z1, r, g, b, a);  // down
            quad(consumer, pose, x0, y1, z1, x1, y1, z0, r, g, b, a);  // up
            quad(consumer, pose, x0, y0, z1, x1, y1, z1, r, g, b, a);  // south
            quad(consumer, pose, x1, y0, z0, x0, y1, z0, r, g, b, a);  // north
            quad(consumer, pose, x1, y0, z1, x1, y1, z0, r, g, b, a);  // east
            quad(consumer, pose, x0, y0, z0, x0, y1, z1, r, g, b, a);  // west
        });
    }

    private static void quad(VertexConsumer buf, PoseStack.Pose pose,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float r, float g, float b, float a) {
        // 以 (a→b) 为对角线的四边形面(轴对齐盒面)
        buf.addVertex(pose, ax, ay, az).setColor(r, g, b, a);
        buf.addVertex(pose, bx, ay, az).setColor(r, g, b, a);
        buf.addVertex(pose, bx, by, bz).setColor(r, g, b, a);
        buf.addVertex(pose, ax, by, bz).setColor(r, g, b, a);
    }

    // ═══════════ 物品管渲染器(琥珀芯) ═══════════

    /** 物品管道内容渲染：琥珀色芯，填充率=缓冲件数/容量。 */
    public static class ItemPipeRenderer implements BlockEntityRenderer<com.srqingchen.fieldworks.block.ItemPipeBlockEntity, PipeContentRenderState> {
        public ItemPipeRenderer(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public PipeContentRenderState createRenderState() {
            return new PipeContentRenderState();
        }

        @Override
        public void extractRenderState(com.srqingchen.fieldworks.block.ItemPipeBlockEntity be, PipeContentRenderState state,
                                       float partialTicks, Vec3 cameraPosition,
                                       ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
            if (be.buffer.isEmpty()) {
                state.contentColor = null;
                return;
            }
            fillState(state, be.getBlockState(), ITEM_COLOR,
                    be.count() / (float) com.srqingchen.fieldworks.block.ItemPipeBlockEntity.CAPACITY, be, partialTicks);
        }

        @Override
        public void submit(PipeContentRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                           CameraRenderState camera) {
            submitContent(state, poseStack, nodes);
        }
    }

    // ═══════════ 能量管渲染器(青色强脉冲) ═══════════

    /** 能量管道内容渲染：亮青芯，快速行波脉冲。 */
    public static class EnergyPipeRenderer implements BlockEntityRenderer<com.srqingchen.fieldworks.block.EnergyPipeBlockEntity, PipeContentRenderState> {
        public EnergyPipeRenderer(BlockEntityRendererProvider.Context ctx) {}

        @Override
        public PipeContentRenderState createRenderState() {
            return new PipeContentRenderState();
        }

        @Override
        public void extractRenderState(com.srqingchen.fieldworks.block.EnergyPipeBlockEntity be, PipeContentRenderState state,
                                       float partialTicks, Vec3 cameraPosition,
                                       ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
            BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
            if (be.amount() <= 0) {
                state.contentColor = null;
                return;
            }
            fillState(state, be.getBlockState(), ENERGY_COLOR,
                    be.amount() / (float) com.srqingchen.fieldworks.block.EnergyPipeBlockEntity.CAPACITY, be, partialTicks);
        }

        @Override
        public void submit(PipeContentRenderState state, PoseStack poseStack, SubmitNodeCollector nodes,
                           CameraRenderState camera) {
            submitContent(state, poseStack, nodes);
        }
    }
}
