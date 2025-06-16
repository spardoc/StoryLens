package com.example.write_vision_ai.utils;

public class StoryConstants {
    public static final String[][] options = {
            {"un niño", "una niña", "un perro", "un gato"},
            {"una flor mágica", "una piedra brillante", "una hoja gigante"},
            {"un camino dorado", "una huella misteriosa", "un mapa antiguo"},
            {"un árbol parlante", "una cueva secreta", "un castillo en miniatura"},
            {"una ardilla sabia", "un búho cantante", "un ratón inventor"},
            {"una bicicleta voladora", "un paraguas que habla", "un robot de hojas"},
            {"una nube gigante", "un monstruo de chocolate", "una sombra traviesa"},
            {"una fiesta de globos", "una danza mágica", "un picnic de estrellas"}
    };

    public static final String[] basePrompts = {
            "%s salió a pasear por el parque.",
            "Encontró %s en el suelo.",
            "Decidió seguir %s.",
            "Llegó a %s.",
            "Allí conoció a %s.",
            "Juntos inventaron %s.",
            "Pero apareció %s.",
            "Al final, todos celebraron con %s."
    };
}
