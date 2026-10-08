package br.ufal.ic.p2.wepayu.excecoes;

public class ComissaoNegativaException extends WePayUException {
    public ComissaoNegativaException() {
        super("Comissao deve ser nao-negativa.");
    }
}
