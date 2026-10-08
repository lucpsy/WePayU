package br.ufal.ic.p2.wepayu.excecoes;

/**
 * Raiz de todas as exceções de negócio do sistema. É abstrata de propósito:
 * cada erro tem sua própria classe, com a mensagem que os testes esperam.
 */
public abstract class WePayUException extends Exception {
    protected WePayUException(String mensagem) {
        super(mensagem);
    }

    protected WePayUException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
