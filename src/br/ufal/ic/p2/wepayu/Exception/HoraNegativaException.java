package br.ufal.ic.p2.wepayu.Exception;

public class HoraNegativaException extends Exception {
    public HoraNegativaException() {
        super("Horas devem ser positivas.");
    }
}
