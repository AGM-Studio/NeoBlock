package xyz.agmstudio.neoblock.neo.world;

import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;
import xyz.agmstudio.neoblock.NeoBlockMod;
import xyz.agmstudio.neoblock.neo.tiers.TierSpec;
import xyz.agmstudio.neocore.data.NBTSaveable;

import javax.annotation.ParametersAreNonnullByDefault;


@ParametersAreNonnullByDefault
public class NeoBlockCooldown implements NBTSaveable {
    public interface Type {
        String id();
        void onFinish(NeoBlock block);
        void onStart(NeoBlock block);

        class TierResearch implements Type {
            private final TierSpec tier;
            public TierResearch(TierSpec tier) {
                this.tier = tier;
            }
            public static void create(TierSpec tier) {
                NeoBlockCooldown cooldown = new NeoBlockCooldown(null, new TierResearch(tier)); // TODO: with TierSpec holding block
                cooldown.time = tier.getResearchTime();

                // tier.block.addCooldown(cooldown); TODO: with TierSpec holding block
            }

            public String id() {
                return "cooldown-" + tier.getID();
            }
            public void onFinish(NeoBlock block) {
                tier.enable();
                tier.setResearched(true);
                tier.startSequence.addToQueue(block, false);
                tier.unlockActions.apply(block);
                NeoBlockMod.sendInstantMessage("message.neoblock.unlocked_tier", block.level, false, tier.getID());
            }
            public void onStart(NeoBlock block) {
                NeoBlockMod.sendInstantMessage("message.neoblock.unlocking_tier", block.level, false, tier.getID());
                tier.researchActions.apply(block);
            }
        }
        class Normal implements Type {
            private final static Type NORMAL = new Normal();
            public static void create(@NotNull NeoBlock block, long ticks) {
                if (!block.cooldowns.isEmpty()) {
                    NeoBlockCooldown last = block.cooldowns.get(block.cooldowns.size() - 1);
                    if (last.type instanceof Normal) {
                        last.time += ticks;
                        return;
                    }
                }

                NeoBlockCooldown cooldown = new NeoBlockCooldown(block, NORMAL);
                cooldown.time = ticks;

                block.addCooldown(cooldown);
            }
            public String id() {
                return "normal";
            }
            public void onFinish(NeoBlock block) {
                block.updateBlock(false);
            }
            public void onStart(NeoBlock block) {
            }
        }

        static Type parse(@NotNull String id) {
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
    private final NeoBlock block;
    private Type type;
    private NeoBlockCooldown(NeoBlock block, Type type) {
        this.block = block;
        this.type = type;
    }
    protected NeoBlockCooldown(NeoBlock block) {
        this.block = block;
    }

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

    public void onStart() {
        type.onStart(block);
    }
    public void onFinish() {
        type.onFinish(block);
    }
}