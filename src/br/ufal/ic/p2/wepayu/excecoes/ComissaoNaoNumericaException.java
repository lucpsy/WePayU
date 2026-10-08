package br.ufal.ic.p2.wepayu.excecoes;

public class ComissaoNaoNumericaException extends WePayUException {
    public ComissaoNaoNumericaException() {
        super("Comissao deve ser numerica.");
    }
}
