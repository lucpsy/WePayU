package br.ufal.ic.p2.wepayu.excecoes;

public class IdentificacaoSindicatoJaExistenteException extends WePayUException {
    public IdentificacaoSindicatoJaExistenteException() {
        super("Ha outro empregado com esta identificacao de sindicato");
    }
}
