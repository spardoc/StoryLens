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

    public static final String comicStyleJSONPrompt =
            "Create an image based on the following scene using this visual comic strip style in JSON format:\n" +
                    "{\n" +
                    "  \"styleAesthetic\": {\n" +
                    "    \"title\": \"Children’s Comic Book Illustration\",\n" +
                    "    \"overallVibe\": \"Energetic and fun, stylized like a comic strip with bold outlines and dynamic compositions\",\n" +
                    "    \"viewAngle\": \"Varied comic panel angles — mostly frontal or slightly dramatic perspectives\",\n" +
                    "    \"renderingStyle\": \"Hand-drawn comic ink style with flat colors and halftone textures\",\n" +
                    "    \"colorPalette\": {\n" +
                    "      \"baseTones\": [\"Primary colors\", \"High-contrast tones\"],\n" +
                    "      \"accents\": [\"Speech bubble whites\", \"Shadow blacks\"],\n" +
                    "      \"gradientStyle\": \"None or minimal; solid fills with occasional crosshatching\"\n" +
                    "    },\n" +
                    "    \"lightingAndShadows\": {\n" +
                    "      \"type\": \"Comic book-style spot lighting\",\n" +
                    "      \"shadowStyle\": \"Heavy inked shadows or bold crosshatching\",\n" +
                    "      \"highlightIntensity\": 0.4\n" +
                    "    },\n" +
                    "    \"characterFeatures\": {\n" +
                    "      \"facialExpressions\": \"Highly exaggerated expressions with thick outlines\",\n" +
                    "      \"eyeStyle\": \"Cartoon eyes with black pupils and expressive eyebrows\",\n" +
                    "      \"mouthStyle\": \"Wide, open shapes, sometimes with action lines\"\n" +
                    "    },\n" +
                    "    \"objectSurfaces\": {\n" +
                    "      \"type\": \"Flat with thick contour lines\",\n" +
                    "      \"textureDetail\": \"Minimal, mostly patterns like dots or hatching\"\n" +
                    "    },\n" +
                    "    \"linework\": {\n" +
                    "      \"thickness\": \"Varied line weights for dynamic comic feel\",\n" +
                    "      \"color\": \"Black ink-style\"\n" +
                    "    },\n" +
                    "    \"moodKeywords\": [\"Comic\", \"Energetic\", \"Fun\", \"Childlike\", \"Narrative\", \"Dynamic\"]\n" +
                    "  }\n" +
                    "}\n";
}