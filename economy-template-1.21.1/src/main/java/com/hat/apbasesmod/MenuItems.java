package com.hat.apbasesmod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/** Utilidades compartidas por todos los menús: crear ítems con nombre/descripción y rellenar el fondo. */
public final class MenuItems {
    private MenuItems() {}

    public static ItemStack icon(Item item, String name, ChatFormatting color, String... loreLines) {
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
    public static Component plain(String text, ChatFormatting color) {
        return Component.literal(text).setStyle(Style.EMPTY.withItalic(false).withColor(color));
    }

    /** Rellena todo el contenedor con paneles de vidrio gris sin nombre. */
    public static void fill(SimpleContainer container) {
        ItemStack filler = icon(Items.GRAY_STAINED_GLASS_PANE, " ", ChatFormatting.GRAY);
        for (int i = 0; i < container.getContainerSize(); i++) {
            container.setItem(i, filler.copy());
        }
    }
}
