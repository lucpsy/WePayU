package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/** Registro de um valor numa data: base de cartões de ponto, vendas e taxas de serviço. */
public abstract class Lancamento {
    private final LocalDate data;
    private final BigDecimal valor;

    protected Lancamento(LocalDate data, BigDecimal valor) {
        this.data = Objects.requireNonNull(data);
        this.valor = Objects.requireNonNull(valor);
    }

    public LocalDate getData() {
        return data;
    }

    public BigDecimal getValor() {
        return valor;
    }
}
