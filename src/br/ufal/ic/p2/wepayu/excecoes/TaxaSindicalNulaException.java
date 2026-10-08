package br.ufal.ic.p2.wepayu.excecoes;

public class TaxaSindicalNulaException extends WePayUException {
    public TaxaSindicalNulaException() {
        super("Taxa sindical nao pode ser nula.");
    }
}
