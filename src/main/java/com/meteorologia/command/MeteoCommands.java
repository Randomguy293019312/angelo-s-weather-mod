package com.meteorologia.command;

import com.meteorologia.Meteorologia;
import com.meteorologia.weather.Compass;
import com.meteorologia.weather.StormManager;
import com.meteorologia.weather.StormWorld;
import com.meteorologia.weather.Supercell;
import com.meteorologia.weather.WindModel;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /meteo tornado criar [ef] | tornado limpar
 * /meteo supercelula criar [distancia] | supercelula limpar
 * /meteo chuva leve|moderada|forte|limpar
 * /meteo tempestade iniciar|parar
 * /meteo vento | /meteo info
 */
@EventBusSubscriber(modid = Meteorologia.MODID)
public final class MeteoCommands {
    private MeteoCommands() {}

    @SubscribeEvent
    public static void onRegister(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralCommandNode<CommandSourceStack> root = dispatcher.register(Commands.literal("meteo")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("tornado")
                        .then(Commands.literal("criar")
                                .executes(ctx -> spawnTornado(ctx.getSource(), 2))
                                .then(Commands.argument("ef", IntegerArgumentType.integer(0, 5))
                                        .executes(ctx -> spawnTornado(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "ef")))))
                        .then(Commands.literal("limpar")
                                .executes(ctx -> clearTornadoes(ctx.getSource()))))
                .then(Commands.literal("supercelula")
                        .then(Commands.literal("criar")
                                .executes(ctx -> spawnSupercell(ctx.getSource(), 1500))
                                .then(Commands.argument("distancia", IntegerArgumentType.integer(200, 6000))
                                        .executes(ctx -> spawnSupercell(ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "distancia")))))
                        .then(Commands.literal("limpar")
                                .executes(ctx -> clearAll(ctx.getSource()))))
                .then(Commands.literal("chuva")
                        .then(Commands.literal("leve").executes(ctx -> rain(ctx.getSource(), 0.35F, false, "leve")))
                        .then(Commands.literal("moderada").executes(ctx -> rain(ctx.getSource(), 0.65F, false, "moderada")))
                        .then(Commands.literal("forte").executes(ctx -> rain(ctx.getSource(), 1.0F, true, "forte")))
                        .then(Commands.literal("limpar").executes(ctx -> rain(ctx.getSource(), 0F, false, "limpa"))))
                .then(Commands.literal("tempestade")
                        .then(Commands.literal("iniciar").executes(ctx -> startStorm(ctx.getSource())))
                        .then(Commands.literal("parar").executes(ctx -> stopStorm(ctx.getSource()))))
                .then(Commands.literal("vento").executes(ctx -> wind(ctx.getSource())))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource()))));

        dispatcher.register(Commands.literal("meteorologia")
                .requires(src -> src.hasPermission(2))
                .redirect(root));
    }

    private static void say(CommandSourceStack src, String msg) {
        src.sendSuccess(() -> Component.literal(msg), true);
    }

    private static int spawnTornado(CommandSourceStack src, int ef) {
        ServerLevel level = src.getLevel();
        Vec3 pos = src.getPosition();
        Vec3 look = Vec3.directionFromRotation(src.getRotation());
        double x = pos.x + look.x * 80.0;
        double z = pos.z + look.z * 80.0;
        StormManager.spawnTornado(level, x, z, ef);
        say(src, "Tornado EF" + ef + " criado a ~80 blocos de voce.");
        return 1;
    }

    private static int clearTornadoes(CommandSourceStack src) {
        StormWorld w = StormManager.get(src.getLevel());
        int n = w.tornadoes.size();
        w.tornadoes.clear();
        say(src, n + " tornado(s) removido(s).");
        return n;
    }

    private static int spawnSupercell(CommandSourceStack src, int distance) {
        ServerLevel level = src.getLevel();
        StormWorld w = StormManager.get(level);
        Vec3 pos = src.getPosition();
        Supercell s = StormManager.spawnApproaching(level, w, pos.x, pos.z, distance, 3);
        say(src, "Supercelula criada a " + Compass.distance(distance) + ", vindo do "
                + Compass.direction(s.x - pos.x, s.z - pos.z) + " (potencial de tornado EF3).");
        return 1;
    }

    private static int clearAll(CommandSourceStack src) {
        StormManager.clearAll(src.getLevel());
        say(src, "Todas as supercelulas e tornados foram removidos.");
        return 1;
    }

    private static int rain(CommandSourceStack src, float intensity, boolean thunder, String name) {
        StormManager.setRain(src.getLevel(), intensity, thunder);
        say(src, "Chuva " + name + ".");
        return 1;
    }

    private static int startStorm(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        StormManager.setRain(level, 1.0F, true);
        Vec3 pos = src.getPosition();
        StormManager.spawnApproaching(level, StormManager.get(level), pos.x, pos.z, 1800, 3);
        say(src, "Tempestade iniciada: chuva forte e supercelula se aproximando.");
        return 1;
    }

    private static int stopStorm(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        StormManager.clearAll(level);
        StormManager.setRain(level, 0F, false);
        say(src, "Tempestade encerrada.");
        return 1;
    }

    private static int wind(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        Vec3 p = src.getPosition();
        double[] w = WindModel.wind(StormManager.get(level), level.getGameTime(), p.x, p.y, p.z);
        say(src, "Vento aqui: " + WindModel.describeWind(w));
        return 1;
    }

    private static int info(CommandSourceStack src) {
        Vec3 p = src.getPosition();
        for (String line : StormManager.report(src.getLevel(), p.x, p.z)) {
            src.sendSuccess(() -> Component.literal(line), false);
        }
        return 1;
    }
}
