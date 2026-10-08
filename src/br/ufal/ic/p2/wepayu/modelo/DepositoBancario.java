package br.ufal.ic.p2.wepayu.modelo;

/** Depósito em conta bancária (não confundir com a antiga classe Banco, que guardava os dados). */
public class DepositoBancario implements MetodoPagamento {
    private final String banco;
    private final String agencia;
    private final String contaCorrente;

    public DepositoBancario(String banco, String agencia, String contaCorrente) {
        this.banco = banco;
        this.agencia = agencia;
        this.contaCorrente = contaCorrente;
    }

    public String getBanco() {
        return banco;
    }

    public String getAgencia() {
        return agencia;
    }

    public String getContaCorrente() {
        return contaCorrente;
    }

    @Override
    public String getNome() {
        return BANCO;
    }

    @Override
    public String descrever(String enderecoDoEmpregado) {
        return banco + ", Ag. " + agencia + " CC " + contaCorrente;
    }
}
