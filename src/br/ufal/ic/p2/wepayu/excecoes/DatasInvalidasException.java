package br.ufal.ic.p2.wepayu.excecoes;

public class DatasInvalidasException extends WePayUException {
    public DatasInvalidasException() {
        super("Data inicial nao pode ser posterior aa data final.");
    }
}
