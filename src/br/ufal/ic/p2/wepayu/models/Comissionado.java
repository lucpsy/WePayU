package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;

public class Comissionado extends Empregado {
    public String comissao;
    public Comissionado(String nome, String endereco, String tipo, String salario,String comissao){
        super(nome,endereco,tipo,salario);
        if(!comissao.contains(",")){
            comissao += ",00";
        }
        this.comissao=comissao;
    }
    public String getComissao() {
        return comissao;
    }
}
