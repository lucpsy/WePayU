package br.ufal.ic.p2.wepayu.modelo;

/** Forma como o empregado recebe o salário. */
public interface MetodoPagamento {
    String EM_MAOS = "emMaos";
    String CORREIOS = "correios";
    String BANCO = "banco";

    /** Nome do método como é lido e gravado pelo sistema. */
    String getNome();

    /** Texto da coluna "Metodo" da folha de pagamento. */
    String descrever(String enderecoDoEmpregado);
}
