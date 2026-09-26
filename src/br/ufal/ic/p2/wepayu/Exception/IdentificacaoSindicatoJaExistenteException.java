package br.ufal.ic.p2.wepayu.Exception;

public class IdentificacaoSindicatoJaExistenteException extends Exception {
    public IdentificacaoSindicatoJaExistenteException() {
        super("Ha outro empregado com esta identificacao de sindicato");
    }
}
