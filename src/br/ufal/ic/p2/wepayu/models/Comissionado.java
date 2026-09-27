package br.ufal.ic.p2.wepayu.models;

public class Comissionado extends Empregado {
    public String comissao;
    public Comissionado(String nome, String endereco, String tipo, String salario,String comissao){
        super(nome,endereco,tipo,salario);
        this.comissao=comissao;
    }
    public String getComissao() {
        if(!comissao.contains(",")){
            comissao += ",00";
        }
        return comissao;
    }
}
