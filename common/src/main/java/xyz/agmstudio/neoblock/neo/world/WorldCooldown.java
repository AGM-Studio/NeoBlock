package xyz.agmstudio.neoblock.neo.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.animations.phase.UpgradePhaseAnimation;
import xyz.agmstudio.neoblock.animations.progress.UpgradeProgressAnimation;
import xyz.agmstudio.neoblock.data.NBTSaveable;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.neo.block.NeoBlockPos;
import xyz.agmstudio.neoblock.neo.tiers.TierManager;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;

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

                WorldManager.getWorldStatus().addCooldown(cooldown);
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
            public String id() {
                return "normal";
            }
            public void onFinish(ServerLevel level) {
                BlockManager.updateBlock(level, false);
            }
            public void onStart(ServerLevel level) {
                BlockManager.BEDROCK_SPEC.placeAt(level, NeoBlockPos.get());
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
    
    // Static methods
    public static void tick(ServerLevel level) {
        WorldData data = WorldManager.getWorldStatus();
        if (data == null || !data.isOnCooldown()) return;
        if (data.cooldowns.isEmpty()) return;
        WorldCooldown cooldown = data.cooldowns.get(0);
        if (cooldown.tick++ == 0) {
            cooldown.type.onStart(level);

            if (TierManager.progressbar != null) level.players().forEach(TierManager.progressbar::addPlayer);
            for (UpgradePhaseAnimation animation : TierManager.phaseAnimations)
                if (animation.isActiveOnUpgradeStart()) animation.animate(level);
        }
        if (cooldown.time > 0 && cooldown.tick >= cooldown.time) {
            cooldown.type.onFinish(level);
            data.removeCooldown(cooldown);

            if (data.cooldowns.isEmpty()) {
                if (TierManager.progressbar != null) TierManager.progressbar.removeAllPlayers();
                for (UpgradePhaseAnimation animation : TierManager.phaseAnimations)
                    if (animation.isActiveOnUpgradeFinish()) animation.animate(level);
            }
        } else {
            if (TierManager.progressbar != null) TierManager.progressbar.update(cooldown.tick, cooldown.time);
            for (UpgradeProgressAnimation animation : TierManager.progressAnimations)
                animation.upgradeTick(level, cooldown.tick);
        }

        WorldManager.getInstance().setDirty();
    }
}
