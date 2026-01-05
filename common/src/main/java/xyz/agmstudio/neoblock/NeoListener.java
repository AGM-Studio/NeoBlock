package xyz.agmstudio.neoblock;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.commands.NeoblockCommand;
import xyz.agmstudio.neoblock.compatibility.ForgivingVoid;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.loot.trade.NeoMerchant;
import xyz.agmstudio.neoblock.neo.world.WorldManager;

import java.util.HashSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class NeoListener {
    public static class EventResult<T> {
        private final T result;

        public EventResult(T result) {
            this.result = result;
        }
        public T getResult() {
            return result;
        }
    }
    public abstract static class CancelableEventResult<T> extends EventResult<T> {
        private final boolean canceled;

        public CancelableEventResult(T result, boolean canceled) {
            super(result);
            this.canceled = canceled;
        }
        public boolean isCanceled() {
            return canceled;
        }
    }

    public interface Ticker {
        void tick(ServerLevel level);
        boolean canTick(ServerLevel level);

        static Builder of(Consumer<ServerLevel> consumer) {
            return new Builder(consumer);
        }
        class Builder {
            @NotNull private final Consumer<ServerLevel> consumer;
            @NotNull private Predicate<ServerLevel> condition = level -> level.dimension() == Level.OVERWORLD;

            private Builder(@NotNull Consumer<ServerLevel> consumer) {
                this.consumer = consumer;
            }
            public Builder condition(@NotNull Predicate<ServerLevel> condition) {
                this.condition = condition;
                return this;
            }

            public Ticker build() {
                return new Ticker() {
                    @Override public void tick(ServerLevel level) {
                        consumer.accept(level);
                    }

                    @Override public boolean canTick(ServerLevel level) {
                        return level != null && condition.test(level);
                    }
                };
            }
        }
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    static final HashSet<Ticker> tickers = new HashSet<>();

    public static <T> void execute(Callable<T> callable) {
        executor.submit(callable);
    }
    public static void registerTicker(Consumer<ServerLevel> ticker) {
        tickers.add(Ticker.of(ticker).build());
    }
    public static void registerTicker(Consumer<ServerLevel> ticker, ResourceKey<Level> level) {
        tickers.add(Ticker.of(ticker).condition(l -> l.dimension() == level).build());
    }

    private static @Nullable ServerLevel getServerConditioned(LevelAccessor level, boolean isNotDisabled) {
        if (!(level instanceof ServerLevel server)) return null;
        if (isNotDisabled && WorldManager.getWorldData().isDisabled()) return null;

        return server;
    }

    public static void onWorldLoad(LevelAccessor accessor) {
        ServerLevel level = getServerConditioned(accessor, false);
        if (level != null && level.dimension() == Level.OVERWORLD) WorldManager.setup(level);
    }

    public static void onWorldTick(LevelAccessor accessor) {
        ServerLevel level = getServerConditioned(accessor, true);
        for (Ticker ticker : tickers) if (ticker.canTick(level)) ticker.tick(level);
    }

    public static void onBlockBroken(LevelAccessor accessor, ServerPlayer player, BlockPos pos, BlockState state) {
        ServerLevel level = getServerConditioned(accessor, true);
        if (player.isCreative()) return;
        if (BlockManager.isNeoBlock(level, pos))
            WorldManager.addBlocksBroken(player, 1);
        if (state.getBlock() == Blocks.END_PORTAL_FRAME)
            BlockManager.handleEndPortalFrameBreak(level, state, pos, player);
    }

    public static void onEntitySpawn(LevelAccessor accessor, Entity entity) {
        ServerLevel level = getServerConditioned(accessor, true);
        if (level == null) return;

        if (entity instanceof WanderingTrader trader) NeoMerchant.handleTrader(trader);
        if (entity instanceof ServerPlayer player) {
            if (WorldManager.getWorldData().isOnCooldown())
                Animation.addPlayer(player);
            NeoBlock.onPlayerJoin(level, player);
        }
    }

    public static void onRegisterCommands(CommandBuildContext buildContext, CommandDispatcher<CommandSourceStack> dispatcher) {
        NeoblockCommand.getInstance(buildContext).register(dispatcher);
    }

    public static final class LivingDamageResult extends CancelableEventResult<Float> {
        private LivingDamageResult(float damage, boolean canceled) {
            super(damage, canceled);
        }
    }
    public static LivingDamageResult onLivingDamage(LivingEntity entity, DamageSource source, float amount) {
        ServerLevel level = getServerConditioned(entity.level(), true);
        if (source.is(DamageTypes.FELL_OUT_OF_WORLD))
            if (ForgivingVoid.handleVoid(level, entity)) return new LivingDamageResult(0.0F, true);

        return null;
    }
}
