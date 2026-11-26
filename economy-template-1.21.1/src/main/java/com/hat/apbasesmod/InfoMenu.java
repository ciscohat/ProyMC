package com.hat.apbasesmod;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
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
 * Menú principal de información (cofre de 3 filas).
 * Desde acá se llega al menú de facciones.
 */
public class InfoMenu extends ChestMenu {
    private static final int ROWS = 3;

    private static final int SLOT_BANK = 11;
    private static final int SLOT_EVENTS = 13;
    private static final int SLOT_FACTIONS = 15;

    private InfoMenu(int containerId, Inventory playerInventory, SimpleContainer container) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, container, ROWS);
    }

    public static void open(ServerPlayer player) {
        SimpleContainer contents = buildContents(player);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new InfoMenu(id, inventory, contents),
                Component.literal("Menú de información")));
    }

    private static SimpleContainer buildContents(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(ROWS * 9);
        MenuItems.fill(container);

        long balance = EconomyData.get(player.getServer()).getBalance(player.getUUID());
        container.setItem(SLOT_BANK, MenuItems.icon(ModItems.GOLD_COIN.get(), "Banco", ChatFormatting.GOLD,
                "Saldo: " + balance,
                "",
                "Clic: ver tu saldo en el chat",
                "Comandos: /bank deposit, /bank withdraw"));

        container.setItem(SLOT_EVENTS, MenuItems.icon(Items.CLOCK, "Eventos", ChatFormatting.AQUA,
                "Todavía no hay un sistema de eventos.",
                "",
                "Próximamente"));

        Faction faction = FactionsData.get(player.getServer()).getFactionOf(player.getUUID());
        String factionLine = faction != null
                ? "Tu facción: " + faction.getName()
                : "No pertenecés a ninguna facción.";
        container.setItem(SLOT_FACTIONS, MenuItems.icon(Items.WHITE_BANNER, "Facciones", ChatFormatting.GREEN,
                factionLine,
                "",
                "Clic: abrir el menú de facciones"));

        return container;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (clickType == ClickType.PICKUP && slotId >= 0 && slotId < ROWS * 9
                && player instanceof ServerPlayer serverPlayer) {
            onButton(slotId, serverPlayer);
        }
        // Si el botón abrió otro menú, este ya no es el activo: no hay nada que resincronizar.
        if (player.containerMenu == this) {
            sendAllDataToRemote();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    private void onButton(int slot, ServerPlayer player) {
        switch (slot) {
            case SLOT_BANK -> {
                long balance = EconomyData.get(player.getServer()).getBalance(player.getUUID());
                player.sendSystemMessage(Component.literal("Saldo bancario: " + balance));
                player.closeContainer();
            }
            case SLOT_EVENTS -> player.sendSystemMessage(Component.literal("Todavía no hay eventos activos."));
            case SLOT_FACTIONS -> FactionMenu.open(player); // conexión con el menú de facciones
        }
    }
}
