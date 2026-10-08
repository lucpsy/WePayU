package br.ufal.ic.p2.wepayu.excecoes;

public class SindicalizadoInvalidoException extends WePayUException {
    public SindicalizadoInvalidoException() {
        super("Valor deve ser true ou false.");
    }
}
