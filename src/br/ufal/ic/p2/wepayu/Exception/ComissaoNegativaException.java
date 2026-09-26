package br.ufal.ic.p2.wepayu.Exception;

public class ComissaoNegativaException extends Exception{
    public ComissaoNegativaException(){
        super("Comissao deve ser nao-negativa.");
    }
}
