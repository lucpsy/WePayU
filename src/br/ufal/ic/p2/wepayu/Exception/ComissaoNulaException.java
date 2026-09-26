package br.ufal.ic.p2.wepayu.Exception;

public class ComissaoNulaException extends  Exception{
    public ComissaoNulaException(){
        super("Comissao nao pode ser nula.");
    }
}
