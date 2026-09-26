package br.ufal.ic.p2.wepayu.Exception;

public class NaoSindicalizadoException extends Exception{
    public NaoSindicalizadoException(){
        super("Empregado nao eh sindicalizado.");
    }
}
