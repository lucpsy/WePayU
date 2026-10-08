package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Resultado de venda de um comissionado. */
public class Venda extends Lancamento {
    public Venda(LocalDate data, BigDecimal valor) {
        super(data, valor);
    }
}
