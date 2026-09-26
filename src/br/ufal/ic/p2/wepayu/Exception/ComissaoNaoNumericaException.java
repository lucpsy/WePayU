package br.ufal.ic.p2.wepayu.Exception;

public class ComissaoNaoNumericaException extends Exception{
    public ComissaoNaoNumericaException(){
        super("Comissao deve ser numerica.");
    }
}
