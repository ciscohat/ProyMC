package com.hat.apbasesmod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * Menú de información con aspecto de cofre (3 filas).
 * Es un ChestMenu normal de Minecraft: el servidor decide qué ítems "decorativos"
 * se muestran y qué pasa al hacer clic. No se puede sacar nada de adentro.
 */
public class InfoMenu extends ChestMenu {
    private static final int ROWS = 3;

    // Posición de cada botón (0 a 26: fila 1 = 0-8, fila 2 = 9-17, fila 3 = 18-26)
    private static final int SLOT_BANK = 11;
    private static final int SLOT_EVENTS = 13;
    private static final int SLOT_FACTIONS = 15;

    private InfoMenu(int containerId, Inventory playerInventory, SimpleContainer container) {
        super(MenuType.GENERIC_9x3, containerId, playerInventory, container, ROWS);
    }

    /** Abre el menú a un jugador. */
    public static void open(ServerPlayer player) {
        SimpleContainer contents = buildContents(player);
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new InfoMenu(id, inventory, contents),
                Component.literal("Menú de información")));
    }

    private static SimpleContainer buildContents(ServerPlayer player) {
        SimpleContainer container = new SimpleContainer(ROWS * 9);

        // Fondo: paneles de vidrio gris sin nombre
        ItemStack filler = icon(Items.GRAY_STAINED_GLASS_PANE, " ", ChatFormatting.GRAY);
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, filler.copy());
        }

        long balance = EconomyData.get(player.getServer()).getBalance(player.getUUID());

        container.setItem(SLOT_BANK, icon(ModItems.GOLD_COIN.get(), "Banco", ChatFormatting.GOLD,
                "Saldo: " + balance,
                "",
                "Clic: ver tu saldo en el chat",
                "Comandos: /bank deposit, /bank withdraw"));

        container.setItem(SLOT_EVENTS, icon(Items.CLOCK, "Eventos", ChatFormatting.AQUA,
                "Todavía no hay un sistema de eventos.",
                "",
                "Próximamente"));

        container.setItem(SLOT_FACTIONS, icon(Items.WHITE_BANNER, "Facciones", ChatFormatting.GREEN,
                "Todavía no hay un sistema de facciones.",
                "",
                "Próximamente"));

        return container;
    }

    // --- Qué pasa al hacer clic ---

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Solo reaccionamos a clic simple (izquierdo o derecho) sobre los slots del menú.
        if (clickType == ClickType.PICKUP && slotId >= 0 && slotId < ROWS * 9
                && player instanceof ServerPlayer serverPlayer) {
            onButton(slotId, serverPlayer);
        }
        // Todo lo demás (shift-clic, arrastrar, tirar, números) se ignora.
        // Esto le dice al cliente que vuelva a mostrar el estado real y evita que "mueva" ítems.
        sendAllDataToRemote();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // desactiva shift-clic
    }

    private void onButton(int slot, ServerPlayer player) {
        switch (slot) {
            case SLOT_BANK -> {
                long balance = EconomyData.get(player.getServer()).getBalance(player.getUUID());
                player.sendSystemMessage(Component.literal("Saldo bancario: " + balance));
                player.closeContainer();
            }
            case SLOT_EVENTS -> player.sendSystemMessage(Component.literal("Todavía no hay eventos activos."));
            case SLOT_FACTIONS -> player.sendSystemMessage(Component.literal("Las facciones llegarán pronto."));
        }
    }

    // --- Helpers para crear ítems con nombre y descripción ---

    private static ItemStack icon(Item item, String name, ChatFormatting color, String... loreLines) {
        ItemStack stack = new ItemStack(item);
        stack.set(DataComponents.CUSTOM_NAME, plain(name, color));
        if (loreLines.length > 0) {
            List<Component> lines = new ArrayList<>();
            for (String line : loreLines) {
                lines.add(plain(line, ChatFormatting.GRAY));
            }
            stack.set(DataComponents.LORE, new ItemLore(lines));
        }
        return stack;
    }

    // Minecraft pone en cursiva los nombres personalizados; se desactiva a mano.
    private static Component plain(String text, ChatFormatting color) {
        return Component.literal(text).setStyle(Style.EMPTY.withItalic(false).withColor(color));
    }
}
