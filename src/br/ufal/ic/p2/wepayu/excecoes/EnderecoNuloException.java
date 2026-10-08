package br.ufal.ic.p2.wepayu.excecoes;

public class EnderecoNuloException extends WePayUException {
    public EnderecoNuloException() {
        super("Endereco nao pode ser nulo.");
    }
}
