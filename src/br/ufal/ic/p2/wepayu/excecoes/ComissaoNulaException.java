package br.ufal.ic.p2.wepayu.excecoes;

public class ComissaoNulaException extends WePayUException {
    public ComissaoNulaException() {
        super("Comissao nao pode ser nula.");
    }
}
