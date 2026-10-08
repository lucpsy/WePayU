package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;

/** Resultado do cálculo de um pagamento: salário bruto, descontos e salário líquido. */
public final class Contracheque {
    private final BigDecimal bruto;
    private final BigDecimal descontos;
    private final BigDecimal liquido;

    private Contracheque(BigDecimal bruto, BigDecimal descontos, BigDecimal liquido) {
        this.bruto = bruto;
        this.descontos = descontos;
        this.liquido = liquido;
    }

    public static Contracheque de(BigDecimal bruto, BigDecimal descontos) {
        return new Contracheque(bruto, descontos, bruto.subtract(descontos));
    }

    /** Caso em que os descontos passariam do bruto: não recebe nada e não aparece desconto. */
    public static Contracheque zerado(BigDecimal bruto) {
        return new Contracheque(bruto, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    public BigDecimal getBruto() {
        return bruto;
    }

    public BigDecimal getDescontos() {
        return descontos;
    }

    public BigDecimal getLiquido() {
        return liquido;
    }
}
