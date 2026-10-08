package br.ufal.ic.p2.wepayu.modelo;

import br.ufal.ic.p2.wepayu.util.Datas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/** Recebe o salário mensal inteiro, no último dia útil de cada mês. */
public class Assalariado extends Empregado {
    public static final String TIPO = "assalariado";

    public Assalariado(String nome, String endereco, BigDecimal salario) {
        super(nome, endereco, salario);
    }

    public Assalariado(Empregado origem, BigDecimal salario) {
        super(origem, salario);
    }

    @Override
    public String getTipo() {
        return TIPO;
    }

    @Override
    public boolean recebeEm(LocalDate data) {
        return !data.isBefore(DATA_CONTRATACAO_PADRAO)
                && data.equals(Datas.ultimoDiaUtil(YearMonth.from(data)));
    }

    @Override
    public Periodo periodoDeApuracao(LocalDate dataPagamento) {
        LocalDate pagamentoAnterior = Datas.ultimoDiaUtil(YearMonth.from(dataPagamento).minusMonths(1));
        LocalDate inicio = pagamentoAnterior.plusDays(1);
        if (inicio.isBefore(DATA_CONTRATACAO_PADRAO)) {
            inicio = DATA_CONTRATACAO_PADRAO;
        }
        return Periodo.de(inicio, dataPagamento.plusDays(1));
    }

    @Override
    protected BigDecimal brutoDoPeriodo(Periodo periodo) {
        return getSalario();
    }
}
