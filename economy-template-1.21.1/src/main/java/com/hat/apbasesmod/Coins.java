package com.hat.apbasesmod;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/* Valores de las monedas y utilidades para mover monedas entre inventario y banco. */
public final class Coins {
    public static final long COPPER_VALUE = 1;
    public static final long SILVER_VALUE = 32;
    public static final long GOLD_VALUE = 64;

    // Tope por retiro, para no llenar el inventario ni el suelo de stacks.
    public static final long MAX_WITHDRAW = 1_000_000;

    private Coins() {} // clase de utilidades: no se instancia (como una static class de C#)

    /* Valor de UNA moneda de este stack (0 si no es una moneda). */
    public static long unitValue(ItemStack stack) {
        Item item = stack.getItem();
        if (item == ModItems.COPPER_COIN.get()) return COPPER_VALUE;
        if (item == ModItems.SILVER_COIN.get()) return SILVER_VALUE;
        if (item == ModItems.GOLD_COIN.get()) return GOLD_VALUE;
        return 0;
    }

    /* Quita todas las monedas del inventario y devuelve su valor total. */
    public static long takeAllCoins(ServerPlayer player) {
        Inventory inv = player.getInventory();
        long total = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            long unit = unitValue(stack);
            if (unit > 0) {
                total += unit * stack.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        return total;
    }

    /* Entrega el monto en monedas, usando la menor cantidad posible de cada tipo. */
    public static void giveCoins(ServerPlayer player, long amount) {
        long gold = amount / GOLD_VALUE;
        long remainder = amount % GOLD_VALUE;
        long silver = remainder / SILVER_VALUE;
        long copper = remainder % SILVER_VALUE;

        give(player, ModItems.GOLD_COIN.get(), gold);
        give(player, ModItems.SILVER_COIN.get(), silver);
        give(player, ModItems.COPPER_COIN.get(), copper);
    }

    private static void give(ServerPlayer player, Item item, long count) {
        int maxStack = new ItemStack(item).getMaxStackSize();
        while (count > 0) {
            int n = (int) Math.min(count, maxStack);
            // Lo mete al inventario; si no hay lugar, lo tira al suelo junto al jugador.
            ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(item, n));
            count -= n;
        }
    }
}
