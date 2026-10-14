package com.srqingchen.fieldworks.machine;

import com.srqingchen.fieldworks.Fieldworks;
import com.srqingchen.fieldworks.api.field.FieldChangedEvent;
import com.srqingchen.fieldworks.api.field.FieldRegistry;
import com.srqingchen.fieldworks.api.recipe.FieldProfile;
import com.srqingchen.fieldworks.api.recipe.MachineRecipe;
import com.srqingchen.fieldworks.atmosphere.AtmosphereInstanceConsume;
import com.srqingchen.fieldworks.carbon.CarbonState;
import com.srqingchen.fieldworks.fieldengine.AtmosphereInstance;
import com.srqingchen.fieldworks.fieldengine.EngineSavedData;
import com.srqingchen.fieldworks.fieldengine.FieldEngine;
import com.srqingchen.fieldworks.fieldengine.FWFields;
import com.srqingchen.fieldworks.registry.FWBlockEntities;
import com.srqingchen.fieldworks.registry.FWFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 通用机器方块实体(§10.1 原则 1)：场感知 + 能量 + 配方执行器。
 * 47 台工艺机器共享本类，差异全部在 {@link MachineSpec} + 数据包配方。
 *
 * <p>软门槛执行(§7.2)：Fit ∈ [0.6,1]，速度 ×= Fit，能耗 ×= (1+(1−Fit)×0.5)。</p>
 * <p>休眠调度(§21.1)：无任务/无输入不 tick 处理逻辑(仅做极廉价的能量沉降检查)。</p>
 * <p>六面 IO(§10.3/§16.1)：面独立 INPUT/OUTPUT/DISABLED/AUTO，wrench 旋转不改 IO。</p>
 */
public class MachineBlockEntity extends BlockEntity implements MenuProvider, FieldEngine.Subscriber, Container {
    public final MachineSpec spec;
    public final NonNullList<ItemStack> items;
    public final List<FWTank> tanks;
    public final SimpleEnergyHandler energy;
    private final Map<Direction, IOSide> io = new EnumMap<>(Direction.class);

    // ── 加工状态 ──
    @Nullable
    protected RecipeHolder<MachineRecipe> currentRecipe;
    protected int progress;         // 累计 tick
    protected int progressTotal;
    protected float fit = 1f;
    protected boolean active;
    protected boolean sleeping = true;

    // 发电机状态
    protected int burnRemaining;
    protected int burnTotal;

    // ── 客户端镜像(update tag 同步，§21.2 GUI_ONLY) ──
    public float clientTemp, clientHumidity, clientLight, clientMagnetic, clientGravity;
    public String clientAtmosphere = "";

    // ── 环境实时镜像(§T4：服务端 20t 刷新, 经菜单数据槽同步 → GUI 动态) ──
    public float envTemp = 20f, envHumidity = 45f, envLight = 12f, envMagnetic = 0f, envGravity = 1f;
    public int envAtmoIndex = -1;
    private int envRefreshTimer;
    /** 最近一次配方的场档案(空闲时仍用于 Fit 展示评估)。 */
    private com.srqingchen.fieldworks.api.recipe.@org.jspecify.annotations.Nullable FieldProfile lastProfile;

