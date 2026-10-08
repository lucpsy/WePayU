package br.ufal.ic.p2.wepayu.excecoes;

public class MetodoInvalidoException extends WePayUException {
    public MetodoInvalidoException() {
        super("Metodo de pagamento invalido.");
    }
}
