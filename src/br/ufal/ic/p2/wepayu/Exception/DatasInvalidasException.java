package br.ufal.ic.p2.wepayu.Exception;

public class DatasInvalidasException extends Exception {
    public DatasInvalidasException() {
        super("Data inicial nao pode ser posterior aa data final.");
    }
}
