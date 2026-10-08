package br.ufal.ic.p2.wepayu.servico;

import br.ufal.ic.p2.wepayu.excecoes.ErroDeArquivoException;
import br.ufal.ic.p2.wepayu.excecoes.NadaParaDesfazerException;
import br.ufal.ic.p2.wepayu.excecoes.NadaParaRefazerException;
import br.ufal.ic.p2.wepayu.excecoes.SistemaEncerradoException;
import br.ufal.ic.p2.wepayu.excecoes.WePayUException;
import br.ufal.ic.p2.wepayu.persistencia.ArmazenamentoXml;
import br.ufal.ic.p2.wepayu.persistencia.RepositorioEmpregados;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Controla a vida do sistema: cada comando que altera dados é executado aqui para
 * que possa ser desfeito e refeito, e nenhum comando é aceito depois de encerrar.
 * <p>
 * O estado antes e depois de cada comando é guardado como XML. Se o comando falha
 * no meio, o estado anterior é restaurado, então o sistema nunca fica pela metade.
 */
public class GerenciadorComandos {

    /** Comando que não devolve nada. */
    public interface Acao {
        void executar() throws WePayUException;
    }

    /** Comando ou consulta que devolve um resultado. */
    public interface Operacao<T> {
        T executar() throws WePayUException;
    }

    private record RegistroComando(String antes, String depois) {
    }

    private final RepositorioEmpregados repositorio;
    private final ArmazenamentoXml armazenamento;
    private final Deque<RegistroComando> pilhaDesfazer = new ArrayDeque<>();
    private final Deque<RegistroComando> pilhaRefazer = new ArrayDeque<>();
    private boolean encerrado = false;

    public GerenciadorComandos(RepositorioEmpregados repositorio, ArmazenamentoXml armazenamento) {
        this.repositorio = repositorio;
        this.armazenamento = armazenamento;
    }

    public void executar(Acao acao) throws WePayUException {
        executarComResultado(() -> {
            acao.executar();
            return null;
        });
    }

    public <T> T executarComResultado(Operacao<T> operacao) throws WePayUException {
        verificarSeEncerrado();
        String antes = armazenamento.serializar(repositorio);
        try {
            T resultado = operacao.executar();
            pilhaDesfazer.push(new RegistroComando(antes, armazenamento.serializar(repositorio)));
            pilhaRefazer.clear();
            return resultado;
        } catch (WePayUException | RuntimeException e) {
            repositorio.substituirPor(armazenamento.desserializar(antes));
            throw e;
        }
    }

    /** Consultas não alteram nada e por isso não entram no histórico. */
    public <T> T consultar(Operacao<T> operacao) throws WePayUException {
        verificarSeEncerrado();
        return operacao.executar();
    }

    public void zerar() throws WePayUException {
        executar(() -> {
            repositorio.limpar();
            armazenamento.apagarArquivo();
        });
    }

    public void desfazer() throws WePayUException {
        verificarSeEncerrado();
        if (pilhaDesfazer.isEmpty()) {
            throw new NadaParaDesfazerException();
        }
        RegistroComando registro = pilhaDesfazer.pop();
        repositorio.substituirPor(armazenamento.desserializar(registro.antes()));
        pilhaRefazer.push(registro);
    }

    public void refazer() throws WePayUException {
        verificarSeEncerrado();
        if (pilhaRefazer.isEmpty()) {
            throw new NadaParaRefazerException();
        }
        RegistroComando registro = pilhaRefazer.pop();
        repositorio.substituirPor(armazenamento.desserializar(registro.depois()));
        pilhaDesfazer.push(registro);
    }

    /** Grava os dados em arquivo e bloqueia qualquer comando novo. Não entra no histórico. */
    public void encerrar() throws ErroDeArquivoException {
        if (!encerrado) {
            armazenamento.salvar(repositorio);
            encerrado = true;
        }
    }

    private void verificarSeEncerrado() throws SistemaEncerradoException {
        if (encerrado) {
            throw new SistemaEncerradoException();
        }
    }
}
