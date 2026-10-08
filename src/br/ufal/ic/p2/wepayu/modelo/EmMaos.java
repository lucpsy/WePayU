package br.ufal.ic.p2.wepayu.modelo;

public class EmMaos implements MetodoPagamento {
    @Override
    public String getNome() {
        return EM_MAOS;
    }

    @Override
    public String descrever(String enderecoDoEmpregado) {
        return "Em maos";
    }
}
