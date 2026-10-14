package com.srqingchen.fieldworks.client.render;

import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.srqingchen.fieldworks.Fieldworks;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * 场域工艺自定义 RenderType(261012规划 T6)。
 * PIPE_CONTENT：无贴图纯色半透明(position_color 核心 shader)，用于管道运载流体色芯。
 * 写法对齐 AE2 AERenderTypes.STORAGE_CELL_LEDS(26.1.2 管线)。
 */
public final class FWRenderTypes {
    public static final RenderPipeline PIPE_CONTENT_PIPELINE = RenderPipeline.builder(
                    new RenderPipeline.Snippet[]{RenderPipelines.MATRICES_PROJECTION_SNIPPET, RenderPipelines.GLOBALS_SNIPPET})
            .withLocation(Fieldworks.id("pipeline/pipe_content"))
            .withVertexShader("core/position_color")
            .withFragmentShader("core/position_color")
            .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS)
            .withDepthStencilState(DepthStencilState.DEFAULT)
            .build();

    public static final RenderType PIPE_CONTENT = RenderType.create(
            "fieldworks_pipe_content",
            RenderSetup.builder(PIPE_CONTENT_PIPELINE).bufferSize(4096).createRenderSetup());

    private FWRenderTypes() {}
}
