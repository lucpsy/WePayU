package br.ufal.ic.p2.wepayu.excecoes;

public class ValorNaoNumericoException extends WePayUException {
    public ValorNaoNumericoException() {
        super("Valor deve ser numerico.");
    }
}
