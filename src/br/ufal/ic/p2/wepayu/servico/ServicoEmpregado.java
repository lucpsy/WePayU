package br.ufal.ic.p2.wepayu.servico;

import br.ufal.ic.p2.wepayu.excecoes.*;
import br.ufal.ic.p2.wepayu.modelo.*;
import br.ufal.ic.p2.wepayu.persistencia.RepositorioEmpregados;
import br.ufal.ic.p2.wepayu.util.Formato;

import java.math.BigDecimal;
import java.util.List;

/** Cadastro, consulta, alteração e remoção de empregados. */
public class ServicoEmpregado {
    private final RepositorioEmpregados repositorio;

    public ServicoEmpregado(RepositorioEmpregados repositorio) {
        this.repositorio = repositorio;
    }

    /** Cria um horista ou assalariado. Comissionado precisa da outra versão, com comissão. */
    public String criar(String nome, String endereco, String tipo, String salario) throws WePayUException {
        Entrada.nome(nome);
        Entrada.endereco(endereco);
        BigDecimal valorSalario = Entrada.salario(salario);
        Empregado novo = switch (tipo) {
            case Horista.TIPO -> new Horista(nome, endereco, valorSalario);
            case Assalariado.TIPO -> new Assalariado(nome, endereco, valorSalario);
            case Comissionado.TIPO -> throw new TipoNaoAplicavelException();
            default -> throw new TipoInvalidoException();
        };
        return repositorio.adicionar(novo);
    }

    public String criar(String nome, String endereco, String tipo, String salario, String comissao)
            throws WePayUException {
        Entrada.nome(nome);
        Entrada.endereco(endereco);
        BigDecimal valorSalario = Entrada.salario(salario);
        BigDecimal valorComissao = Entrada.comissao(comissao);
        if (tipo.equals(Horista.TIPO) || tipo.equals(Assalariado.TIPO)) {
            throw new TipoNaoAplicavelException();
        }
        if (!tipo.equals(Comissionado.TIPO)) {
            throw new TipoInvalidoException();
        }
        return repositorio.adicionar(new Comissionado(nome, endereco, valorSalario, valorComissao));
    }

    /** Devolve o id do empregado de número {@code indice} (começando em 1) entre os que têm o nome. */
    public String buscarPorNome(String nome, int indice) throws WePayUException {
        List<String> ids = repositorio.idsComNome(nome);
        if (indice < 1 || indice > ids.size()) {
            throw new SemNomeException();
        }
        return ids.get(indice - 1);
    }

    public String getAtributo(String id, String atributo) throws WePayUException {
        Empregado empregado = repositorio.buscar(id);
        return switch (atributo) {
            case "nome" -> empregado.getNome();
            case "endereco" -> empregado.getEndereco();
            case "tipo" -> empregado.getTipo();
            case "salario" -> Formato.valor(empregado.getSalario());
            case "comissao" -> Formato.valor(comissionado(empregado).getComissao());
            case "sindicalizado" -> String.valueOf(empregado.isSindicalizado());
            case "idSindicato" -> membro(empregado).getIdentificacao();
            case "taxaSindical" -> Formato.valor(membro(empregado).getTaxaSindical());
            case "metodoPagamento" -> empregado.getMetodoPagamento().getNome();
            case "banco" -> deposito(empregado).getBanco();
            case "agencia" -> deposito(empregado).getAgencia();
            case "contaCorrente" -> deposito(empregado).getContaCorrente();
            default -> throw new AtributoInexistenteException();
        };
    }

    /**
     * Altera nome, endereço, salário, comissão, tipo (mantendo o salário), método
     * de pagamento (emMaos ou correios) ou remove a filiação ao sindicato.
     */
    public void alterar(String id, String atributo, String valor) throws WePayUException {
        Empregado empregado = repositorio.buscar(id);
        switch (atributo) {
            case "nome" -> empregado.setNome(Entrada.nome(valor));
            case "endereco" -> empregado.setEndereco(Entrada.endereco(valor));
            case "salario" -> empregado.setSalario(Entrada.salario(valor));
            case "comissao" -> comissionado(empregado).setComissao(Entrada.comissao(valor));
            case "tipo" -> trocarTipoMantendoSalario(id, empregado, valor);
            case "sindicalizado" -> desfiliar(empregado, valor);
            case "metodoPagamento" -> alterarMetodoPagamento(empregado, valor);
            default -> throw new AtributoInexistenteException();
        }
    }

    /** Filia o empregado ao sindicato. A identificação precisa ser única. */
    public void sindicalizar(String id, String atributo, String valor, String idSindicato, String taxaSindical)
            throws WePayUException {
        if (Entrada.vazio(id)) {
            throw new IdentificacaoNulaException();
        }
        exigirAtributo(atributo, "sindicalizado");
        if (!valor.equals("true")) {
            throw new SindicalizadoInvalidoException();
        }
        String identificacao = Entrada.idSindicato(idSindicato);
        BigDecimal taxa = Entrada.taxaSindical(taxaSindical);
        Empregado empregado = repositorio.buscar(id);
        if (repositorio.existeIdSindicato(identificacao)) {
            throw new IdentificacaoSindicatoJaExistenteException();
        }
        empregado.sindicalizar(identificacao, taxa);
    }

