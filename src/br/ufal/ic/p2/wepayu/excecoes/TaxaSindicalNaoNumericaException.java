package br.ufal.ic.p2.wepayu.excecoes;

public class TaxaSindicalNaoNumericaException extends WePayUException {
    public TaxaSindicalNaoNumericaException() {
        super("Taxa sindical deve ser numerica.");
    }
}
