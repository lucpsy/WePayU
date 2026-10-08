package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Taxa extra cobrada pelo sindicato de um de seus membros. */
public class TaxaServico extends Lancamento {
    public TaxaServico(LocalDate data, BigDecimal valor) {
        super(data, valor);
    }
}
