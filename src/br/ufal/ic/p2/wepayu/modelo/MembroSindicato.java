package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Filiação de um empregado ao sindicato. A identificação aqui não tem relação
 * com o id do empregado no sistema de folha.
 */
public class MembroSindicato {
    private final String identificacao;
    private final BigDecimal taxaSindical;
    private final List<TaxaServico> taxasServico = new ArrayList<>();

    public MembroSindicato(String identificacao, BigDecimal taxaSindical) {
        if (identificacao == null || identificacao.isEmpty()) {
            throw new IllegalArgumentException("A identificação no sindicato é obrigatória.");
        }
        if (taxaSindical == null || taxaSindical.signum() < 0) {
            throw new IllegalArgumentException("A taxa sindical não pode ser negativa.");
        }
        this.identificacao = identificacao;
        this.taxaSindical = taxaSindical;
    }

    public String getIdentificacao() {
        return identificacao;
    }

    /** Taxa cobrada por dia. */
    public BigDecimal getTaxaSindical() {
        return taxaSindical;
    }

    public void lancarTaxaServico(LocalDate data, BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("A taxa de serviço precisa ser positiva.");
        }
        taxasServico.add(new TaxaServico(data, valor));
    }

    public BigDecimal totalTaxasServico(Periodo periodo) {
        BigDecimal total = BigDecimal.ZERO;
        for (TaxaServico taxa : taxasServico) {
            if (periodo.contem(taxa.getData())) {
                total = total.add(taxa.getValor());
            }
        }
        return total;
    }

    /** Taxa sindical de cada dia do período mais as taxas de serviço lançadas nele. */
    public BigDecimal calcularDesconto(Periodo periodo) {
        BigDecimal taxaDoPeriodo = taxaSindical.multiply(BigDecimal.valueOf(periodo.dias()));
        return taxaDoPeriodo.add(totalTaxasServico(periodo));
    }
}
