package service;

import java.util.Locale;

/** Normaliza los textos visibles del medicamento de forma consistente. */
public final class NormalizacionMedicamento {
    private NormalizacionMedicamento() {
    }

    public static String capitalizarPrimeraLetra(String texto) {
        String minusculas = texto.toLowerCase(Locale.ROOT);
        for (int indice = 0; indice < minusculas.length();) {
            int codigo = minusculas.codePointAt(indice);
            if (Character.isLetter(codigo)) {
                String primeraLetra = new String(Character.toChars(codigo))
                        .toUpperCase(Locale.ROOT);
                return minusculas.substring(0, indice) + primeraLetra
                        + minusculas.substring(indice + Character.charCount(codigo));
            }
            indice += Character.charCount(codigo);
        }
        return minusculas;
    }

    public static String aMayusculas(String texto) {
        return texto.toUpperCase(Locale.ROOT);
    }
}
