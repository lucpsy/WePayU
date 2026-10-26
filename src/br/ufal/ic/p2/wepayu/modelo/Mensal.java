package br.ufal.ic.p2.wepayu.modelo;

import br.ufal.ic.p2.wepayu.util.Datas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;

public class Mensal implements AgendaPagamento{
    private final int dia;
    public Mensal(String dia){
        if(Objects.equals(dia, "$")){
            this.dia = 0;
        }else{
            this.dia = Integer.parseInt(dia);
        }
    }
    @Override
    public boolean recebeEm(LocalDate data) {
        if(!data.isBefore(ANCORA)){
            if(dia == 0 ){
                return data.equals(Datas.ultimoDiaUtil(YearMonth.from(data)));
            }else{
                return data.getDayOfMonth() == dia;
            }
        }
        return false;
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
        if(dia == 0){return "mensal $";}
        else{return "mensal " + dia;}
    }
}
