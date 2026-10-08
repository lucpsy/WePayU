package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Horas trabalhadas por um horista em um dia. */
public class CartaoPonto extends Lancamento {
    public CartaoPonto(LocalDate data, BigDecimal horas) {
        super(data, horas);
    }

    public BigDecimal getHoras() {
        return getValor();
    }
}
