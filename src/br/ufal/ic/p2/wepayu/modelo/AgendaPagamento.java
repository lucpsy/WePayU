package br.ufal.ic.p2.wepayu.modelo;

import br.ufal.ic.p2.wepayu.excecoes.AgendaNaoDisponivelException;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AgendaPagamento {
    LocalDate ANCORA = LocalDate.of(2005, 1, 1);

    boolean recebeEm(LocalDate data);

    BigDecimal fixoDoPeriodo(BigDecimal salario, Periodo periodo);

    String descricao();

    AgendaPagamento SEMANAL_SEXTA = new Semanal(1);
    AgendaPagamento QUINZENAL_SEXTA = new Semanal(2);
    AgendaPagamento MENSAL_ULTIMO_UTIL = new Mensal();

    static AgendaPagamento de(String texto) throws AgendaNaoDisponivelException {
        for (AgendaPagamento a : new AgendaPagamento[]{SEMANAL_SEXTA, MENSAL_ULTIMO_UTIL, QUINZENAL_SEXTA}) {
            if (a.descricao().equals(texto)) return a;
        }
        throw new AgendaNaoDisponivelException();
    }
}
