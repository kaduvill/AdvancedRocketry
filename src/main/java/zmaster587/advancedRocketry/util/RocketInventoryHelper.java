package zmaster587.advancedRocketry.util;

import com.google.common.collect.MapMaker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public class RocketInventoryHelper {

    // Weak keys keep membership tied to the player instance and let collected players expire
    private static final Set<EntityPlayer> inventoryCheckPlayerBypassMap = Collections.newSetFromMap(new MapMaker().weakKeys().makeMap());
    private static WeakHashMap<EntityPlayer, Long> inventoryTimingMap = new WeakHashMap<>();
    private static WeakHashMap<EntityPlayer, BlockPos> inventoryDismapping = new WeakHashMap<>();

    // TODO: Scope the bypass to the active rocket-backed container, including nested inventories.
    public static boolean allowAccess(Object tile) {
        EntityPlayer player = (EntityPlayer) tile;

        //If a small amount of time is passed since interfacing with the rocket and the player has moved then assume the player is no longer accessing the rocket
        //and possibly trying to abuse AR to circumvent inv checks
        if (inventoryTimingMap.containsKey(player)) {
            if (inventoryTimingMap.get(player) + 10 < player.world.getTotalWorldTime() &&
                    inventoryDismapping.get(player).getDistance(player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ()) > 3)
                removePlayerFromInventoryBypass(player);
        }
        return !canPlayerBypassInvChecks((EntityPlayer) tile);
    }

    public static boolean canPlayerBypassInvChecks(EntityPlayer player) {
        return inventoryCheckPlayerBypassMap.contains(player);
    }

    public static void removePlayerFromInventoryBypass(EntityPlayer player) {
        inventoryCheckPlayerBypassMap.remove(player);
    }

    public static void addPlayerToInventoryBypass(EntityPlayer player) {
        inventoryCheckPlayerBypassMap.add(player);
    }

    public static void updateTime(EntityPlayer entity, long worldTime) {
        inventoryTimingMap.put(entity, worldTime);
        inventoryDismapping.put(entity, entity.getPosition());
    }
}
