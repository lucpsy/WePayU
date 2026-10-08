package br.ufal.ic.p2.wepayu.excecoes;

public class SalarioNuloException extends WePayUException {
    public SalarioNuloException() {
        super("Salario nao pode ser nulo.");
    }
}
