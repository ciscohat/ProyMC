package com.hat.apbasesmod;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = APBasesMod.MODID)
public class EconomyCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // /balance
        dispatcher.register(Commands.literal("balance")
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    long balance = EconomyData.get(ctx.getSource().getServer()).getBalance(player.getUUID());
                    ctx.getSource().sendSuccess(() -> Component.literal("Saldo bancario: " + balance), false);
                    return 1;
                }));

        // /bank deposit  -> mete TODAS las monedas del inventario al banco
        // /bank withdraw <monto> -> saca monedas del banco
        dispatcher.register(Commands.literal("bank")
                .then(Commands.literal("deposit")
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayerOrException();
                            EconomyData data = EconomyData.get(ctx.getSource().getServer());

                            long total = Coins.takeAllCoins(player);
                            if (total == 0) {
                                ctx.getSource().sendFailure(Component.literal("No tenés monedas en el inventario."));
                                return 0;
                            }
                            data.add(player.getUUID(), total);
                            long newBalance = data.getBalance(player.getUUID());
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("Depositaste " + total + ". Saldo: " + newBalance), false);
                            return 1;
                        }))
                .then(Commands.literal("withdraw")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1, Coins.MAX_WITHDRAW))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    EconomyData data = EconomyData.get(ctx.getSource().getServer());
                                    long amount = LongArgumentType.getLong(ctx, "amount");

                                    // Primero se descuenta del banco, después se entregan las monedas.
                                    if (!data.remove(player.getUUID(), amount)) {
                                        ctx.getSource().sendFailure(Component.literal("Saldo insuficiente."));
                                        return 0;
                                    }
                                    Coins.giveCoins(player, amount);
                                    long newBalance = data.getBalance(player.getUUID());
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Retiraste " + amount + ". Saldo: " + newBalance), false);
                                    return 1;
                                }))));

        // /pay <jugador> <monto>
        dispatcher.register(Commands.literal("pay")
                .then(Commands.argument("target", EntityArgument.player())
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer from = ctx.getSource().getPlayerOrException();
                                    ServerPlayer to = EntityArgument.getPlayer(ctx, "target");
                                    long amount = LongArgumentType.getLong(ctx, "amount");

                                    boolean ok = EconomyData.get(ctx.getSource().getServer())
                                            .transfer(from.getUUID(), to.getUUID(), amount);
                                    if (!ok) {
                                        ctx.getSource().sendFailure(Component.literal("Saldo insuficiente."));
                                        return 0;
                                    }
                                    ctx.getSource().sendSuccess(
                                            () -> Component.literal("Enviaste " + amount + " a " + to.getName().getString()), false);
                                    to.sendSystemMessage(
                                            Component.literal(from.getName().getString() + " te envió " + amount));
                                    return 1;
                                }))));

        // /economy give <jugador> <monto>  (solo operadores, para probar)
        dispatcher.register(Commands.literal("economy")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("give")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> {
                                            ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
                                            long amount = LongArgumentType.getLong(ctx, "amount");
                                            EconomyData.get(ctx.getSource().getServer()).add(target.getUUID(), amount);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Diste " + amount + " a " + target.getName().getString()), true);
                                            return 1;
                                        })))));
    }
}
