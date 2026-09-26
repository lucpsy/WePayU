package br.ufal.ic.p2.wepayu.Exception;

public class ContaNulaException extends Exception{
    public ContaNulaException(){
        super("Conta corrente nao pode ser nulo.");
    }
}
