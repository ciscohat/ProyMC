package com.hat.apbasesmod;

import java.util.UUID;
import java.util.regex.Pattern;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = APBasesMod.MODID)
public class FactionCommands {
    // Letras, números y guion bajo; entre 3 y 16 caracteres.
    private static final Pattern NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // FactionCommands::create es una "referencia a método": equivale a pasar el método como delegado en C#.
        dispatcher.register(Commands.literal("faction")
                .executes(FactionCommands::openMenu)
                .then(Commands.literal("menu").executes(FactionCommands::openMenu))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .executes(FactionCommands::create)))
                .then(Commands.literal("invite")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(FactionCommands::invite)))
                .then(Commands.literal("accept").executes(FactionCommands::accept))
                .then(Commands.literal("leave").executes(ctx -> {
                    leave(ctx.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("disband").executes(ctx -> {
                    disband(ctx.getSource().getPlayerOrException());
                    return 1;
                })));
    }

    private static int openMenu(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        FactionMenu.open(ctx.getSource().getPlayerOrException());
        return 1;
    }

    private static int create(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = StringArgumentType.getString(ctx, "name");
        FactionsData data = FactionsData.get(ctx.getSource().getServer());

        if (!NAME_PATTERN.matcher(name).matches()) {
            ctx.getSource().sendFailure(Component.literal(
                    "El nombre debe tener entre 3 y 16 caracteres (letras, números o _)."));
            return 0;
        }
        if (data.getFactionOf(player.getUUID()) != null) {
            ctx.getSource().sendFailure(Component.literal("Ya pertenecés a una facción."));
            return 0;
        }
        if (data.nameTaken(name)) {
            ctx.getSource().sendFailure(Component.literal("Ese nombre ya está en uso."));
            return 0;
        }
        data.create(name, player.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("Creaste la facción " + name + "."), false);
        return 1;
    }

    private static int invite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ServerPlayer target = EntityArgument.getPlayer(ctx, "target");
        FactionsData data = FactionsData.get(ctx.getSource().getServer());
        Faction mine = data.getFactionOf(player.getUUID());

        if (mine == null) {
            ctx.getSource().sendFailure(Component.literal("No pertenecés a ninguna facción."));
            return 0;
        }
        if (!mine.getOwner().equals(player.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("Solo el dueño puede invitar."));
            return 0;
        }
        if (target.getUUID().equals(player.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("No podés invitarte a vos mismo."));
            return 0;
        }
        if (data.getFactionOf(target.getUUID()) != null) {
            ctx.getSource().sendFailure(Component.literal("Ese jugador ya pertenece a una facción."));
            return 0;
        }

        data.invite(target.getUUID(), mine.getId());
        ctx.getSource().sendSuccess(
                () -> Component.literal("Invitaste a " + target.getName().getString() + "."), false);
        target.sendSystemMessage(Component.literal(player.getName().getString()
                + " te invitó a la facción " + mine.getName() + ". Usá /faction accept para unirte."));
        return 1;
    }

    private static int accept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        FactionsData data = FactionsData.get(ctx.getSource().getServer());

        Faction joined = data.acceptInvite(player.getUUID());
        if (joined == null) {
            ctx.getSource().sendFailure(Component.literal("No tenés invitaciones pendientes."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Te uniste a la facción " + joined.getName() + "."), false);
        notifyMembers(ctx.getSource().getServer(), joined, player.getName().getString() + " se unió a la facción.", player.getUUID());
        return 1;
    }

    // Públicos porque también los usa FactionMenu.

    public static void leave(ServerPlayer player) {
        FactionsData data = FactionsData.get(player.getServer());
        Faction mine = data.getFactionOf(player.getUUID());

        if (mine == null) {
            player.sendSystemMessage(Component.literal("No pertenecés a ninguna facción."));
            return;
        }
        if (mine.getOwner().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("Sos el dueño: usá /faction disband para disolver la facción."));
            return;
        }
        data.leave(player.getUUID());
        player.sendSystemMessage(Component.literal("Saliste de la facción " + mine.getName() + "."));
        notifyMembers(player.getServer(), mine, player.getName().getString() + " salió de la facción.", player.getUUID());
    }

    public static void disband(ServerPlayer player) {
        FactionsData data = FactionsData.get(player.getServer());
        Faction removed = data.disband(player.getUUID());

        if (removed == null) {
            player.sendSystemMessage(Component.literal("No sos dueño de ninguna facción."));
            return;
        }
        // La facción ya no está en los datos, pero el objeto todavía recuerda a sus miembros.
        notifyMembers(player.getServer(), removed, "La facción " + removed.getName() + " fue disuelta.", null);
    }

    /** Manda un mensaje a los miembros conectados (menos a 'except', si se indica). */
    private static void notifyMembers(MinecraftServer server, Faction faction, String message, UUID except) {
        for (UUID member : faction.getMembers()) {
            if (member.equals(except)) {
                continue;
            }
            ServerPlayer online = server.getPlayerList().getPlayer(member);
            if (online != null) {
                online.sendSystemMessage(Component.literal(message));
            }
        }
    }
}
