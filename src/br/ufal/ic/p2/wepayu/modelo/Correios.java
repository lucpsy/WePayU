package br.ufal.ic.p2.wepayu.modelo;

public class Correios implements MetodoPagamento {
    @Override
    public String getNome() {
        return CORREIOS;
    }

    @Override
    public String descrever(String enderecoDoEmpregado) {
        return "Correios, " + enderecoDoEmpregado;
    }
}
