package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Semanal implements AgendaPagamento{
    private final int intervalo;
    private final int dia;
    public Semanal(int intervalo, int dia) {
        this.intervalo = intervalo;
        this.dia = dia;
    }
    @Override
    public boolean recebeEm(LocalDate data) {
        if(intervalo == 1) {
            return data.getDayOfWeek() == DayOfWeek.of(dia);
        }else {
            long semanas = ChronoUnit.WEEKS.between(ANCORA, data);
            return semanas % intervalo == intervalo - 1 && data.getDayOfWeek() == DayOfWeek.of(dia);
        }
    }

    @Override
    public BigDecimal fixoDoPeriodo(BigDecimal salario, Periodo periodo) {
        boolean temPagamento = false;
        for (LocalDate d = periodo.getInicio(); d.isBefore(periodo.getFim()); d = d.plusDays(1)) {
            if (recebeEm(d)) {
                temPagamento = true;
                break;
            }
        }
        if (!temPagamento) return BigDecimal.ZERO;
        return salario.multiply(BigDecimal.valueOf(12))
                .multiply(BigDecimal.valueOf(intervalo))
                .divide(BigDecimal.valueOf(52), 2, RoundingMode.DOWN);
    }

    @Override
    public String descricao() {
        if(intervalo == 1){
            return "semanal " + dia;
        }else{
            return "semanal " + intervalo + " " + dia;
        }
    }
}
