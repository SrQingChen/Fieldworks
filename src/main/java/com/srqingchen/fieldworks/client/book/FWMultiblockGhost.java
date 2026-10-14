package com.srqingchen.fieldworks.client.book;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.block.GiantControllerBlock;
import com.srqingchen.fieldworks.giant.GiantStructure;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.SubmitCustomGeometryEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 书内多方块世界投影(261013 规划 T5)：
 * 替换 Patchouli 26.1-94 破损的世界幽灵(其把 viewRotationMatrix 二次 mulPose 进 pose，
 * 与着色器 ModelViewMat 叠加→相机旋转被应用两次→投影"反相移动"——261013 R4 诊断)。
 *
 * <p>本实现的正确性依据：</p>
 * <ul>
 *   <li>渲染走 NeoForge 官方 {@link SubmitCustomGeometryEvent}——事件提供的 pose 为
 *       相机相对(与原版活塞移动方块同语义)，只需 translate(worldPos - camera)。</li>
 *   <li>方块模型幽灵复用 Patchouli 公开的 {@code GhostBlockGeometry}
 *       (translucentMovingBlock 渲染类型)。</li>
 *   <li>锚定/旋转语义与 {@code GiantControllerEntity.checkStructure} 完全一致：
 *       锚点=控制核心落位，同一 4 向旋转变换——所见即所校验。</li>
 * </ul>
 */
public final class FWMultiblockGhost {
    private static boolean active;
    private static boolean anchored;
    private static String variant = "";
    @Nullable
    private static BlockPos anchor;
    private static int rot;   // 0..3，与 checkStructure 的旋转公式一致
    private static final Map<String, GiantStructure> CLIENT_CACHE = new ConcurrentHashMap<>();

    private FWMultiblockGhost() {}

    public static void register() {
        NeoForge.EVENT_BUS.register(FWMultiblockGhost.class);
    }

    /** 书页"可视化"按钮入口：同变体再点=关闭。返回 true=本次已开启(调用方据此关书)。 */
    public static boolean toggle(String newVariant) {
        if (active && newVariant.equals(variant)) {
            off();
            message("message.fieldworks.ghost.off");
            return false;
        }
        active = true;
        anchored = false;
        anchor = null;
        variant = newVariant;
        rot = 0;
        message("message.fieldworks.ghost.on");
        return true;
    }

    public static void off() {
        active = false;
        anchored = false;
        anchor = null;
    }

