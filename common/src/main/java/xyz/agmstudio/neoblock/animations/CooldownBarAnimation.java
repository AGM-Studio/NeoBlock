package xyz.agmstudio.neoblock.animations;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.world.NeoBlock;
import xyz.agmstudio.neoblock.neo.world.WorldManager;
import xyz.agmstudio.neocore.util.StringUtil;

import java.util.HashMap;

public class CooldownBarAnimation extends Animation {
    @ConfigField
    private String color = "red";
    @ConfigField("show-time")
    private boolean dynamic = true;

    private final HashMap<NeoBlock, ServerBossEvent> bars = new HashMap<>();
    public CooldownBarAnimation() {
        super("progressbar");
    }
    @Override protected void onRegister() {}

    public ServerBossEvent getBar(NeoBlock block) {
        if (bars.containsKey(block)) return bars.get(block);
        ServerBossEvent bar = new ServerBossEvent(Component.literal(""), BossEvent.BossBarColor.byName(color), BossEvent.BossBarOverlay.PROGRESS);
        bars.put(block, bar);
        return bar;
    }

    public void update(NeoBlock block, long ticks, long goal) {
        ServerBossEvent bar = getBar(block);
        bar.setProgress((float) ticks / goal);
        MutableComponent name = dynamic ?
                Component.translatable("bossbar.neoblock.upgrade_bar", StringUtil.formatTicks(goal - ticks)) :
                Component.translatable("bossbar.neoblock.upgrade_bar_no_time");

        bar.setName(name);
    }

    private static BlockHitResult raycastBlock(Player player, double reach) {
        Level level = player.level();
        var eyePos = player.getEyePosition(1.0F);
        var viewVec = player.getViewVector(1.0F);
        var endPos = eyePos.add(viewVec.x * reach, viewVec.y * reach, viewVec.z * reach);

        return level.clip(new ClipContext(eyePos, endPos, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
    }

    private final HashMap<Player, ServerBossEvent> playerBars = new HashMap<>();
    public void checkPlayer(Player entity) {
        if (!(entity instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) return;
        BlockHitResult looking = raycastBlock(player, 5.0);
        NeoBlockMod.getLogger().debug("Tick Player: {} @ {}", player, looking.getType());
        if (looking.getType() == HitResult.Type.BLOCK) {
            NeoBlock block = WorldManager.getNeoBlock(level, looking.getBlockPos());
            NeoBlockMod.getLogger().debug("Block: {} @ {}", block, looking.getBlockPos());
            if (block != null) {
                if (block.isOnCooldown()) {
                    ServerBossEvent bar = getBar(block);
                    if (playerBars.get(player) == bar) return;

                    bar.addPlayer(player);
                    playerBars.put(player, bar);
                    return;
                }
            }
        }
        if (playerBars.containsKey(player)) {
            ServerBossEvent bar = playerBars.remove(player);
            bar.removePlayer(player);
        }
    }
}