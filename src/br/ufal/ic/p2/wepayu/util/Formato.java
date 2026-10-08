package br.ufal.ic.p2.wepayu.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Formatação de números no padrão brasileiro (vírgula como separador decimal). */
public final class Formato {

    private Formato() {
    }

    /** Valor com no mínimo duas casas, sem esconder casas a mais (ex.: 23 vira 23,00 e 0,055 continua 0,055). */
    public static String valor(BigDecimal numero) {
        BigDecimal ajustado = numero.stripTrailingZeros();
        if (ajustado.scale() < 2) {
            ajustado = ajustado.setScale(2);
        }
        return ajustado.toPlainString().replace('.', ',');
    }

    /** Valor em reais arredondado para centavos, como aparece na folha de pagamento. */
    public static String moeda(BigDecimal numero) {
        return numero.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    /** Quantidade de horas sem zeros sobrando (8 em vez de 8,00; 7,5 continua 7,5). */
    public static String horas(BigDecimal numero) {
        return numero.stripTrailingZeros().toPlainString().replace('.', ',');
    }
}
