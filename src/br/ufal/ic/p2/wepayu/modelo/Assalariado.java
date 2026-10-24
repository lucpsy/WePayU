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
        setAgendaPagamento(new Mensal());
    }

    public Assalariado(Empregado origem, BigDecimal salario) {
        super(origem, salario);
    }

    @Override
    public String getTipo() {
        return TIPO;
    }

    @Override
    protected BigDecimal brutoDoPeriodo(Periodo periodo) {
        return fixoDoPeriodo(periodo);
    }
}
