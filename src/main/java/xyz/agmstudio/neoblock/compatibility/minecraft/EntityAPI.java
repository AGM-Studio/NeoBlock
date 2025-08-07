package xyz.agmstudio.neoblock.compatibility.minecraft;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

public final class EntityAPI {
    public static void leash(Entity mob, Entity to) {
        if (mob instanceof Mob leashable) leashable.setLeashedTo(to, true);
    }
}
