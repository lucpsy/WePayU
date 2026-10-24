package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class Semanal implements AgendaPagamento{
    private final int intervalo;
    Semanal(int intervalo) {
        this.intervalo = intervalo;
    }
    @Override
    public boolean recebeEm(LocalDate data) {
        if(intervalo == 1) {
            return data.getDayOfWeek() == DayOfWeek.FRIDAY;
        }else {
            long dias = ChronoUnit.DAYS.between(ANCORA, data);
            return dias % 14 == 14 - 1;
        }
    }

    @Override
    public BigDecimal fixoDoPeriodo(BigDecimal salario, Periodo periodo) {
        BigDecimal semanas = BigDecimal.valueOf(periodo.dias() / 7);
        return salario.multiply(BigDecimal.valueOf(12)).multiply(semanas)
                .divide(BigDecimal.valueOf(52), 2, RoundingMode.DOWN);
    }

    @Override
    public String descricao() {
        if(intervalo == 1){
            return "semanal 5";
        }else{
            return "semanal 2 5";
        }
    }
}
