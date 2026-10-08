package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Dados comuns a todo empregado. Cada tipo (horista, assalariado, comissionado)
 * sabe em que dias é pago e como calcula o próprio pagamento.
 */
public abstract class Empregado {

    /**
     * Os testes não têm comando para informar a data de contratação. Por regra do
     * enunciado, assalariados e comissionados contam como contratados em 1/1/2005.
     */
    protected static final LocalDate DATA_CONTRATACAO_PADRAO = LocalDate.of(2005, 1, 1);

    private String nome;
    private String endereco;
    private BigDecimal salario;
    private MetodoPagamento metodoPagamento = new EmMaos();
    private MembroSindicato membroSindicato;

    // Ficam no empregado, e não no tipo, para não se perderem se ele trocar de tipo e voltar.
    private final List<CartaoPonto> cartoes = new ArrayList<>();
    private final List<Venda> vendas = new ArrayList<>();

    protected Empregado(String nome, String endereco, BigDecimal salario) {
        setNome(nome);
        setEndereco(endereco);
        setSalario(salario);
    }

    /** Troca de tipo: leva todos os dados do empregado atual para o novo objeto. */
    protected Empregado(Empregado origem, BigDecimal salario) {
        this(origem.nome, origem.endereco, salario);
        this.metodoPagamento = origem.metodoPagamento;
        this.membroSindicato = origem.membroSindicato;
        this.cartoes.addAll(origem.cartoes);
        this.vendas.addAll(origem.vendas);
    }

    public abstract String getTipo();

    /** Diz se este empregado é pago na data informada. */
    public abstract boolean recebeEm(LocalDate data);

    /**
     * Período que o pagamento da data cobre: tudo o que foi lançado desde o último
     * pagamento (inclusive a própria data) entra nas contas.
     */
    public abstract Periodo periodoDeApuracao(LocalDate dataPagamento);

    protected abstract BigDecimal brutoDoPeriodo(Periodo periodo);

    public BigDecimal calcularBruto(LocalDate dataPagamento) {
        return brutoDoPeriodo(periodoDeApuracao(dataPagamento));
    }

    public Contracheque calcularContracheque(LocalDate dataPagamento) {
        Periodo periodo = periodoDeApuracao(dataPagamento);
        return Contracheque.de(brutoDoPeriodo(periodo), descontoSindical(periodo));
    }

    protected BigDecimal descontoSindical(Periodo periodo) {
        if (membroSindicato == null) {
            return BigDecimal.ZERO;
        }
        return membroSindicato.calcularDesconto(periodo);
    }

    public String getNome() {
        return nome;
    }

    public final void setNome(String nome) {
        if (nome == null || nome.isEmpty()) {
            throw new IllegalArgumentException("O nome é obrigatório.");
        }
        this.nome = nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public final void setEndereco(String endereco) {
        if (endereco == null || endereco.isEmpty()) {
            throw new IllegalArgumentException("O endereço é obrigatório.");
        }
        this.endereco = endereco;
    }

    public BigDecimal getSalario() {
        return salario;
    }

    public final void setSalario(BigDecimal salario) {
        if (salario == null || salario.signum() < 0) {
            throw new IllegalArgumentException("O salário não pode ser negativo.");
        }
        this.salario = salario;
    }

    public MetodoPagamento getMetodoPagamento() {
        return metodoPagamento;
    }

    public void setMetodoPagamento(MetodoPagamento metodoPagamento) {
        if (metodoPagamento == null) {
            throw new IllegalArgumentException("O método de pagamento é obrigatório.");
        }
        this.metodoPagamento = metodoPagamento;
    }

    public boolean isSindicalizado() {
        return membroSindicato != null;
    }

    /** Dados da filiação, ou null se o empregado não é sindicalizado. */
    public MembroSindicato getMembroSindicato() {
        return membroSindicato;
    }

    public void sindicalizar(String identificacao, BigDecimal taxaSindical) {
        this.membroSindicato = new MembroSindicato(identificacao, taxaSindical);
    }

    public void desfiliarDoSindicato() {
        this.membroSindicato = null;
    }

    protected List<CartaoPonto> cartoes() {
        return Collections.unmodifiableList(cartoes);
    }

    protected void adicionarCartao(CartaoPonto cartao) {
        cartoes.add(cartao);
    }

    protected List<Venda> vendas() {
        return Collections.unmodifiableList(vendas);
    }

    protected void adicionarVenda(Venda venda) {
        vendas.add(venda);
    }
}
