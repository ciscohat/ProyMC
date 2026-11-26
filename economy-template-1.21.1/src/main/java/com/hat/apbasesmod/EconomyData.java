package com.hat.apbasesmod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Saldos bancarios de todos los jugadores. Se guarda dentro del mundo,
 * así que sobrevive a reinicios del servidor.
 * Equivalente a un Dictionary<Guid, long> persistido.
 */
public class EconomyData extends SavedData {
    private static final String NAME = APBasesMod.MODID + "_economy";

    private final Map<UUID, Long> balances = new HashMap<>();

    // Punto de acceso: siempre se guarda en el overworld, así hay una sola copia por servidor.
    public static EconomyData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(EconomyData::new, EconomyData::load, null), NAME);
    }

    public long getBalance(UUID player) {
        return balances.getOrDefault(player, 0L);
    }

    public void add(UUID player, long amount) {
        balances.merge(player, amount, Long::sum);
        setDirty(); // avisa al juego que hay que escribir a disco
    }

    /** Resta si hay saldo suficiente. Devuelve false si no alcanza. */
    public boolean remove(UUID player, long amount) {
        long current = getBalance(player);
        if (current < amount) {
            return false;
        }
        balances.put(player, current - amount);
        setDirty();
        return true;
    }

    public boolean transfer(UUID from, UUID to, long amount) {
        if (!remove(from, amount)) {
            return false;
        }
        add(to, amount);
        return true;
    }

    // --- Persistencia ---

    public static EconomyData load(CompoundTag tag, HolderLookup.Provider registries) {
        EconomyData data = new EconomyData();
        for (String key : tag.getAllKeys()) {
            data.balances.put(UUID.fromString(key), tag.getLong(key));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        balances.forEach((uuid, amount) -> tag.putLong(uuid.toString(), amount));
        return tag;
    }
}
