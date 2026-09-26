package br.ufal.ic.p2.wepayu.Exception;

public class SindicalizadoInvalidoException extends Exception{
    public SindicalizadoInvalidoException(){
        super("Valor deve ser true ou false.");
    }
}
