package br.ufal.ic.p2.wepayu.Exception;

public class MembroNuloException extends Exception {
    public MembroNuloException() {
        super("Identificacao do membro nao pode ser nula.");
    }
}
