package br.ufal.ic.p2.wepayu.Exception;

public class ValorNegativoException extends Exception {
    public ValorNegativoException() {
        super("Valor deve ser positivo.");
    }
}
