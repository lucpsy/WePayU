package br.ufal.ic.p2.wepayu.excecoes;

public class DescricaoInvalidaException extends WePayUException {
    public DescricaoInvalidaException() {
        super("Descricao de agenda invalida");
    }
}
