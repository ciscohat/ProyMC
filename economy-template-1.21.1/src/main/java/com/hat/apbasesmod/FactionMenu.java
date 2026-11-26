package com.hat.apbasesmod;

import java.util.UUID;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Menú de facciones (cofre de 6 filas).
 * - Si el jugador tiene facción: muestra sus datos, sus miembros y un botón para salir o disolverla.
 * - Si no tiene: muestra cómo crear una y la lista de facciones que existen.
 * Un menú de cofre no puede pedir texto, por eso crear e invitar se hacen con comandos.
 */
public class FactionMenu extends ChestMenu {
    private static final int ROWS = 6;
    private static final int SIZE = ROWS * 9;

    private static final int SLOT_INFO = 4;
    private static final int SLOT_BACK = 45;
    private static final int SLOT_ACTION = 49;
    private static final int BODY_START = 18;
    private static final int BODY_END = 44; // inclusive

    private FactionMenu(int containerId, Inventory playerInventory, SimpleContainer container) {
        super(MenuType.GENERIC_9x6, containerId, playerInventory, container, ROWS);
    }

    public static void open(ServerPlayer player) {
        SimpleContainer contents = buildContents(player);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new FactionMenu(id, inventory, contents),
                Component.literal("Facciones")));
    }

    private static SimpleContainer buildContents(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        FactionsData data = FactionsData.get(server);
        Faction mine = data.getFactionOf(player.getUUID());

        SimpleContainer container = new SimpleContainer(SIZE);
        MenuItems.fill(container);

        container.setItem(SLOT_BACK, MenuItems.icon(Items.ARROW, "Volver", ChatFormatting.WHITE,
                "Regresar al menú principal"));

        if (mine != null) {
            container.setItem(SLOT_INFO, MenuItems.icon(Items.WHITE_BANNER, "Facción: " + mine.getName(),
                    ChatFormatting.GREEN,
                    "Dueño: " + nameOf(server, mine.getOwner()),
                    "Miembros: " + mine.getMembers().size()));

            // Primero el dueño, después el resto.
            int slot = BODY_START;
            container.setItem(slot++, MenuItems.icon(Items.PLAYER_HEAD, nameOf(server, mine.getOwner()),
                    ChatFormatting.GOLD, "Dueño"));
            for (UUID member : mine.getMembers()) {
                if (slot > BODY_END) {
                    break;
                }
                if (!member.equals(mine.getOwner())) {
                    container.setItem(slot++, MenuItems.icon(Items.PLAYER_HEAD, nameOf(server, member),
                            ChatFormatting.WHITE, "Miembro"));
                }
            }

            boolean isOwner = mine.getOwner().equals(player.getUUID());
            if (isOwner) {
                container.setItem(SLOT_ACTION, MenuItems.icon(Items.TNT, "Disolver facción", ChatFormatting.RED,
                        "Shift + clic para confirmar.",
                        "Se borra para todos los miembros."));
            } else {
                container.setItem(SLOT_ACTION, MenuItems.icon(Items.OAK_DOOR, "Salir de la facción",
                        ChatFormatting.RED, "Clic para salir."));
            }
        } else {
            container.setItem(SLOT_INFO, MenuItems.icon(Items.GRAY_BANNER, "Sin facción", ChatFormatting.GRAY,
                    "Creá una con /faction create <nombre>",
                    data.hasInvite(player.getUUID())
                            ? "Tenés una invitación: usá /faction accept"
                            : "O pedile al dueño de una que te invite."));

            if (data.all().isEmpty()) {
                container.setItem(22, MenuItems.icon(Items.BARRIER, "Todavía no hay facciones",
                        ChatFormatting.GRAY, "Sé el primero en crear una."));
            }
            int slot = BODY_START;
            for (Faction f : data.all()) {
                if (slot > BODY_END) {
                    break;
                }
                container.setItem(slot++, MenuItems.icon(Items.WHITE_BANNER, f.getName(), ChatFormatting.GREEN,
                        "Dueño: " + nameOf(server, f.getOwner()),
                        "Miembros: " + f.getMembers().size()));
            }
        }
        return container;
    }

    /** Nombre del jugador: si está conectado se lee directo; si no, de la caché del servidor. */
    private static String nameOf(MinecraftServer server, UUID uuid) {
        ServerPlayer online = server.getPlayerList().getPlayer(uuid);
        if (online != null) {
            return online.getName().getString();
        }
        var cache = server.getProfileCache();
        if (cache != null) {
            return cache.get(uuid).map(profile -> profile.getName()).orElse(uuid.toString().substring(0, 8));
        }
        return uuid.toString().substring(0, 8);
    }

    // --- Clics ---

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // PICKUP = clic normal; QUICK_MOVE = shift + clic (se usa para confirmar disolver).
        if ((clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)
                && slotId >= 0 && slotId < SIZE
                && player instanceof ServerPlayer serverPlayer) {
            onButton(slotId, clickType, serverPlayer);
        }
        if (player.containerMenu == this) {
            sendAllDataToRemote();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void onButton(int slot, ClickType clickType, ServerPlayer player) {
        switch (slot) {
            case SLOT_BACK -> InfoMenu.open(player); // vuelve al menú principal
            case SLOT_ACTION -> {
                Faction mine = FactionsData.get(player.getServer()).getFactionOf(player.getUUID());
                if (mine == null) {
                    return;
                }
                if (mine.getOwner().equals(player.getUUID())) {
                    if (clickType != ClickType.QUICK_MOVE) {
                        player.sendSystemMessage(Component.literal(
                                "Para disolver la facción mantené Shift y hacé clic."));
                        return;
                    }
                    FactionCommands.disband(player);
                } else {
                    FactionCommands.leave(player);
                }
                open(player); // vuelve a abrir el menú ya actualizado
            }
        }
    }
}
