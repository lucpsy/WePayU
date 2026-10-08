package br.ufal.ic.p2.wepayu.excecoes;

public class SemNomeException extends WePayUException {
    public SemNomeException() {
        super("Nao ha empregado com esse nome.");
    }
}
