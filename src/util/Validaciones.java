package util;

import java.util.Locale;

public final class Validaciones {
    private Validaciones() {
    }

    public static String formatearTexto(String texto) {
        String normalizado = texto.trim().toLowerCase(Locale.ROOT);
        if (normalizado.isEmpty()) {
            return normalizado;
        }

        StringBuilder resultado = new StringBuilder();
        for (String palabra : normalizado.split("\\s+")) {
            resultado.append(palabra.substring(0, 1).toUpperCase(Locale.ROOT))
                    .append(palabra.substring(1))
                    .append(' ');
        }
        return resultado.toString().trim();
    }
}
