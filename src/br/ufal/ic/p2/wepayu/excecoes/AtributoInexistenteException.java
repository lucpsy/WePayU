package br.ufal.ic.p2.wepayu.excecoes;

public class AtributoInexistenteException extends WePayUException {
    public AtributoInexistenteException() {
        super("Atributo nao existe.");
    }
}
