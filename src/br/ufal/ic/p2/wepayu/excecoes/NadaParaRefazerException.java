package br.ufal.ic.p2.wepayu.excecoes;

public class NadaParaRefazerException extends WePayUException {
    public NadaParaRefazerException() {
        super("Nao ha comando a refazer.");
    }
}
