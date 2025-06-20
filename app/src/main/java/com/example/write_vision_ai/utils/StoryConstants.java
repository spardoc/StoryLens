package com.example.write_vision_ai.utils;


public class StoryConstants {
    public static final String[][] options = {
            {"un niño", "una niña", "un perro", "un gato"},            // Personaje principal
            {"una flor mágica", "una piedra brillante", "una hoja gigante"}, // Objeto encontrado
            {"un camino dorado", "una huella misteriosa", "un mapa antiguo"}, // Camino a seguir
            {"un árbol parlante", "una cueva secreta", "un castillo en miniatura"}, // Lugar
            {"una ardilla sabia", "un búho cantante", "un ratón inventor"},   // Personaje secundario
            {"una bicicleta voladora", "un paraguas que habla", "un robot de hojas"}, // Invento
            {"una nube gigante", "un monstruo de chocolate", "una sombra traviesa"}, // Antagonista
            {"una fiesta de globos", "una danza mágica", "un picnic de estrellas"}  // Final
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