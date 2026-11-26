package com.hat.apbasesmod;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Una facción: id, nombre, dueño y miembros. Es un objeto de datos simple (como una clase POCO de C#). */
public class Faction {
    private final long id;
    private String name;
    private UUID owner;
    private final Set<UUID> members = new HashSet<>();

    public Faction(long id, String name, UUID owner) {
        this.id = id;
        this.name = name;
        this.owner = owner;
        this.members.add(owner); // el dueño siempre es miembro
    }

    // Java no tiene propiedades { get; set; }: se escriben los métodos a mano.
    public long getId() { return id; }
    public String getName() { return name; }
    public UUID getOwner() { return owner; }

    /** Vista de solo lectura: quien la reciba no puede modificar la lista por fuera. */
    public Set<UUID> getMembers() { return Collections.unmodifiableSet(members); }

    public void addMember(UUID player) { members.add(player); }
    public void removeMember(UUID player) { members.remove(player); }
}
