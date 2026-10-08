package br.ufal.ic.p2.wepayu.excecoes;

public class HoraNegativaException extends WePayUException {
    public HoraNegativaException() {
        super("Horas devem ser positivas.");
    }
}
