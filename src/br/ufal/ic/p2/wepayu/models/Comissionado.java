package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;

public class Comissionado extends Empregado {
    double comissao;
    public Comissionado(String nome, String endereco, String tipo, String salario,double comissao) throws EmpregadoNaoExisteException {
        super(nome,endereco,tipo,salario);
        this.comissao=comissao;
    }
}
