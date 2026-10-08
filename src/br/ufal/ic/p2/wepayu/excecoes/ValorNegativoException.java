package br.ufal.ic.p2.wepayu.excecoes;

public class ValorNegativoException extends WePayUException {
    public ValorNegativoException() {
        super("Valor deve ser positivo.");
    }
}
