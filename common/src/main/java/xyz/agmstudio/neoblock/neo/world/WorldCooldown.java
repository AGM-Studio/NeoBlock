package xyz.agmstudio.neoblock.neo.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.animations.Animation;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;

import java.util.List;

public class WorldCooldown implements NBTSaveable {
    public interface Type {
        String id();
        void onFinish(ServerLevel level);
        void onStart(ServerLevel level);

        class TierResearch implements Type {
            private final TierSpec tier;
            public TierResearch(TierSpec tier) {
                this.tier = tier;
            }
            public static void create(TierSpec tier) {
                WorldCooldown cooldown = new WorldCooldown();
                cooldown.type = new TierResearch(tier);
                cooldown.time = tier.getResearchTime();

                WorldManager.getWorldData().addCooldown(cooldown);
            }

            public String id() {
                return "cooldown-" + tier.getID();
            }
            public void onFinish(ServerLevel level) {
                tier.enable();
                tier.setResearched(true);
                tier.startSequence.addToQueue(false);
                tier.unlockActions.apply(level);
                NeoBlock.sendInstantMessage("message.neoblock.unlocked_tier", level, false, tier.getID());
            }
            public void onStart(ServerLevel level) {
                NeoBlock.sendInstantMessage("message.neoblock.unlocking_tier", level, false, tier.getID());
                tier.researchActions.apply(level);
            }
        }
        class Normal implements Type {
            public static void create(long ticks) {
                List<WorldCooldown> cooldowns = WorldManager.getWorldData().getCooldowns();
                if (!cooldowns.isEmpty()) {
                    WorldCooldown last = cooldowns.get(cooldowns.size() - 1);
                    if (last.type instanceof Normal) {
                        last.time += ticks;
                        return;
                    }
                }

                WorldCooldown cooldown = new WorldCooldown();
                cooldown.type = new Normal();
                cooldown.time = ticks;

                WorldManager.getWorldData().addCooldown(cooldown);
            }
            public String id() {
                return "normal";
            }
            public void onFinish(ServerLevel level) {
                BlockManager.updateBlock(level, false);
            }
            public void onStart(ServerLevel level) {
            }
        }

        static Type parse(String id) {
            if (id.startsWith("cooldown-")) {
                int tier = Integer.parseInt(id.substring(9));
                TierSpec spec = WorldManager.getWorldTier(tier);
                return new TierResearch(spec);
            }
            return new Normal();
        }
    }

    @NBTData protected long time = 72000;
    @NBTData protected long tick = 0;
    protected Type type;

    @Override public CompoundTag onSave(CompoundTag tag) {
        tag.putString("type", type.id());
        return tag;
    }
    @Override public void onLoad(CompoundTag tag) {
        this.type = Type.parse(tag.getString("type"));
    }

    public long advanceBy(int value) {
        tick += value;
        return tick;
    }
    public long getTime() {
        return time;
    }
    public Type getType() {
        return type;
    }
    public long getTick() {
        return tick;
    }

    // Static methods
    private static boolean FIRST = true;
    public static void tick(ServerLevel level) {
        WorldData data = WorldManager.getWorldData();
        if (data == null || !data.isOnCooldown()) return;
        if (data.cooldowns.isEmpty()) return;
        WorldCooldown cooldown = data.cooldowns.get(0);
        if (cooldown.tick++ == 0) {
            cooldown.type.onStart(level);
            if (FIRST) Animation.animateCooldownStart(level);
        }
        if (FIRST) {
            FIRST = false;      // Will make sure to not play the cooldown start multiple times.
            WorldManager.getInstance().setDirty();
        }
        if (cooldown.time > 0 && cooldown.tick >= cooldown.time) {
            cooldown.type.onFinish(level);
            data.removeCooldown(cooldown);
            if (data.cooldowns.isEmpty()) {
                Animation.animateCooldownFinish(level);
                FIRST = true;   // Will make sure to play cooldown start animations
            }
        } else Animation.tickCooldown(level, cooldown);

        WorldManager.getInstance().setDirty();
    }
}
