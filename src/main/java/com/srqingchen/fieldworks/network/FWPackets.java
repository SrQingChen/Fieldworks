package com.srqingchen.fieldworks.network;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.machine.IOSide;
import com.srqingchen.fieldworks.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * GUI 网络通道(§15.2 菜单层 / §8 同步协议)：
 * C2S IO 面配置 —— 侧面配置浮窗点击循环(参考文档 §4 交互层，clickType 语义同 Mek 0/1/2)。
 */
public final class FWPackets {
    public static final CustomPacketPayload.Type<C2SIoConfigPayload> IO_CONFIG =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Fieldworks.MODID, "io_config"));
    public static final CustomPacketPayload.Type<C2SPumpGearPayload> PUMP_GEAR =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(Fieldworks.MODID, "pump_gear"));

    public record C2SIoConfigPayload(BlockPos pos, Direction face, int op) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, C2SIoConfigPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, C2SIoConfigPayload::pos,
                Direction.STREAM_CODEC, C2SIoConfigPayload::face,
                ByteBufCodecs.VAR_INT, C2SIoConfigPayload::op,
                C2SIoConfigPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return IO_CONFIG;
        }
    }

    /** 261014 T7：无限水泵档位切换(0=低 1=高 2=无限)。 */
    public record C2SPumpGearPayload(BlockPos pos, int gear) implements CustomPacketPayload {
        public static final StreamCodec<RegistryFriendlyByteBuf, C2SPumpGearPayload> CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC, C2SPumpGearPayload::pos,
                ByteBufCodecs.VAR_INT, C2SPumpGearPayload::gear,
                C2SPumpGearPayload::new);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return PUMP_GEAR;
        }
    }

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(IO_CONFIG, C2SIoConfigPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp
                    && sp.level().getBlockEntity(payload.pos()) instanceof MachineBlockEntity be) {
                // 权限：距离校验
                if (sp.blockPosition().distSqr(payload.pos()) > 64) return;
                IOSide cur = be.ioOf(payload.face());
                IOSide next = payload.op() == 2 ? IOSide.AUTO
                        : payload.op() == 1 ? (cur == IOSide.INPUT ? IOSide.AUTO : cur.previous())
                        : cur.next();
                be.setIo(payload.face(), next);
                be.syncIoToClient();
            }
        }));
        registrar.playToServer(PUMP_GEAR, C2SPumpGearPayload.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer sp
                    && sp.level().getBlockEntity(payload.pos()) instanceof MachineBlockEntity be
                    && be instanceof com.srqingchen.fieldworks.machine.InfinitePumpBlockEntity pump) {
                if (sp.blockPosition().distSqr(payload.pos()) > 64) return;
                pump.setGear(payload.gear());
            }
        }));
    }

    private FWPackets() {}
}