    public void alterarParaBanco(String id, String atributo, String valor, String banco, String agencia,
                                 String contaCorrente) throws WePayUException {
        Empregado empregado = repositorio.buscar(id);
        exigirAtributo(atributo, "metodoPagamento");
        if (!valor.equals(MetodoPagamento.BANCO)) {
            throw new MetodoInvalidoException();
        }
        if (Entrada.vazio(banco)) {
            throw new BancoNuloException();
        }
        if (Entrada.vazio(agencia)) {
            throw new AgenciaNulaException();
        }
        if (Entrada.vazio(contaCorrente)) {
            throw new ContaNulaException();
        }
        empregado.setMetodoPagamento(new DepositoBancario(banco, agencia, contaCorrente));
    }

    /**
     * Troca o tipo do empregado informando o valor do novo tipo: salário para
     * horista e assalariado, comissão para comissionado (que mantém o salário atual).
     * Se o tipo já é o mesmo, só o valor é atualizado.
     */
    public void alterarTipo(String id, String atributo, String tipoNovo, String valor) throws WePayUException {
        Empregado atual = repositorio.buscar(id);
        exigirAtributo(atributo, "tipo");
        validarTipo(tipoNovo);
        BigDecimal valorNovo = tipoNovo.equals(Comissionado.TIPO) ? Entrada.comissao(valor) : Entrada.salario(valor);
        repositorio.substituir(id, converter(atual, tipoNovo, valorNovo));
    }

    public void remover(String id) throws WePayUException {
        repositorio.remover(id);
    }

    public String numeroDeEmpregados() {
        return String.valueOf(repositorio.quantidade());
    }

    private void trocarTipoMantendoSalario(String id, Empregado atual, String tipoNovo) throws WePayUException {
        validarTipo(tipoNovo);
        if (atual.getTipo().equals(tipoNovo)) {
            return;
        }
        if (tipoNovo.equals(Comissionado.TIPO)) {
            throw new ComissaoNulaException();
        }
        repositorio.substituir(id, converter(atual, tipoNovo, atual.getSalario()));
    }

    /**
     * Devolve o empregado já no tipo novo. Cartões, vendas, método de pagamento e
     * sindicato acompanham a troca. No mesmo tipo, o próprio objeto é atualizado.
     */
    private Empregado converter(Empregado atual, String tipoNovo, BigDecimal valor) {
        switch (tipoNovo) {
            case Horista.TIPO:
                if (atual instanceof Horista) {
                    atual.setSalario(valor);
                    return atual;
                }
                return new Horista(atual, valor);
            case Assalariado.TIPO:
                if (atual instanceof Assalariado) {
                    atual.setSalario(valor);
                    return atual;
                }
                return new Assalariado(atual, valor);
            default:
                if (atual instanceof Comissionado comissionado) {
                    comissionado.setComissao(valor);
                    return comissionado;
                }
                return new Comissionado(atual, valor);
        }
    }

    private void desfiliar(Empregado empregado, String valor) throws WePayUException {
        if (!valor.equals("false")) {
            throw new SindicalizadoInvalidoException();
        }
        empregado.desfiliarDoSindicato();
    }

    private void alterarMetodoPagamento(Empregado empregado, String metodo) throws WePayUException {
        switch (metodo) {
            case MetodoPagamento.EM_MAOS -> empregado.setMetodoPagamento(new EmMaos());
            case MetodoPagamento.CORREIOS -> empregado.setMetodoPagamento(new Correios());
            default -> throw new MetodoInvalidoException();
        }
    }

    private void validarTipo(String tipo) throws TipoInvalidoException {
        if (!(tipo.equals(Horista.TIPO) || tipo.equals(Assalariado.TIPO) || tipo.equals(Comissionado.TIPO))) {
            throw new TipoInvalidoException();
        }
    }

    private void exigirAtributo(String informado, String esperado) throws AtributoInexistenteException {
        if (!esperado.equals(informado)) {
            throw new AtributoInexistenteException();
        }
    }

    private Comissionado comissionado(Empregado empregado) throws NaoComissionadoException {
        if (!(empregado instanceof Comissionado comissionado)) {
            throw new NaoComissionadoException();
        }
        return comissionado;
    }

    private MembroSindicato membro(Empregado empregado) throws NaoSindicalizadoException {
        if (!empregado.isSindicalizado()) {
            throw new NaoSindicalizadoException();
        }
        return empregado.getMembroSindicato();
    }

    private DepositoBancario deposito(Empregado empregado) throws NaoBancoException {
        if (!(empregado.getMetodoPagamento() instanceof DepositoBancario deposito)) {
            throw new NaoBancoException();
        }
        return deposito;
    }
}
