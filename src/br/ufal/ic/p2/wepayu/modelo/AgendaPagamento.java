package br.ufal.ic.p2.wepayu.modelo;

import br.ufal.ic.p2.wepayu.excecoes.AgendaNaoDisponivelException;
import br.ufal.ic.p2.wepayu.servico.ServicoAgenda;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface AgendaPagamento {
    LocalDate ANCORA = LocalDate.of(2005, 1, 1);

    boolean recebeEm(LocalDate data);

    BigDecimal fixoDoPeriodo(BigDecimal salario, Periodo periodo);

    String descricao();

}
