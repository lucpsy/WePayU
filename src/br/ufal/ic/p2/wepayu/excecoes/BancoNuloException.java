package br.ufal.ic.p2.wepayu.excecoes;

public class BancoNuloException extends WePayUException {
    public BancoNuloException() {
        super("Banco nao pode ser nulo.");
    }
}
