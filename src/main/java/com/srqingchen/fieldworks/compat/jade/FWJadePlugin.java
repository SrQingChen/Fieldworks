package com.srqingchen.fieldworks.compat.jade;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.block.PipeBlockEntity;
import com.srqingchen.fieldworks.machine.MachineBlock;
import com.srqingchen.fieldworks.machine.MachineBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * Jade 联动(§17 可诊断性的悬浮层)：
 * 机器：Fit% / 能量；管道：内容物与余量(§10.5 可读性)。
 */
@snownee.jade.api.WailaPlugin
public class FWJadePlugin implements IWailaPlugin {
    public static final Identifier MACHINE_DATA = Fieldworks.id("machine");
    public static final Identifier PIPE_DATA = Fieldworks.id("pipe");

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(new MachineServer(), MachineBlockEntity.class);
        registration.registerBlockDataProvider(new PipeServer(), PipeBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(new MachineClient(), MachineBlock.class);
        registration.registerBlockComponent(new PipeClient(), com.srqingchen.fieldworks.block.PipeBlock.class);
    }

    public static class MachineServer implements IServerDataProvider<BlockAccessor> {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof MachineBlockEntity m) {
                data.putInt("fit", (int) (m.getFit() * 100));
                data.putLong("energy", m.getEnergyStored());
                data.putLong("energyMax", m.getEnergyMax());
            }
        }

        @Override
        public Identifier getUid() {
            return MACHINE_DATA;
        }
    }

    public static class MachineClient implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (data.contains("fit")) {
                tooltip.add(Component.translatable("jade.fieldworks.fit", data.getIntOr("fit", 0)));
                if (data.getLongOr("energyMax", 0) > 0) {
                    tooltip.add(Component.translatable("jade.fieldworks.energy",
                            data.getLongOr("energy", 0), data.getLongOr("energyMax", 0)));
                }
            }
        }

        @Override
        public Identifier getUid() {
            return MACHINE_DATA;
        }
    }

    public static class PipeServer implements IServerDataProvider<BlockAccessor> {
        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (accessor.getBlockEntity() instanceof PipeBlockEntity p) {
                data.putInt("amt", p.amount());
                if (!p.buffer.isEmpty()) {
                    var fid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(p.buffer.getFluid());
                    data.putString("fluid", fid == null ? "" : fid.toString());
                }
            }
        }

        @Override
        public Identifier getUid() {
            return PIPE_DATA;
        }
    }

    public static class PipeClient implements IBlockComponentProvider {
        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            int amt = data.getIntOr("amt", 0);
            if (amt > 0) {
                String fid = data.getStringOr("fluid", "");
                String name = fid.isEmpty() ? "?" : fid.substring(fid.indexOf(':') + 1);
                tooltip.add(Component.literal(String.format("%s · %s/%smB", name, amt, PipeBlockEntity.CAPACITY)));
            }
        }

        @Override
        public Identifier getUid() {
            return PIPE_DATA;
        }
    }
}
