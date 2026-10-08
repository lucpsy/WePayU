package br.ufal.ic.p2.wepayu.excecoes;

public class ContaNulaException extends WePayUException {
    public ContaNulaException() {
        super("Conta corrente nao pode ser nulo.");
    }
}
