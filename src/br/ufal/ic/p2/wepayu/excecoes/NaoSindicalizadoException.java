package br.ufal.ic.p2.wepayu.excecoes;

public class NaoSindicalizadoException extends WePayUException {
    public NaoSindicalizadoException() {
        super("Empregado nao eh sindicalizado.");
    }
}
