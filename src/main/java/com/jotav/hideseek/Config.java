package com.jotav.hideseek;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    // Configurações de tempo do jogo
    public static final ModConfigSpec.IntValue HIDE_TIME = BUILDER
            .comment("Tempo em segundos para a fase de esconder")
            .defineInRange("hideTime", 60, 10, 600);
    
    public static final ModConfigSpec.IntValue SEEK_TIME = BUILDER
            .comment("Tempo em segundos para a fase de busca")
            .defineInRange("seekTime", 300, 30, 1200);
    
    public static final ModConfigSpec.IntValue STARTING_COUNTDOWN = BUILDER
            .comment("Tempo de contagem regressiva antes do início (segundos)")
            .defineInRange("startingCountdown", 10, 3, 30);
    
    // Configurações de jogadores
    public static final ModConfigSpec.IntValue MIN_HIDERS = BUILDER
            .comment("Número mínimo de Hiders")
            .defineInRange("minHiders", 1, 1, 50);
    
    public static final ModConfigSpec.IntValue MAX_HIDERS = BUILDER
            .comment("Número máximo de Hiders")
            .defineInRange("maxHiders", 10, 1, 50);
    
    public static final ModConfigSpec.IntValue MIN_PLAYERS = BUILDER
            .comment("Número mínimo de jogadores para iniciar")
            .defineInRange("minPlayers", 2, 2, 100);
    
    // Coordenadas dos pontos de spawn (formato: "x,y,z,dimension")
    public static final ModConfigSpec.ConfigValue<String> LOBBY_SPAWN = BUILDER
            .comment("Coordenadas do spawn do lobby (formato: x,y,z,dimension)")
            .define("lobbySpawn", "");
    
    public static final ModConfigSpec.ConfigValue<String> SEEKER_SPAWN = BUILDER
            .comment("Coordenadas do spawn dos Seekers (formato: x,y,z,dimension)")
            .define("seekerSpawn", "");
    
    // Limites do mapa
    public static final ModConfigSpec.ConfigValue<String> MAP_BOUNDARY_MIN = BUILDER
            .comment("Coordenadas mínimas do mapa (formato: x,y,z)")
            .define("mapBoundaryMin", "");
    
    public static final ModConfigSpec.ConfigValue<String> MAP_BOUNDARY_MAX = BUILDER
            .comment("Coordenadas máximas do mapa (formato: x,y,z)")
            .define("mapBoundaryMax", "");
    
    // Configurações de efeitos
    public static final ModConfigSpec.IntValue SEEKER_SLOWNESS_LEVEL = BUILDER
            .comment("Nível do efeito de lentidão aplicado aos Seekers (255 = imobilização total)")
            .defineInRange("seekerSlownessLevel", 255, 1, 255);
    
    public static final ModConfigSpec.IntValue SEEKER_BLINDNESS_LEVEL = BUILDER
            .comment("Nível do efeito de cegueira aplicado aos Seekers")
            .defineInRange("seekerBlindnessLevel", 1, 1, 255);
    
    // Configurações de exemplo (manter para compatibilidade)
    public static final ModConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    public static final ModConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ModConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), () -> "", Config::validateItemName);

    static final ModConfigSpec SPEC = BUILDER.build();

    private static boolean validateItemName(final Object obj) {
        return obj instanceof String itemName && BuiltInRegistries.ITEM.containsKey(ResourceLocation.parse(itemName));
    }
}
