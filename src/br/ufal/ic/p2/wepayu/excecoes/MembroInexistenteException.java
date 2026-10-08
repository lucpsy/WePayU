package br.ufal.ic.p2.wepayu.excecoes;

public class MembroInexistenteException extends WePayUException {
    public MembroInexistenteException() {
        super("Membro nao existe.");
    }
}
