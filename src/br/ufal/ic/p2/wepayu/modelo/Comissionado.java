package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Assalariado que também ganha um percentual das vendas. Recebe a cada 14 dias:
 * duas semanas de salário fixo mais a comissão do período.
 */
public class Comissionado extends Empregado {
    public static final String TIPO = "comissionado";

    private static final int DIAS_ENTRE_PAGAMENTOS = 14;
    private static final int MESES_POR_ANO = 12;
    private static final int SEMANAS_POR_ANO = 52;
    private static final int SEMANAS_POR_PAGAMENTO = 2;

    private BigDecimal comissao;

    public Comissionado(String nome, String endereco, BigDecimal salario, BigDecimal comissao) {
        super(nome, endereco, salario);
        setComissao(comissao);
    }

    /** Troca de tipo: o salário continua o mesmo e só a comissão é informada. */
    public Comissionado(Empregado origem, BigDecimal comissao) {
        super(origem, origem.getSalario());
        setComissao(comissao);
    }

    @Override
    public String getTipo() {
        return TIPO;
    }

    public BigDecimal getComissao() {
        return comissao;
    }

    public final void setComissao(BigDecimal comissao) {
        if (comissao == null || comissao.signum() < 0) {
            throw new IllegalArgumentException("A comissão não pode ser negativa.");
        }
        this.comissao = comissao;
    }

    public void lancarVenda(LocalDate data, BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) {
            throw new IllegalArgumentException("O valor da venda precisa ser positivo.");
        }
        adicionarVenda(new Venda(data, valor));
    }

    public BigDecimal totalVendas(Periodo periodo) {
        BigDecimal total = BigDecimal.ZERO;
        for (Venda venda : vendas()) {
            if (periodo.contem(venda.getData())) {
                total = total.add(venda.getValor());
            }
        }
        return total;
    }

    /** Parte fixa de cada pagamento: salário mensal x 12 / 52 semanas x 2 semanas, truncada em centavos. */
    public BigDecimal calcularFixo() {
        BigDecimal meses = BigDecimal.valueOf((long) MESES_POR_ANO * SEMANAS_POR_PAGAMENTO);
        return getSalario().multiply(meses).divide(BigDecimal.valueOf(SEMANAS_POR_ANO), 2, RoundingMode.DOWN);
    }

    public BigDecimal calcularComissao(Periodo periodo) {
        return totalVendas(periodo).multiply(comissao).setScale(2, RoundingMode.DOWN);
    }

    @Override
    public boolean recebeEm(LocalDate data) {
        long dias = ChronoUnit.DAYS.between(DATA_CONTRATACAO_PADRAO, data);
        return dias >= DIAS_ENTRE_PAGAMENTOS - 1 && dias % DIAS_ENTRE_PAGAMENTOS == DIAS_ENTRE_PAGAMENTOS - 1;
    }

    @Override
    public Periodo periodoDeApuracao(LocalDate dataPagamento) {
        return Periodo.de(dataPagamento.minusDays(DIAS_ENTRE_PAGAMENTOS - 1), dataPagamento.plusDays(1));
    }

    @Override
    protected BigDecimal brutoDoPeriodo(Periodo periodo) {
        return calcularFixo().add(calcularComissao(periodo));
    }
}
