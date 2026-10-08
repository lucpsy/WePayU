package br.ufal.ic.p2.wepayu.excecoes;

public class SistemaEncerradoException extends WePayUException {
    public SistemaEncerradoException() {
        super("Nao pode dar comandos depois de encerrarSistema.");
    }
}
