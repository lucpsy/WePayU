package br.ufal.ic.p2.wepayu.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;

/** Conversão e cálculos de datas usados em todo o sistema. */
public final class Datas {

    // Estrito de propósito: "31/2/2005" deve ser recusado, e não corrigido para outra data.
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Datas() {
    }

    /** Lê uma data no formato dia/mês/ano. Lança DateTimeParseException se for inválida. */
    public static LocalDate interpretar(String texto) {
        return LocalDate.parse(texto, FORMATO);
    }

    /** Último dia do mês que não cai em sábado nem domingo (feriados são desconsiderados). */
    public static LocalDate ultimoDiaUtil(YearMonth mes) {
        LocalDate dia = mes.atEndOfMonth();
        while (dia.getDayOfWeek() == DayOfWeek.SATURDAY || dia.getDayOfWeek() == DayOfWeek.SUNDAY) {
            dia = dia.minusDays(1);
        }
        return dia;
    }
}
