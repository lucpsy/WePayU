package br.ufal.ic.p2.wepayu.models;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;

import java.text.DecimalFormat;

public class Empregado {
    private String nome;
    private String endereco;
    private String tipo;
    private String salario;
    private int sindicalizado;
    public String data_ponto;
    public String hora_ponto;
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
        return salario;
    }

}
