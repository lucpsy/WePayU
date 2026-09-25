package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;

import java.text.DecimalFormat;

public class Empregado {
    public String nome;
    public String endereco;
    public String tipo;
    public String salario;
    public boolean sindicalizado = false;
    public String id_sindicato = null;
    public String taxa_sindical =  null;
    public Empregado(String nome, String endereco, String tipo, String salario){
        this.nome = nome;
        this.endereco = endereco;
        this.tipo = tipo;
        if(!salario.contains(",")){
            salario += ",00";
        }
        this.salario = salario;
    }

    public String getNome() {
        return nome;
    }

    public String getEndereco() {
        return endereco;
    }

    public String getTipo() {
        return tipo;
    }

    public String getSalario() {
        if(!salario.contains(",")){
           return salario + ",00";
        }
        return salario;
    }

}
