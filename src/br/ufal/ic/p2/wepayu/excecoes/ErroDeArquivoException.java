package br.ufal.ic.p2.wepayu.excecoes;

public class ErroDeArquivoException extends WePayUException {
    public ErroDeArquivoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
