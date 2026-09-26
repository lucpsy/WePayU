package br.ufal.ic.p2.wepayu.Exception;

public class MembroInexistenteException extends Exception {
    public MembroInexistenteException() {
        super("Membro nao existe.");
    }
}
