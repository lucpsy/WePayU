package br.ufal.ic.p2.wepayu.excecoes;

public class NaoHoristaException extends WePayUException {
    public NaoHoristaException() {
        super("Empregado nao eh horista.");
    }
}
