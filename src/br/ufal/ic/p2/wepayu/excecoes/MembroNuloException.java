package br.ufal.ic.p2.wepayu.excecoes;

public class MembroNuloException extends WePayUException {
    public MembroNuloException() {
        super("Identificacao do membro nao pode ser nula.");
    }
}
