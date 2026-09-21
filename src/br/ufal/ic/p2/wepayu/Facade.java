package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.models.Assalariado;
import br.ufal.ic.p2.wepayu.models.Comissionado;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.Horista;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Facade {
    Banco banco = new Banco();
    //-------------------------------------------------------
    public void zerarSistema(){
        banco.clear();
    }
    //-------------------------------------------------------
    public String criarEmpregado(String nome,String endereco,String tipo,String salario) throws EmpregadoNaoExisteException {
        if(tipo.equals("horista")){
            banco.add(new Horista(nome,endereco,tipo,salario));
        }else if(tipo.equals("assalariado")){
            banco.add(new Assalariado(nome,endereco,tipo,salario));
        }
        return  banco.empregados.lastEntry().getKey();
    }
    //-------------------------------------------------------
    public String criarEmpregado(String nome,String endereco,String tipo,String salario,double comissao) throws EmpregadoNaoExisteException {
        banco.add(new Comissionado(nome,endereco,tipo,salario,comissao));
        return banco.empregados.lastEntry().getKey();
    }
    //--------------------------------------------------------
    public String getEmpregadoPorNome(String nome,int indice) throws EmpregadoNaoExisteException {
        for(Map.Entry<String,Empregado> e: banco.empregados.entrySet()){
            if(e.getValue().getNome().equals(nome)){
                indice--;
                if(indice <= 0){
                    return e.getKey();
                }
            }
        }
        throw  new EmpregadoNaoExisteException();
    }
    //-------------------------------------------------------
    public String getAtributoEmpregado(String id,String atributo){
        if(atributo.equals("nome")){
            return banco.empregados.get(id).getNome();
        }
        else if(atributo.equals("endereco")){
            return banco.empregados.get(id).getEndereco();
        }
        else if(atributo.equals("tipo")){
            return banco.empregados.get(id).getTipo();
        }
        else if(atributo.equals("salario")){
            return banco.empregados.get(id).getSalario();
        }
        //-------------------------------------------------------
        return null;
    }
    //------------------------------------------------------
    public void removerEmpregado(String id) throws EmpregadoNaoExisteException{
        if(banco.empregados.remove(id) == null){
            throw new EmpregadoNaoExisteException();
        }
    }
    //--------------------------------------------------------
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    public void lancaCartao(String id,String data,String hora) throws EmpregadoNaoExisteException{
        if(!banco.bancoDeHoras.containsKey(data)){
            throw new EmpregadoNaoExisteException();
        }
        banco.bancoDeHoras.get(id).put(data,hora);
    }
    //------------------------------------------------------
    public String getHorasTrabalhadas(String id,String data_inicial,String data_final,String hora) throws EmpregadoNaoExisteException{
        double total = 0;
        if(banco.bancoDeHoras.containsKey(id)){
            for(Map.Entry<String,String> e: banco.bancoDeHoras.get(id).entrySet()){
                LocalDate data =  LocalDate.parse(e.getKey(),formatter);
                if(!data.isBefore(LocalDate.parse(data_final,formatter))){
                    return String.valueOf(total);
                }
                if(!data.isBefore(LocalDate.parse(data_inicial,formatter))){
                    double hrs = Double.parseDouble(e.getValue());
                    if(hrs > 8){
                        total += 8;
                    }else{
                        total += hrs;
                    }
                }
            }
        }else{
            throw new EmpregadoNaoExisteException();
        }
        return String.valueOf(total);
    }
    //---------------------------------------------------------
    public String getHorasExtrasTrabalhadas(String id,String data_inicial,String data_final,String hora) throws EmpregadoNaoExisteException{
        double total = 0;
        if(banco.bancoDeHoras.containsKey(id) && banco.empregados.get(id).getTipo().equals("horista")){
            for(Map.Entry<String,String> e: banco.bancoDeHoras.get(id).entrySet()){
                LocalDate data =  LocalDate.parse(e.getKey(),formatter);
                if(!data.isBefore(LocalDate.parse(data_final,formatter))){
                    return String.valueOf(total);
                }
                if(!data.isBefore(LocalDate.parse(data_inicial,formatter))){
                    double hrs = Double.parseDouble(e.getValue());+
                    if(hrs > 8){
                        total += hrs - 8;
                    }
                }
            }
        }else{
            throw new EmpregadoNaoExisteException();
        }
        return String.valueOf(total);
    }
    //------------------------------------------------------
    public void lancaVenda(String id,String data,String valor) throws EmpregadoNaoExisteException{
        if(banco.bancoDeHoras.containsKey(id)){
            banco.bancoDeHoras.get(id).put(data,valor);
        }else{
            throw new EmpregadoNaoExisteException();
        }
    }
    //------------------------------------------------------
    public String getVendasRealizadas(String id,String data_inicial,String data_final) throws EmpregadoNaoExisteException{
        double total = 0;
        if(banco.bancoDeHoras.containsKey(id) && banco.empregados.get(id).getTipo().equals("comissionado")){
            for(Map.Entry<String,String> e: banco.bancoDeHoras.get(id).entrySet()){
                LocalDate data =  LocalDate.parse(e.getKey(),formatter);
                if(!data.isBefore(LocalDate.parse(data_final,formatter))){
                    return String.valueOf(total);
                }
                if(!data.isBefore(LocalDate.parse(data_inicial,formatter))){
                    total += Double.parseDouble(e.getValue());
                }
            }
        }else{
            throw new EmpregadoNaoExisteException();
        }
        return String.valueOf(total);
    }
    //-----------------------------------------------------------

}