    public MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, MachineSpec spec) {
        super(type, pos, state);
        this.spec = spec;
        this.items = NonNullList.withSize(spec.inputSlots() + spec.outputSlots() + 4, ItemStack.EMPTY); // +4 升级芯槽
        this.tanks = new ArrayList<>();
        for (int i = 0; i < spec.tanks(); i++) {
            tanks.add(new FWTank(spec.tankCapacity(), this::markChanged));
        }
        this.energy = spec.energyCapacity() > 0
                ? new SimpleEnergyHandler(spec.energyCapacity(), spec.maxEnergyReceive(), spec.maxEnergyReceive())
                : null;
        for (Direction d : Direction.values()) io.put(d, IOSide.AUTO);
    }

    private void markChanged() {
        setChanged();
    }

    // ═══════════ tick ═══════════

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        if (level.isClientSide()) return;
        // 环境实时刷新(§T4)：每 20t 快照一次(区块级缓存, 极廉价)；空闲机器 Fit 也随环境联动
        if (be.envRefreshTimer-- <= 0) {
            be.envRefreshTimer = 20;
            be.refreshEnvironment((ServerLevel) level);
        }
        switch (be.spec.mode()) {
            case GENERATOR -> be.tickGenerator((ServerLevel) level, pos);
            case FLUID_BURNER -> be.tickFluidBurner((ServerLevel) level);
            case STORAGE -> { /* 储气罐：零逻辑 */ }
            case PUMP -> be.tickPump((ServerLevel) level);
            default -> be.tickProcessor((ServerLevel) level, pos);
        }
    }

    /** 无限水泵默认空实现(由 {@code InfinitePumpBlockEntity} 覆写，261014 T7)。 */
    protected void tickPump(ServerLevel level) {}

    /** 环境镜像刷新 + 空闲 Fit 重评(§T4 环境适配率动态化)。 */
    private void refreshEnvironment(ServerLevel level) {
        try {
            var snap = FieldEngine.snapshot(level, worldPosition);
            envTemp = snap.value(FWFields.TEMPERATURE);
            envHumidity = snap.value(FWFields.HUMIDITY);
            envLight = snap.value(FWFields.LIGHT);
            envMagnetic = snap.value(FWFields.MAGNETIC);
            envGravity = snap.value(FWFields.GRAVITY);
            envAtmoIndex = com.srqingchen.fieldworks.api.atmosphere.AtmosphereRegistry.indexOf(snap.atmosphereType());
            // 无进行中配方时，用最近档案就地重评 → Fit 随环境/位置动态变化
            if (currentRecipe == null && lastProfile != null) {
                var r = lastProfile.evaluate(level, worldPosition);
                fit = r.fit();
            }
        } catch (Exception ignored) {}
    }

    /** 处理器：场感知 → 配方匹配 → Fit 计算 → 能量消耗 → 产出(§7.2)。 */
    protected void tickProcessor(ServerLevel level, BlockPos pos) {
        if (sleeping && !hasWorkAvailable()) return;
        sleeping = false;
        // 1. 场感知(§3.3)：Fit 评估
        FieldProfile profile = currentRecipe != null ? currentRecipe.value().profile() : FieldProfile.EMPTY;
        // 2. 配方推进
        if (currentRecipe == null) {
            currentRecipe = findRecipe(level);
            if (currentRecipe == null) {
                sleeping = true;
                if (active) setActive(false);
                return;
            }
            progress = 0;
            progressTotal = currentRecipe.value().time();
            if (!consumeInputs(true)) {
                currentRecipe = null;
                sleeping = true;
                return;
            }
        }
        MachineRecipe recipe = currentRecipe.value();
        lastProfile = recipe.profile();
        var result = recipe.profile().evaluate(level, pos);
        fit = result.fit();
        if (!result.gate()) {
            // 硬门槛未达(仅人工场类)：配方冻结，等待场变化(§11.2)
            if (active) setActive(false);
            return;
        }
        int energyCost = Math.max(1, Math.round(recipe.energy() / (float) recipe.time() * (1f + (1f - fit) * 0.5f)));
        if (energy != null && energy.getAmountAsInt() < energyCost) {
            if (active) setActive(false);
            return;
        }
        setActive(true);
        if (fit >= 0.9f) {
            com.srqingchen.fieldworks.util.AdvancementHelper.awardNearby(level, worldPosition, 16, "siting_master");
        }
        if (energy != null) com.srqingchen.fieldworks.util.EnergyIo.extractSafe(energy, energyCost);
        progress++;
        if (progress >= progressTotal) {
            finishCraft(level, recipe);
        }
    }

    /** 完成一次执行：产出 + 大气消耗三模式(§7.3) + 碳申报(§5.1)。 */
    protected void finishCraft(ServerLevel level, MachineRecipe recipe) {
        if (!consumeInputs(false)) {
            currentRecipe = null;
            return;
        }
        for (int i = 0; i < recipe.outputs().size(); i++) {
            float chance = i < recipe.chances().size() ? recipe.chances().get(i) : 1f;
            if (level.getRandom().nextFloat() > chance) continue;
            ItemStack out = recipe.outputs().get(i).create();
            int slot = spec.inputSlots();
            if (slot < items.size() && !out.isEmpty()) {
                items.set(slot, out);   // MVP：单产物直接放置输出槽
                markChanged();
            }
        }
        // 流体输入消耗 + 流体产出(§7.1)
        recipe.fluidInput().ifPresent(fs -> {
            var wanted = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(fs.fluid());
            if (wanted.isEmpty()) return;
            for (FWTank t : tanks) {
                if (t.getFluid().getFluid() == wanted.get().value()) {
                    t.drainInternal(fs.amount());
                    break;
                }
            }
        });
        for (var tpl : recipe.fluidOutputs()) {
            FluidStack fs = tpl.stack();
            if (fs.isEmpty()) continue;
            for (FWTank t : tanks) {
                t.fillInternal(fs.copy());
                break;
            }
        }
        // 大气三模式(§7.3)：消耗/排放作用于群系实例池
        recipe.atmosphereCost().ifPresent(cost ->
                AtmosphereInstanceConsume.apply(level, worldPosition, cost.ox(), cost.fg(), cost.cr()));
        // 碳申报：耗电配方按能量比例近似(§5.1 埋点)
        CarbonState.report(level, recipe.energy() * 1e-6);
        progress = 0;
        currentRecipe = null;
        level.playSound(null, worldPosition, SoundEvents.COPPER_BULB_TURN_ON, SoundSource.BLOCKS, 0.4f, 1.2f);
        setChanged();
    }

    /** matches/消耗输入(261014 T6：计数配料，每条声明消耗 count 件，可跨槽清点)。simulate=true 只检验。 */
    protected boolean consumeInputs(boolean simulate) {
        MachineRecipe recipe = currentRecipe.value();
        NonNullList<ItemStack> snapshot = NonNullList.create();
        snapshot.addAll(items);
        for (MachineRecipe.InputEntry entry : recipe.inputs()) {
            int need = entry.count();
            for (int i = 0; i < spec.inputSlots() && i < snapshot.size() && need > 0; i++) {
                ItemStack s = snapshot.get(i);
                if (!s.isEmpty() && entry.test(s)) {
                    int take = Math.min(need, s.getCount());
                    ItemStack left = s.copyWithCount(s.getCount() - take);
                    snapshot.set(i, left.getCount() > 0 ? left : ItemStack.EMPTY);
                    if (!simulate) items.set(i, left.getCount() > 0 ? left : ItemStack.EMPTY);
                    need -= take;
                }
            }
            if (need > 0) return false;
        }
        if (!simulate) markChanged();
        return true;
    }

    /** 查找配方(261013 T2：机器→配方族精确取族配方，白名单用于同族多机分级)。 */
    @SuppressWarnings("unchecked")
    protected RecipeHolder<MachineRecipe> findRecipe(ServerLevel level) {
        var family = com.srqingchen.fieldworks.api.recipe.MachineRecipeFamily.fromMachine(spec.id());
        if (family == null) return null;
        var recipeManager = level.getServer().getRecipeManager();
        var map = recipeManager.recipeMap();
        var all = map.byType((net.minecraft.world.item.crafting.RecipeType<MachineRecipe>) family.type());
        for (var holder : all) {
            if (!(holder.value() instanceof MachineRecipe mr)) continue;
            if (!mr.fitsMachine(spec.id())) continue;
            // 输入检验(261014 T6：计数配料)
            boolean ok = true;
            var snapshot = new ArrayList<ItemStack>();
            for (int i = 0; i < spec.inputSlots() && i < items.size(); i++) snapshot.add(items.get(i).copy());
            for (MachineRecipe.InputEntry entry : mr.inputs()) {
                int need = entry.count();
                for (ItemStack s : snapshot) {
                    if (!s.isEmpty() && entry.test(s)) {
                        int take = Math.min(need, s.getCount());
                        s.shrink(take);
                        need -= take;
                        if (need <= 0) break;
                    }
                }
                if (need > 0) {
                    ok = false;
                    break;
                }
            }
            if (!ok) continue;
            // 流体输入检验(§7.1 fluid_input)
            if (mr.fluidInput().isPresent()) {
                var spec0 = mr.fluidInput().get();
                boolean fluidOk = false;
                var wanted = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(spec0.fluid());
                if (wanted.isPresent()) {
                    for (FWTank t : tanks) {
                        if (t.getFluid().getFluid() == wanted.get().value() && t.getAmount() >= spec0.amount()) {
                            fluidOk = true;
                            break;
                        }
                    }
                }
                if (!fluidOk) continue;
            }
            // 输出槽空位检验
            int outSlot = spec.inputSlots();
            if (outsNeedSlot(mr) && outSlot < items.size() && !items.get(outSlot).isEmpty()) continue;
            return (RecipeHolder<MachineRecipe>) holder;
        }
        return null;
    }

    protected boolean outsNeedSlot(MachineRecipe mr) {
        return !mr.outputs().isEmpty();
    }

    protected boolean hasWorkAvailable() {
        for (int i = 0; i < spec.inputSlots() && i < items.size(); i++) {
            if (!items.get(i).isEmpty()) return true;
        }
        for (FWTank t : tanks) {
            if (!t.isEmpty()) return true;
        }
        return energy != null && energy.getAmountAsInt() > 0;
    }

    // ═══════════ 发电机(§8.3 蒸汽发电机 16J/t) ═══════════

    protected void tickGenerator(ServerLevel level, BlockPos pos) {
        boolean wasLit = burnRemaining > 0;
        if (burnRemaining > 0) {
            burnRemaining--;
            if (energy != null) {
                int room = (int) (energy.getCapacityAsLong() - energy.getAmountAsLong());
                com.srqingchen.fieldworks.util.EnergyIo.insertSafe(energy, Math.min(spec.maxEnergyReceive(), room));
            }
        } else {
            // 燃料消耗：燃烧槽物品 + 水箱(蒸汽发电机需要水，§8.3 内置水箱)
            ItemStack fuel = items.get(0);
            int burn = fuel.getBurnTime(null, level.fuelValues());
            if (burn > 0 && hasWaterForSteam()) {
                com.srqingchen.fieldworks.util.AdvancementHelper.awardNearby(level, worldPosition, 16, "power_on");
                burnRemaining = burn / 4;                    // 折算到 16J/t 档
                burnTotal = burnRemaining;
                var remainder = fuel.getCraftingRemainder();
                fuel.shrink(1);
                if (fuel.isEmpty()) items.set(0, remainder != null ? remainder.create() : ItemStack.EMPTY);
                consumeWaterForSteam();
                CarbonState.report(level, 0.004);            // 碳申报(§5.1)
                markChanged();
            }
        }
        if (wasLit != (burnRemaining > 0)) {
            level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
        }
    }

    protected boolean hasWaterForSteam() {
        if (spec.id().getPath().equals("steam_generator")) {
            return !tanks.isEmpty() && tanks.get(0).getFluid().getFluid() == net.minecraft.world.level.material.Fluids.WATER;
        }
        return true;
    }

    protected void consumeWaterForSteam() {
        if (!tanks.isEmpty()) tanks.get(0).drainInternal(100);
    }

    /** 蒸汽轮机：消耗蒸汽流体 → 电(§8.1 能源线)。 */
    protected void tickFluidBurner(ServerLevel level) {
        if (tanks.isEmpty() || energy == null) return;
        FluidStack steam = tanks.get(0).getFluid();
        if (steam.getFluid() == FWFluids.STEAM.get() && steam.getAmount() >= 10) {
            tanks.get(0).drainInternal(10);
            com.srqingchen.fieldworks.util.EnergyIo.insertSafe(energy, spec.maxEnergyReceive());
        }
    }

    // ═══════════ 持久化 ═══════════

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        var list = out.list("items", ItemStack.OPTIONAL_CODEC);
        for (ItemStack s : items) list.add(s);
        for (int i = 0; i < tanks.size(); i++) {
            tanks.get(i).serialize(out.child("tank" + i));
        }
        if (energy != null) energy.serialize(out);
        out.putInt("progress", progress);
        out.putInt("burn", burnRemaining);
        out.putInt("burn_total", burnTotal);
        out.putFloat("fit", fit);
        for (var e : io.entrySet()) {
            out.putString("io_" + e.getKey().getName(), e.getValue().name());
        }
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        int i = 0;
        for (ItemStack s : in.listOrEmpty("items", ItemStack.OPTIONAL_CODEC)) {
            if (i < items.size()) items.set(i, s);
            i++;
        }
        for (int t = 0; t < tanks.size(); t++) {
            final int idx = t;
            in.child("tank" + t).ifPresent(child -> tanks.get(idx).deserialize(child));
        }
        if (energy != null) energy.deserialize(in);
        progress = in.getIntOr("progress", 0);
        burnRemaining = in.getIntOr("burn", 0);
        burnTotal = in.getIntOr("burn_total", 0);
        fit = in.getFloatOr("fit", 1f);
        for (Direction d : Direction.values()) {
            String v = in.getStringOr("io_" + d.getName(), "AUTO");
            try {
                io.put(d, IOSide.valueOf(v));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    // ── 客户端同步(§21.2 GUI_ONLY) ──

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putFloat("fit", fit);
        tag.putBoolean("active", active);
        tag.putInt("progress", progressTotal > 0 ? progress * 100 / progressTotal : 0);
        if (!tanks.isEmpty()) {
            tag.putInt("tank_amount", tanks.get(0).getAmount());
            if (!tanks.get(0).isEmpty()) {
                Identifier fid = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(tanks.get(0).getFluid().getFluid());
                tag.putString("tank_fluid", fid == null ? "" : fid.toString());
            }
        }
        // 六面 IO(GUI 侧面配置镜像，§10.3)
        for (var e : io.entrySet()) tag.putString("io_" + e.getKey().getName(), e.getValue().name());
        // 场快照镜像(§15.5 三层 tooltip 的二/三层数据源)
        if (level instanceof ServerLevel sl) {
            try {
                var snap = FieldEngine.snapshot(sl, worldPosition);
                tag.putFloat("f_temp", snap.value(FWFields.TEMPERATURE));
                tag.putFloat("f_hum", snap.value(FWFields.HUMIDITY));
                tag.putFloat("f_light", snap.value(FWFields.LIGHT));
                tag.putFloat("f_mag", snap.value(FWFields.MAGNETIC));
                tag.putFloat("f_grav", snap.value(FWFields.GRAVITY));
                tag.putString("f_atmo", snap.atmosphereType().getPath());
            } catch (Exception ignored) {}
        }
        return tag;
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net,
                             net.minecraft.world.level.storage.ValueInput in) {
        super.onDataPacket(net, in);
        readClientMirror(in);
    }

    @Override
    public void handleUpdateTag(net.minecraft.world.level.storage.ValueInput in) {
        super.handleUpdateTag(in);
        readClientMirror(in);
    }

    private void readClientMirror(net.minecraft.world.level.storage.ValueInput in) {
        clientTemp = in.getFloatOr("f_temp", 20f);
        clientHumidity = in.getFloatOr("f_hum", 45f);
        clientLight = in.getFloatOr("f_light", 12f);
        clientMagnetic = in.getFloatOr("f_mag", 0f);
        clientGravity = in.getFloatOr("f_grav", 1f);
        clientAtmosphere = in.getStringOr("f_atmo", "");
        for (Direction d : Direction.values()) {
            String v = in.getStringOr("io_" + d.getName(), "AUTO");
            try {
                io.put(d, IOSide.valueOf(v));
            } catch (IllegalArgumentException ignored) {}
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // ═══════════ 场订阅(§6.6 机器永不轮询) ═══════════

    @Override
    public void onFieldChanged(FieldChangedEvent event) {
        sleeping = false;   // 唤醒以重评 Fit
    }

    public void subscribeFields(ServerLevel level) {
        FieldEngine.subscribe(level, worldPosition, this);
    }

    public void unsubscribeFields(ServerLevel level) {
        FieldEngine.unsubscribe(level, worldPosition, this);
    }

    @Override
    public void setRemoved() {
        if (getLevel() instanceof ServerLevel sl) {
            unsubscribeFields(sl);
        }
        super.setRemoved();
    }

    // ═══════════ IO 配置(§10.3 底座能力) ═══════════

    public IOSide ioOf(Direction d) {
        return io.getOrDefault(d, IOSide.AUTO);
    }

    public void setIo(Direction d, IOSide side) {
        io.put(d, side);
        setChanged();
    }

    public Map<Direction, IOSide> ioMap() {
        return io;
    }

    // ═══════════ 菜单(§15.2) ═══════════

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable(spec.langKey());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.srqingchen.fieldworks.machine.MachineMenu(id, inv, this);
    }

    public void openMenu(ServerPlayer sp) {
        sp.openMenu(this, buf -> {
            buf.writeBlockPos(worldPosition);
            buf.writeIdentifier(spec.id());
        });
    }

    // ── 杂项 ──

    public boolean isActive() {
        return active;
    }

    /** 相位桥等外部件访问物品栏(只读副本语义由调用方维护)。 */
    public net.minecraft.core.NonNullList<ItemStack> items() {
        return items;
    }

    protected void setActive(boolean v) {
        if (active != v) {
            active = v;
            // ACTIVE 方块态(§16.1 激活贴图+发光，Thermal 40t 防抖由调用频率天然保证)
            var state = getBlockState();
            if (state.hasProperty(com.srqingchen.fieldworks.machine.MachineBlock.ACTIVE)) {
                getLevel().setBlock(worldPosition, state.setValue(com.srqingchen.fieldworks.machine.MachineBlock.ACTIVE, v),
                        net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            } else {
                getLevel().sendBlockUpdated(worldPosition, state, state, net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            }
            setChanged();
        }
    }

    /** 客户端本地预览(真实值经 C2S/S2S 同步)。 */
    public void setIoLocal(Direction d, IOSide side) {
        io.put(d, side);
    }

    /** IO 变更广播(GUI 内改动后调用，§10.3)。 */
    public void syncIoToClient() {
        getLevel().sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
        setChanged();
    }

    public int tankAmount(int idx) {
        return idx < tanks.size() ? tanks.get(idx).getAmount() : 0;
    }

    public int tankCapacity(int idx) {
        return idx < tanks.size() ? tanks.get(idx).getCapacity() : 0;
    }

    public List<FWTank> tanks() {
        return tanks;
    }

    /** 燃料燃烧百分比(发电机)。 */
    public int burnPercent() {
        return burnTotal > 0 ? burnRemaining * 100 / burnTotal : 0;
    }

    /** 机器面对该方向是否接受外部输入(管道入料判定，§10.5)。 */
    public boolean acceptsFluidFrom(Direction fromSide) {
        IOSide s = ioOf(fromSide);
        return s == IOSide.INPUT || s == IOSide.AUTO;
    }

    /** 机器是否从该方向输出流体(管道取料判定)。 */
    public boolean outputsFluidTo(Direction toSide) {
        IOSide s = ioOf(toSide);
        return s == IOSide.OUTPUT || s == IOSide.AUTO;
    }

    /** 管道从机器取流体(§10.5 PULL 语义)：返回实际取出量。 */
    public FluidStack drainForPipe(Direction side, int maxAmount) {
        if (!outputsFluidTo(side) || tanks.isEmpty()) return FluidStack.EMPTY;
        FWTank t = tanks.get(0);
        FluidStack cur = t.getFluid();
        if (cur.isEmpty()) return FluidStack.EMPTY;
        int take = Math.min(maxAmount, cur.getAmount());
        t.drainInternal(take);
        setChanged();
        return cur.copyWithAmount(take);
    }

    /** 管道向机器注入流体(§10.5 PUSH 语义)：返回实际注入量。 */
    public int fillFromPipe(Direction side, FluidStack stack) {
        if (!acceptsFluidFrom(side) || stack.isEmpty()) return 0;
        for (FWTank t : tanks) {
            int filled = t.fillInternal(stack);
            if (filled > 0) {
                setChanged();
                return filled;
            }
        }
        return 0;
    }

    // ── 物品管道读写口(261014 T2：尊重六面 IO) ──

    /** 物品管道从机器输出面取物：返回取出的物品(空 = 无可取)。 */
    public ItemStack extractItemsForPipe(Direction side, int maxCount) {
        if (!outputsFluidTo(side)) return ItemStack.EMPTY;   // OUTPUT/AUTO 同语义复用
        int start = spec.inputSlots();
        int end = start + spec.outputSlots();
        for (int i = start; i < end && i < items.size(); i++) {
            ItemStack s = items.get(i);
            if (!s.isEmpty()) {
                int take = Math.min(maxCount, s.getCount());
                ItemStack out = s.split(take);
                if (s.isEmpty()) items.set(i, ItemStack.EMPTY);
                setChanged();
                return out;
            }
        }
        return ItemStack.EMPTY;
    }

    /** 物品管道向机器输入面送料：返回未能放入的数量。 */
    public int insertItemFromPipe(Direction side, ItemStack stack) {
        if (!acceptsFluidFrom(side) || stack.isEmpty()) return stack.getCount();
        int left = stack.getCount();
        for (int i = 0; i < spec.inputSlots() && i < items.size() && left > 0; i++) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) {
                int put = Math.min(left, stack.getMaxStackSize());
                items.set(i, stack.copyWithCount(put));
                left -= put;
            } else if (ItemStack.isSameItemSameComponents(s, stack)) {
                int room = stack.getMaxStackSize() - s.getCount();
                if (room > 0) {
                    int put = Math.min(left, room);
                    items.set(i, s.copyWithCount(s.getCount() + put));
                    left -= put;
                }
            }
        }
        if (left != stack.getCount()) setChanged();
        return left;
    }

    // ── 能量管道读写口(261014 T2：发电机 OUTPUT/AUTO 供出，耗电机器 INPUT/AUTO 受入) ──

    /** 能量管道从机器取电：返回实际取出的 FE。仅发电机/流体燃机对外供能(耗电机器不被管道抽空)。 */
    public int extractEnergyForPipe(Direction side, int maxAmount) {
        if (energy == null || !outputsFluidTo(side)) return 0;
        if (spec.mode() != MachineSpec.Mode.GENERATOR && spec.mode() != MachineSpec.Mode.FLUID_BURNER) return 0;
        int amt = Math.min(maxAmount, getEnergyStored());
        if (amt <= 0) return 0;
        com.srqingchen.fieldworks.util.EnergyIo.extractSafe(energy, amt);
        setChanged();
        return amt;
    }

    /** 能量管道向机器送电：返回实际送入的 FE。 */
    public int insertEnergyFromPipe(Direction side, int amount) {
        if (energy == null || !acceptsFluidFrom(side) || amount <= 0) return 0;
        int before = getEnergyStored();
        com.srqingchen.fieldworks.util.EnergyIo.insertSafe(energy, amount);
        int after = getEnergyStored();
        if (after != before) setChanged();
        return after - before;
    }

    public float getFit() {
        return fit;
    }

    public int getProgressPercent() {
        return progressTotal > 0 ? progress * 100 / progressTotal : 0;
    }

    public int getEnergyStored() {
        return energy == null ? 0 : energy.getAmountAsInt();
    }

    public int getEnergyMax() {
        return energy == null ? 0 : (int) energy.getCapacityAsLong();
    }

    /** 比较器输出模式(§10.3)：进度/场强/储量。 */
    public int comparatorSignal() {
        if (spec.mode() == MachineSpec.Mode.GENERATOR) {
            return burnTotal > 0 ? burnRemaining * 15 / burnTotal : 0;
        }
        if (spec.mode() == MachineSpec.Mode.STORAGE) {
            return tanks.isEmpty() ? 0 : tanks.get(0).getAmount() * 15 / Math.max(1, tanks.get(0).getCapacity());
        }
        return getProgressPercent() * 15 / 100;
    }

    // ═══════════ Container(漏斗/Slot 交互) ═══════════

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack s : items) {
            if (!s.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        ItemStack split = items.get(index).split(count);
        setChanged();
        return split;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        ItemStack out = items.get(index);
        items.set(index, ItemStack.EMPTY);
        setChanged();
        return out;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        items.set(index, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }
}
