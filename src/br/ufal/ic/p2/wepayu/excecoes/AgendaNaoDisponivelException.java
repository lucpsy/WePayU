package br.ufal.ic.p2.wepayu.excecoes;

public class AgendaNaoDisponivelException extends WePayUException {
    public AgendaNaoDisponivelException() {
        super("Agenda de pagamento nao esta disponivel");
    }
}
