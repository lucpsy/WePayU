package br.ufal.ic.p2.wepayu.Exception;

public class NaoComissionadoException extends Exception {
    public NaoComissionadoException() {
        super("Empregado nao eh comissionado.");
    }
}
