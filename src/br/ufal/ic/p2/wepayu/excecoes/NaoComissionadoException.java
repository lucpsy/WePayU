package br.ufal.ic.p2.wepayu.excecoes;

public class NaoComissionadoException extends WePayUException {
    public NaoComissionadoException() {
        super("Empregado nao eh comissionado.");
    }
}