    // ── 客户端 tick：跟随准星(未锚定) + 环境清理 ──

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post e) {
        var mc = Minecraft.getInstance();
        if (!active) return;
        if (mc.level == null || mc.player == null) {
            off();
            return;
        }
        if (anchored && anchor != null
                && mc.player.position().distanceToSqr(Vec3.atCenterOf(anchor)) > 64 * 64) {
            off();   // 超过 64 格自动收起
        }
    }

    // ── 右键：锚定 / Shift 旋转 ──

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!active || !e.getLevel().isClientSide()) return;   // 仅客户端处理
        var player = e.getEntity();
        if (player != Minecraft.getInstance().player) return;
        if (anchored) {
            if (player.isShiftKeyDown()) {
                rot = (rot + 1) % 4;
                e.setCanceled(true);
                report(anchor, true);
            }
            return;
        }
        anchor = e.getPos();
        rot = rotFromFacing(player.getDirection());
        anchored = true;
        e.setCanceled(true);
        report(anchor, false);
    }

    /** 锚定/旋转反馈：缺失统计进动作栏。 */
    private static void report(@Nullable BlockPos pos, boolean rotated) {
        if (pos == null) return;
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var struct = structure(variant);
        int total = struct.blocks().size();
        int missing = countMissing(struct, pos, rot);
        if (rotated) {
            Minecraft.getInstance().gui.setOverlayMessage(
                    Component.translatable("message.fieldworks.ghost.rotate", rot * 90), false);
        }
        player.sendSystemMessage(
                Component.translatable("message.fieldworks.ghost.anchored", missing, total));
    }

    private static int rotFromFacing(Direction dir) {
        return switch (dir) {
            case SOUTH -> 0;
            case WEST -> 1;
            case NORTH -> 2;
            case EAST -> 3;
            default -> 0;
        };
    }

    // ── 渲染(SubmitCustomGeometryEvent：相机相对 pose，无双重旋转) ──

    @SubscribeEvent
    public static void onSubmitGeometry(SubmitCustomGeometryEvent e) {
        if (!active) return;
        var mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        BlockPos center;
        if (anchored && anchor != null) {
            center = anchor;
        } else if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit) {
            center = hit.getBlockPos();   // 未锚定：跟随准星
        } else {
            return;
        }
        GiantStructure struct = structure(variant);
        if (struct.blocks().isEmpty()) return;
        int[] c = struct.controllerOffset();

        Vec3 cam = mc.gameRenderer.getMainCamera().position();
        float pulse = 0.32f + 0.06f * (float) Math.sin((mc.level.getGameTime() % 100000) * 0.12f);
        var pose = e.getPoseStack();
        var collector = e.getSubmitNodeCollector();

        for (GiantStructure.Placed p : struct.blocks()) {
            int dx = p.x() - c[0], dy = p.y() - c[1], dz = p.z() - c[2];
            int rx, rz;
            switch (rot) {
                case 1 -> { rx = dz; rz = -dx; }
                case 2 -> { rx = -dx; rz = -dz; }
                case 3 -> { rx = -dz; rz = dx; }
                default -> { rx = dx; rz = dz; }
            }
            BlockPos worldPos = center.offset(rx, dy, rz);
            if (worldPos.equals(center)) continue;   // 控制核心位=锚点本身
            BlockState cur = mc.level.getBlockState(worldPos);
            if (cur.getBlock() == p.block()) continue;   // 已放置的不再画

            pose.pushPose();
            pose.translate(worldPos.getX() - cam.x, worldPos.getY() - cam.y, worldPos.getZ() - cam.z);
            float alpha = p.block() instanceof GiantControllerBlock ? pulse + 0.25f : pulse;
            collector.submitCustomGeometry(pose,
                    net.minecraft.client.renderer.rendertype.RenderTypes.translucentMovingBlock(),
                    new vazkii.patchouli.client.multiblock.GhostBlockGeometry(
                            BlockPos.ZERO, p.block().defaultBlockState(), alpha, 1.0F));
            pose.popPose();
        }
    }

    private static int countMissing(GiantStructure struct, BlockPos center, int rotation) {
        var level = Minecraft.getInstance().level;
        if (level == null) return 0;
        int missing = 0;
        int[] c = struct.controllerOffset();
        for (GiantStructure.Placed p : struct.blocks()) {
            int dx = p.x() - c[0], dy = p.y() - c[1], dz = p.z() - c[2];
            int rx, rz;
            switch (rotation) {
                case 1 -> { rx = dz; rz = -dx; }
                case 2 -> { rx = -dx; rz = -dz; }
                case 3 -> { rx = -dz; rz = dx; }
                default -> { rx = dx; rz = dz; }
            }
            BlockPos worldPos = center.offset(rx, dy, rz);
            if (worldPos.equals(center)) continue;
            if (level.getBlockState(worldPos).getBlock() != p.block()) missing++;
        }
        return missing;
    }

    /** 客户端资源管理器装载结构(与服务端同 CODEC；mod jar 的 data 资源对客户端可见)。 */
    private static GiantStructure structure(String v) {
        return CLIENT_CACHE.computeIfAbsent(v, key -> {
            var id = Identifier.fromNamespaceAndPath(Fieldworks.MODID, "giant_structures/" + key + ".json");
            var res = Minecraft.getInstance().getResourceManager().getResource(id);
            if (res.isEmpty()) return new GiantStructure(java.util.List.of(), new int[0], new int[0]);
            try (var is = res.get().open()) {
                return GiantStructure.parseStream(is, key);
            } catch (Exception ex) {
                return new GiantStructure(java.util.List.of(), new int[0], new int[0]);
            }
        });
    }

    private static void message(String key) {
        var p = Minecraft.getInstance().player;
        if (p != null) p.sendSystemMessage(Component.translatable(key));
    }
}
