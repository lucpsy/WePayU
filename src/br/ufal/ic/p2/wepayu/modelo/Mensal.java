package br.ufal.ic.p2.wepayu.modelo;

import br.ufal.ic.p2.wepayu.util.Datas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public class Mensal implements AgendaPagamento{
    @Override
    public boolean recebeEm(LocalDate data) {
        return !data.isBefore(ANCORA)
                && data.equals(Datas.ultimoDiaUtil(YearMonth.from(data)));
    }

    @Override
    public BigDecimal fixoDoPeriodo(BigDecimal salario, Periodo periodo) {
        long pagamentos = 0;
        for(LocalDate atual = periodo.getInicio();atual.isBefore(periodo.getFim());atual = atual.plusDays(1)) {
            if(recebeEm(atual)) {
                pagamentos++;
            }
        }
        return salario.multiply(new BigDecimal(pagamentos));
    }

    @Override
    public String descricao() {
        return "mensal $";
    }
}
