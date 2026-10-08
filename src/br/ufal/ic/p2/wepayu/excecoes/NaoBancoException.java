package br.ufal.ic.p2.wepayu.excecoes;

public class NaoBancoException extends WePayUException {
    public NaoBancoException() {
        super("Empregado nao recebe em banco.");
    }
}
