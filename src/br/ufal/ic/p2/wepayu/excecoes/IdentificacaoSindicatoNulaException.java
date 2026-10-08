package br.ufal.ic.p2.wepayu.excecoes;

public class IdentificacaoSindicatoNulaException extends WePayUException {
    public IdentificacaoSindicatoNulaException() {
        super("Identificacao do sindicato nao pode ser nula.");
    }
}
