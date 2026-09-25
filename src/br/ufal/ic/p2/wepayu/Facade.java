package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.models.Comissionado;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.Horista;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class Facade {
    Banco banco = new Banco();
    //-------------------------------------------------------
    public void encerrarSistema(){
        banco.salvar();
    }
    public void zerarSistema(){
        banco.clear();
    }
    //-------------------------------------------------------
    public String criarEmpregado(String nome,String endereco,String tipo,String salario){
        if(tipo.equals("horista")){
            banco.add(new Horista(nome,endereco,tipo,salario));
        }else if(tipo.equals("assalariado")){
            banco.add(new Empregado(nome,endereco,tipo,salario));
        }
        return  banco.empregados.lastEntry().getKey();
    }
    //-------------------------------------------------------
    public String criarEmpregado(String nome,String endereco,String tipo,String salario,String comissao){
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
    public String getAtributoEmpregado(String id,String atributo) throws EmpregadoNaoExisteException{
        if(banco.empregados.containsKey(id)){
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
            }else if(banco.empregados.get(id) instanceof Comissionado && atributo.equals("comissao")){
                return ((Comissionado) banco.empregados.get(id)).getComissao();
            }
        }else{
           throw new EmpregadoNaoExisteException();
        }
        return "false";
    }
    //-----------------------------------------------------
    public void alteraEmpregado(String id,String atributo,String valor)throws EmpregadoNaoExisteException{
        if(banco.empregados.containsKey(id)){
            if(atributo.equals("nome")){
                banco.empregados.get(id).nome = valor;
            }
            else if(atributo.equals("endereco")){
                banco.empregados.get(id).endereco = valor;
            }
            else if(atributo.equals("salario")){
                banco.empregados.get(id).salario = valor;
            }else if(banco.empregados.get(id) instanceof Comissionado && atributo.equals("comissao")){
                ((Comissionado) banco.empregados.get(id)).comissao = valor;
            }else if(atributo.equals("sindicalizado")){
                banco.empregados.get(id).sindicalizado = false;
            }
        }else{
            throw new EmpregadoNaoExisteException();
        }
    }
    public void alteraEmpregado(String id,String valor,String id_sindicato,String taxa_sindical){
        banco.empregados.get(id).sindicalizado = true;
        banco.empregados.get(id).id_sindicato = id_sindicato;
        banco.empregados.get(id).taxa_sindical = taxa_sindical;
        banco.add_sindicato(id_sindicato);
    }

    //------------------------------------------------------
    public void removerEmpregado(String id) throws EmpregadoNaoExisteException{
        if(banco.empregados.containsKey(id)){
            banco.empregados.remove(id);
        }else {
            throw new EmpregadoNaoExisteException();
        }
    }
    //--------------------------------------------------------
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
    public void lancaCartao(String id,String data,String hora) throws EmpregadoNaoExisteException{
        if(banco.bancoDeHoras.containsKey(id) && banco.empregados.get(id) instanceof Horista){
            banco.bancoDeHoras.get(id).put(data,hora);
        }else{
            throw new EmpregadoNaoExisteException();
        }
    }
    //------------------------------------------------------
    public String getHorasNormaisTrabalhadas(String id,String data_inicial,String data_final) throws EmpregadoNaoExisteException{
        double total = 0;
        if(banco.bancoDeHoras.containsKey(id)){
            for(Map.Entry<String,String> e: banco.bancoDeHoras.get(id).entrySet()){
                LocalDate data =  LocalDate.parse(e.getKey(),formatter);
                if(!data.isBefore(LocalDate.parse(data_final,formatter))){
                    break;
                }
                if(!data.isBefore(LocalDate.parse(data_inicial,formatter))){
                    double hrs = Double.parseDouble(e.getValue().replace(",","."));
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
        return String.valueOf(total).replace(".",",").replace(",0","");
    }
    //---------------------------------------------------------
    public String getHorasExtrasTrabalhadas(String id,String data_inicial,String data_final) throws EmpregadoNaoExisteException{
        double total = 0;
        if(banco.bancoDeHoras.containsKey(id) && banco.empregados.get(id).getTipo().equals("horista")){
            for(Map.Entry<String,String> e: banco.bancoDeHoras.get(id).entrySet()){
                LocalDate data =  LocalDate.parse(e.getKey(),formatter);
                if(!data.isBefore(LocalDate.parse(data_final,formatter))){
                    break;
                }
                if(!data.isBefore(LocalDate.parse(data_inicial,formatter))){
                    double hrs = Double.parseDouble(e.getValue().replace(",","."));
                    if(hrs > 8){
                        total += hrs - 8;
                    }
                }
            }
        }else{
            throw new EmpregadoNaoExisteException();
        }
        return String.valueOf(total).replace(".",",").replace(",0","");
    }
    //------------------------------------------------------
    public void lancaVenda(String id,String data,String valor) throws EmpregadoNaoExisteException{
        if(banco.bancoDeHoras.containsKey(id) && banco.empregados.get(id) instanceof Comissionado){
            banco.bancoDeHoras.get(id).put(data,valor);
        }else{
            throw new EmpregadoNaoExisteException();
        }
    }
    //------------------------------------------------------
    public String getVendasRealizadas(String id,String data_inicial,String data_final) throws EmpregadoNaoExisteException{
        double total_d = 0;
        if(banco.bancoDeHoras.containsKey(id) && banco.empregados.get(id) instanceof Comissionado){
            for(Map.Entry<String,String> e: banco.bancoDeHoras.get(id).entrySet()){
                LocalDate data =  LocalDate.parse(e.getKey(),formatter);
                if(!data.isBefore(LocalDate.parse(data_final,formatter))){
                    break;
                }
                if(!data.isBefore(LocalDate.parse(data_inicial,formatter))){
                    total_d += Double.parseDouble(e.getValue().replace(",","."));
                }
            }
        }else{
            throw new EmpregadoNaoExisteException();
        }
        String total_s = String.valueOf(total_d);
        return total_s.replace(".",",") + "0";
    }
    //-----------------------------------------------------------
    public void lancaTaxaServico (String id,String data,String valor) throws EmpregadoNaoExisteException{
        if(banco.empregados.containsKey(id) && banco.empregados.get(id).sindicalizado){
            String id_s = banco.empregados.get(id).id_sindicato;
            banco.sindicato.get(id_s).put(data,valor);
        }else{
            throw new EmpregadoNaoExisteException();
        }
    }
    //----------------------------------------------------------------------
    public String getTaxasServico(String id,String data_inicial,String data_final) throws EmpregadoNaoExisteException{
        double total_d = 0;
        if(banco.bancoDeHoras.containsKey(id) && banco.empregados.get(id).sindicalizado){
            for(Map.Entry<String,String> e: banco.sindicato.get(id).entrySet()){
                LocalDate data =  LocalDate.parse(e.getKey(),formatter);
                if(!data.isBefore(LocalDate.parse(data_final,formatter))){
                    break;
                }
                if(!data.isBefore(LocalDate.parse(data_inicial,formatter))){
                    total_d += Double.parseDouble(e.getValue().replace(",","."));
                }
            }
        }else{
            throw new EmpregadoNaoExisteException();
        }
        String total_s = String.valueOf(total_d);
        return total_s.replace(".",",") + "0";
    }
    //--------------------------------------------------------------------------------

}

