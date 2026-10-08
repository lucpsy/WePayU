package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.excecoes.ErroDeArquivoException;
import br.ufal.ic.p2.wepayu.excecoes.WePayUException;
import br.ufal.ic.p2.wepayu.persistencia.ArmazenamentoXml;
import br.ufal.ic.p2.wepayu.persistencia.RepositorioEmpregados;
import br.ufal.ic.p2.wepayu.servico.GerenciadorComandos;
import br.ufal.ic.p2.wepayu.servico.ServicoEmpregado;
import br.ufal.ic.p2.wepayu.servico.ServicoFolha;
import br.ufal.ic.p2.wepayu.servico.ServicoLancamento;

/**
 * Porta de entrada do sistema (usada pelos testes de aceitação). Não tem regra
 * de negócio: só encaminha cada pedido ao serviço responsável e passa os comandos
 * que alteram dados pelo {@link GerenciadorComandos}, para que possam ser desfeitos.
 */
public class Facade {
    private final GerenciadorComandos comandos;
    private final ServicoEmpregado empregados;
    private final ServicoLancamento lancamentos;
    private final ServicoFolha folha;

    public Facade() throws ErroDeArquivoException {
        ArmazenamentoXml armazenamento = new ArmazenamentoXml();
        RepositorioEmpregados repositorio = armazenamento.carregar();
        this.comandos = new GerenciadorComandos(repositorio, armazenamento);
        this.empregados = new ServicoEmpregado(repositorio);
        this.lancamentos = new ServicoLancamento(repositorio);
        this.folha = new ServicoFolha(repositorio);
    }

    public void zerarSistema() throws WePayUException {
        comandos.zerar();
    }

    public void encerrarSistema() throws ErroDeArquivoException {
        comandos.encerrar();
    }

    public void undo() throws WePayUException {
        comandos.desfazer();
    }

    public void redo() throws WePayUException {
        comandos.refazer();
    }

    public String getNumeroDeEmpregados() throws WePayUException {
        return comandos.consultar(empregados::numeroDeEmpregados);
    }

    // ---- empregados ----

    public String criarEmpregado(String nome, String endereco, String tipo, String salario)
            throws WePayUException {
        return comandos.executarComResultado(() -> empregados.criar(nome, endereco, tipo, salario));
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao)
            throws WePayUException {
        return comandos.executarComResultado(() -> empregados.criar(nome, endereco, tipo, salario, comissao));
    }

    public String getEmpregadoPorNome(String nome, int indice) throws WePayUException {
        return comandos.consultar(() -> empregados.buscarPorNome(nome, indice));
    }

    public String getAtributoEmpregado(String id, String atributo) throws WePayUException {
        return comandos.consultar(() -> empregados.getAtributo(id, atributo));
    }

    public void alteraEmpregado(String id, String atributo, String valor) throws WePayUException {
        comandos.executar(() -> empregados.alterar(id, atributo, valor));
    }

    /** Troca de tipo, informando o salário (ou a comissão, se o novo tipo for comissionado). */
    public void alteraEmpregado(String id, String atributo, String tipoNovo, String valor)
            throws WePayUException {
        comandos.executar(() -> empregados.alterarTipo(id, atributo, tipoNovo, valor));
    }

    /** Sindicaliza o empregado, informando identificação e taxa sindical. */
    public void alteraEmpregado(String id, String atributo, String valor, String idSindicato,
                                String taxaSindical) throws WePayUException {
        comandos.executar(() -> empregados.sindicalizar(id, atributo, valor, idSindicato, taxaSindical));
    }

    /** Muda o método de pagamento para depósito em banco. */
    public void alteraEmpregado(String id, String atributo, String valor, String banco, String agencia,
                                String contaCorrente) throws WePayUException {
        comandos.executar(() -> empregados.alterarParaBanco(id, atributo, valor, banco, agencia, contaCorrente));
    }

    public void removerEmpregado(String id) throws WePayUException {
        comandos.executar(() -> empregados.remover(id));
    }

    // ---- lançamentos ----

    public void lancaCartao(String id, String data, String horas) throws WePayUException {
        comandos.executar(() -> lancamentos.lancarCartao(id, data, horas));
    }

    public String getHorasNormaisTrabalhadas(String id, String dataInicial, String dataFinal)
            throws WePayUException {
        return comandos.consultar(() -> lancamentos.horasNormais(id, dataInicial, dataFinal));
    }

    public String getHorasExtrasTrabalhadas(String id, String dataInicial, String dataFinal)
            throws WePayUException {
        return comandos.consultar(() -> lancamentos.horasExtras(id, dataInicial, dataFinal));
    }

    public void lancaVenda(String id, String data, String valor) throws WePayUException {
        comandos.executar(() -> lancamentos.lancarVenda(id, data, valor));
    }

    public String getVendasRealizadas(String id, String dataInicial, String dataFinal)
            throws WePayUException {
        return comandos.consultar(() -> lancamentos.vendasRealizadas(id, dataInicial, dataFinal));
    }

    public void lancaTaxaServico(String membro, String data, String valor) throws WePayUException {
        comandos.executar(() -> lancamentos.lancarTaxaServico(membro, data, valor));
    }

    public String getTaxasServico(String id, String dataInicial, String dataFinal) throws WePayUException {
        return comandos.consultar(() -> lancamentos.taxasServico(id, dataInicial, dataFinal));
    }

    // ---- folha de pagamento ----

    public void rodaFolha(String data, String saida) throws WePayUException {
        comandos.executar(() -> folha.rodarFolha(data, saida));
    }

    public String totalFolha(String data) throws WePayUException {
        return comandos.consultar(() -> folha.totalFolha(data));
    }
}
