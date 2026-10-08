package br.ufal.ic.p2.wepayu.excecoes;

public class NadaParaDesfazerException extends WePayUException {
    public NadaParaDesfazerException() {
        super("Nao ha comando a desfazer.");
    }
}
