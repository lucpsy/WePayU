package br.ufal.ic.p2.wepayu.excecoes;

public class AgenciaNulaException extends WePayUException {
    public AgenciaNulaException() {
        super("Agencia nao pode ser nulo.");
    }
}
