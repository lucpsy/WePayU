package br.ufal.ic.p2.wepayu.excecoes;

public class HoraNaoNumericaException extends WePayUException {
    public HoraNaoNumericaException() {
        super("Horas devem ser numericas.");
    }
}
