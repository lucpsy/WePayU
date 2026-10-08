package br.ufal.ic.p2.wepayu.modelo;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Intervalo de datas que inclui o início e não inclui o fim, igual ao das
 * consultas do sistema (dataInicial entra, dataFinal não entra).
 */
public final class Periodo {
    private final LocalDate inicio;
    private final LocalDate fim;

    private Periodo(LocalDate inicio, LocalDate fim) {
        this.inicio = inicio;
        this.fim = fim;
    }

    public static Periodo de(LocalDate inicio, LocalDate fim) {
        if (inicio.isAfter(fim)) {
            throw new IllegalArgumentException("O início do período não pode ser depois do fim.");
        }
        return new Periodo(inicio, fim);
    }

    public boolean contem(LocalDate data) {
        return !data.isBefore(inicio) && data.isBefore(fim);
    }

    public long dias() {
        return ChronoUnit.DAYS.between(inicio, fim);
    }
}
