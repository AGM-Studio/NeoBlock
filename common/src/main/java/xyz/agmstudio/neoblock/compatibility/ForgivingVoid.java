package xyz.agmstudio.neoblock.compatibility;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import xyz.agmstudio.neoblock.NeoBlock;
import xyz.agmstudio.neoblock.neo.block.BlockManager;
import xyz.agmstudio.neoblock.platform.implants.IConfig;
import xyz.agmstudio.neoblock.util.MinecraftUtil;

import java.util.*;
import java.util.function.Function;

public class ForgivingVoid {
    private static boolean players;
    private static boolean hostile;
    private static boolean villager;
    private static boolean livings;
    private static boolean items;
    private static boolean misc;

    private static boolean animatePlayers;
    private static boolean animateHostile;
    private static boolean animateVillager;
    private static boolean animateLivings;

    private static double offset;

    private static final HashMap<MobEffect, Integer> effects = new HashMap<>();
    private static final List<String> exceptions = new ArrayList<>();

    private static Function<LivingEntity, Float> playersDamager;
    private static Function<LivingEntity, Float> hostileDamager;
    private static Function<LivingEntity, Float> livingsDamager;

    private static Function<LivingEntity, Float> castDamager(Object value) {
        if (value instanceof String s) {
            if (s.equalsIgnoreCase("MAX"))  return e -> e.getMaxHealth() - 1.0F;
            if (s.equalsIgnoreCase("HALF")) return e -> e.getMaxHealth() / 2.0F;

            // "ONE_HEART" is the default
            return e -> e.getHealth() - 1.0F;
        }
        if (value instanceof Float f)   return e -> f;
        if (value instanceof Double d)  return e -> d.floatValue();
        if (value instanceof Integer i) return e -> i.floatValue();
        if (value instanceof Long l)    return e -> l.floatValue();
        return null;
    }

    public static void loadConfig() {
        IConfig config = NeoBlock.getConfig();
        ForgivingVoid.players   = config.get("forgiving-void.players", true);
        ForgivingVoid.hostile   = config.get("forgiving-void.hostile", true);
        ForgivingVoid.villager  = config.get("forgiving-void.villager", true);
        ForgivingVoid.livings   = config.get("forgiving-void.livings", true);
        ForgivingVoid.items     = config.get("forgiving-void.items", true);
        ForgivingVoid.misc      = config.get("forgiving-void.misc", false);

        ForgivingVoid.animatePlayers  = config.get("forgiving-void.animate-players", true);
        ForgivingVoid.animateHostile  = config.get("forgiving-void.animate-hostile", false);
        ForgivingVoid.animateVillager = config.get("forgiving-void.animate-villager", false);
        ForgivingVoid.animateLivings  = config.get("forgiving-void.animate-livings", false);

        ForgivingVoid.offset = config.get("forgiving-void.y-offset", 1.0);

        ForgivingVoid.exceptions.clear();
        List<String> exceptions = config.get("forgiving-void.exceptions", List.of());
        ForgivingVoid.exceptions.addAll(exceptions);

        ForgivingVoid.playersDamager = castDamager(config.get("forgiving-void.damage-players", "MAX"));
        ForgivingVoid.hostileDamager = castDamager(config.get("forgiving-void.damage-hostile", 4.0));
        ForgivingVoid.livingsDamager = castDamager(config.get("forgiving-void.damage-livings", 0.0));

        IConfig potions = config.getSection("forgiving-void.potions");
        if (potions != null) for (String key: potions.valueMap().keySet()) {
            double time = potions.getInt(key);
            Optional<MobEffect> effect = MinecraftUtil.getMobEffect(key.replace('-', ':'));
            if (time > 0 && effect.isPresent()) effects.put(effect.get(), (int) (time * 20));
        }

        NeoBlock.LOGGER.debug("ForgivingVoid: Config loaded.");
    }

    private static boolean shallBeRescued(Entity entity) {
        Optional<ResourceLocation> location = MinecraftUtil.getEntityTypeResource(entity.getType());
        if (location.map(Objects::toString).map(exceptions::contains).orElse(false)) return true;

        if (entity instanceof Player) return ForgivingVoid.players;
        if (entity instanceof LivingEntity living) {
            if (living instanceof Monster) return ForgivingVoid.hostile;
            if (living instanceof Villager || living instanceof WanderingTrader)
                return ForgivingVoid.villager;

            return ForgivingVoid.livings;
        }
        if (entity instanceof ItemEntity) return ForgivingVoid.items;
        return ForgivingVoid.misc;
    }
    private static float getDamage(LivingEntity entity) {
        if (entity instanceof Player) return playersDamager.apply(entity);
        if (entity instanceof Monster) return hostileDamager.apply(entity);

        return livingsDamager.apply(entity);
    }
    private static boolean animate(ServerLevel level, LivingEntity entity) {
        if (entity instanceof Player) return animatePlayers && animate(level, entity.position());
        if (entity instanceof Monster) return animateHostile && animate(level, entity.position());
        if (entity instanceof Villager || entity instanceof WanderingTrader)
            return animateVillager && animate(level, entity.position());

        return animateLivings && animate(level, entity.position());
    }
    private static boolean animate(ServerLevel level, Vec3 pos) {
        LightningBolt lightning = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        lightning.setVisualOnly(true);
        lightning.setPos(pos);

        return level.addFreshEntity(lightning);
    }
    private static void addEffects(Player player) {
        for (Map.Entry<MobEffect, Integer> entry : effects.entrySet()) {
            MobEffectInstance effect = MinecraftUtil.getMobEffectInstance(entry.getKey(), entry.getValue());
            player.addEffect(effect);
        }
    }

    public static boolean handleVoid(ServerLevel level, Entity entity) {
        if (!shallBeRescued(entity)) return false;

        BlockPos pos = BlockManager.getBlockPos();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        entity.teleportTo(pos.getX() + 0.5, y + offset + 0.5, pos.getZ() + 0.5);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.fallDistance = 0;

        if (entity instanceof LivingEntity living) {
            living.hurt(living.damageSources().fall(), getDamage(living));
            ForgivingVoid.animate(level, living);
            if (living instanceof Player player) addEffects(player);
        }

        return true;
    }
}
