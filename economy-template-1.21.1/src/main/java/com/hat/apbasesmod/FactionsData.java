package com.hat.apbasesmod;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Todas las facciones del servidor, guardadas dentro del mundo. */
public class FactionsData extends SavedData {
    private static final String NAME = APBasesMod.MODID + "_factions";

    private final Map<Long, Faction> factions = new HashMap<>();      // id -> facción
    private final Map<UUID, Long> playerFaction = new HashMap<>();    // jugador -> id de su facción
    private long nextId = 1;

    // Invitaciones pendientes (jugador invitado -> id de facción).
    // No se guardan en disco: se pierden al reiniciar el servidor, y está bien.
    private final Map<UUID, Long> invites = new HashMap<>();

    public static FactionsData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(FactionsData::new, FactionsData::load, null), NAME);
    }

    // --- Consultas ---

    /** Devuelve la facción del jugador, o null si no tiene (como un Faction? de C#). */
    public Faction getFactionOf(UUID player) {
        Long id = playerFaction.get(player);
        return id == null ? null : factions.get(id);
    }

    public boolean nameTaken(String name) {
        return factions.values().stream().anyMatch(f -> f.getName().equalsIgnoreCase(name));
    }

    /** Todas las facciones, solo lectura. */
    public Collection<Faction> all() {
        return Collections.unmodifiableCollection(factions.values());
    }

    public boolean hasInvite(UUID player) {
        return invites.containsKey(player);
    }

    // --- Cambios ---

    /** Crea una facción. Devuelve null si el jugador ya tiene una o el nombre está usado. */
    public Faction create(String name, UUID owner) {
        if (playerFaction.containsKey(owner) || nameTaken(name)) {
            return null;
        }
        Faction faction = new Faction(nextId++, name, owner);
        factions.put(faction.getId(), faction);
        playerFaction.put(owner, faction.getId());
        setDirty();
        return faction;
    }

    public void invite(UUID invitee, long factionId) {
        invites.put(invitee, factionId);
    }

    /** Acepta la invitación pendiente. Devuelve la facción, o null si no hay invitación válida. */
    public Faction acceptInvite(UUID player) {
        Long factionId = invites.remove(player);
        if (factionId == null || playerFaction.containsKey(player)) {
            return null;
        }
        Faction faction = factions.get(factionId);
        if (faction == null) { // la facción se disolvió mientras tanto
            return null;
        }
        faction.addMember(player);
        playerFaction.put(player, faction.getId());
        setDirty();
        return faction;
    }

    /** Un miembro sale de su facción. El dueño no puede salir: tiene que disolverla. */
    public boolean leave(UUID player) {
        Faction faction = getFactionOf(player);
        if (faction == null || faction.getOwner().equals(player)) {
            return false;
        }
        faction.removeMember(player);
        playerFaction.remove(player);
        setDirty();
        return true;
    }

    /** El dueño disuelve su facción. Devuelve la facción borrada, o null si no era dueño. */
    public Faction disband(UUID owner) {
        Faction faction = getFactionOf(owner);
        if (faction == null || !faction.getOwner().equals(owner)) {
            return null;
        }
        for (UUID member : faction.getMembers()) {
            playerFaction.remove(member);
        }
        factions.remove(faction.getId());
        invites.values().removeIf(id -> id == faction.getId()); // anula invitaciones a esa facción
        setDirty();
        return faction;
    }

    // --- Persistencia ---

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putLong("nextId", nextId);
        ListTag list = new ListTag();
        for (Faction f : factions.values()) {
            CompoundTag ft = new CompoundTag();
            ft.putLong("id", f.getId());
            ft.putString("name", f.getName());
            ft.putUUID("owner", f.getOwner());

            ListTag members = new ListTag();
            for (UUID member : f.getMembers()) {
                CompoundTag mt = new CompoundTag();
                mt.putUUID("uuid", member);
                members.add(mt);
            }
            ft.put("members", members);
            list.add(ft);
        }
        tag.put("factions", list);
        return tag;
    }

    public static FactionsData load(CompoundTag tag, HolderLookup.Provider registries) {
        FactionsData data = new FactionsData();
        data.nextId = Math.max(1, tag.getLong("nextId"));

        ListTag list = tag.getList("factions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag ft = list.getCompound(i);
            Faction f = new Faction(ft.getLong("id"), ft.getString("name"), ft.getUUID("owner"));

            ListTag members = ft.getList("members", Tag.TAG_COMPOUND);
            for (int j = 0; j < members.size(); j++) {
                f.addMember(members.getCompound(j).getUUID("uuid"));
            }

            data.factions.put(f.getId(), f);
            // El índice jugador -> facción no se guarda: se reconstruye desde los miembros.
            for (UUID member : f.getMembers()) {
                data.playerFaction.put(member, f.getId());
            }
        }
        return data;
    }
}
