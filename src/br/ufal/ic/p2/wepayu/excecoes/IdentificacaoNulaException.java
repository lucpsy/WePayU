package br.ufal.ic.p2.wepayu.excecoes;

public class IdentificacaoNulaException extends WePayUException {
    public IdentificacaoNulaException() {
        super("Identificacao do empregado nao pode ser nula.");
    }
}
