package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.*;
import br.ufal.ic.p2.wepayu.models.Comissionado;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.Horista;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Map;

public class Facade {
    Banco banco = new Banco();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    //-------------------------------------------------------
    public void encerrarSistema(){
        banco.salvar();
    }
    public void zerarSistema(){
        banco.clear();
    }
    //-------------------------------------------------------
    public String criarEmpregado(String nome,String endereco,String tipo,String salario) throws Exception{
        if(nome.isEmpty()) throw new NomeNuloException();
        if(endereco.isEmpty()) throw new EnderecoNuloException();
        if(salario.isEmpty()) throw new SalarioNuloException();
        if(salario.replace(",",".").chars().anyMatch(Character::isLetter)) throw new SalarioNaoNumericoException();
        if(Double.parseDouble(salario.replace(",",".")) <= 0) throw new SalarioNegativoException();
        if(!(tipo.equals("assalariado") || tipo.equals("horista"))){
            if(tipo.equals("comissionado")){
                throw new TipoNaoAplicavelException();
            }else{
                throw new TipoInvalidoException();
            }
        }
        if(tipo.equals("horista")){
            banco.add(new Horista(nome,endereco,tipo,salario));
        }else{
            banco.add(new Empregado(nome,endereco,tipo,salario));
        }
        return  banco.empregados.lastEntry().getKey();
    }
    //-------------------------------------------------------
    public String criarEmpregado(String nome,String endereco,String tipo,String salario,String comissao) throws Exception{
        if(nome.isEmpty()) throw new NomeNuloException();
        if(endereco.isEmpty()) throw new EnderecoNuloException();
        if(salario.isEmpty()) throw new SalarioNuloException();
        if(salario.replace(",","").chars().anyMatch(Character::isLetter)) throw new SalarioNaoNumericoException();
        if(Double.parseDouble(salario.replace(",",".")) <= 0) throw new SalarioNegativoException();
        if(comissao.isEmpty()) throw new ComissaoNulaException();
        if(comissao.replace(",","").chars().anyMatch(Character::isLetter)) throw new ComissaoNaoNumericaException();
        if(Double.parseDouble(comissao.replace(",",".")) <= 0) throw new ComissaoNegativaException();
        if(tipo.equals("assalariado") || tipo.equals("horista") || tipo.equals("comissionado")){
            if(!tipo.equals("comissionado")){
                throw new TipoNaoAplicavelException();
            }
        }else{
                throw new TipoInvalidoException();
        }
        banco.add(new Comissionado(nome,endereco,tipo,salario,comissao));
        return banco.empregados.lastEntry().getKey();
    }
    //--------------------------------------------------------
    public String getEmpregadoPorNome(String nome,int indice) throws Exception{
        for(Map.Entry<String,Empregado> e: banco.empregados.entrySet()){
            if(e.getValue().getNome().equals(nome)){
                indice--;
                if(indice <= 0){
                    return e.getKey();
                }
            }
        }
        throw new SemNomeException();
    }
    //-------------------------------------------------------
    public String getAtributoEmpregado(String id,String atributo) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(banco.empregados.containsKey(id)){
            Empregado e =  banco.empregados.get(id);
            if(atributo.equals("nome")){
                return e.getNome();
            }
            else if(atributo.equals("endereco")){
                return e.getEndereco();
            }
            else if(atributo.equals("tipo")){
                return e.getTipo();
            }
            else if(atributo.equals("salario")){
                return e.getSalario();
            }else if(e instanceof Comissionado && atributo.equals("comissao")){
                return ((Comissionado) e).getComissao();
            }else if (atributo.equals("sindicalizado")){
                return Boolean.toString(e.sindicalizado);
            }else{
                throw new AtributoInexistenteException();
            }
        }else{
           throw new EmpregadoNaoExisteException();
        }
    }
    //-----------------------------------------------------
    public void alteraEmpregado(String id,String atributo,String valor)throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(banco.empregados.containsKey(id)){
            Empregado e =  banco.empregados.get(id);
            if(atributo.equals("nome")){
                e.nome = valor;
            }
            else if(atributo.equals("endereco")){
                e.endereco = valor;
            }
            else if(atributo.equals("salario")){
                e.salario = valor;
            }else if(e instanceof Comissionado && atributo.equals("comissao")){
                ((Comissionado) e).comissao = valor;
            }else if(atributo.equals("sindicalizado")){
                e.sindicalizado = false;
                if(banco.sindicato.containsKey(e.id_sindicato)){
                    banco.sindicato.remove(e.id_sindicato);
                }
                e.id_sindicato = null;
                e.taxa_sindical = null;
            }
        }else{
            throw new EmpregadoNaoExisteException();
        }
    }
    public void alteraEmpregado(String id,String atributo,String valor,String id_sindicato,String taxa_sindical) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(banco.empregados.containsKey(id)){
            Empregado e =  banco.empregados.get(id);
            e.sindicalizado = true;
            e.id_sindicato = id_sindicato;
            e.taxa_sindical = taxa_sindical;
            banco.add_sindicato(id_sindicato);
        }else{
            throw new EmpregadoNaoExisteException();
        }
    }

    //------------------------------------------------------
    public void removerEmpregado(String id) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(banco.empregados.containsKey(id)){
            banco.empregados.remove(id);
        }else {
            throw new EmpregadoNaoExisteException();
        }
    }
    //--------------------------------------------------------
    public void lancaCartao(String id,String data,String hora) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        if(!(banco.empregados.get(id) instanceof Horista)) throw new NaoHoristaException();
        try{
            LocalDate.parse(data, formatter);
        }
        catch(DateTimeParseException e){
            throw new DataInvalidaException();
        }
        if(Double.parseDouble(hora.replace(",","")) <= 0) throw new HoraNegativaException();
        banco.bancoDeHoras.get(id).put(data,hora);
    }
    //------------------------------------------------------
    public String getHorasNormaisTrabalhadas(String id,String data_inicial,String data_final) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        if(!(banco.empregados.get(id) instanceof Horista)) throw new NaoHoristaException();
        try{
            LocalDate.parse(data_inicial, formatter);
        }
        catch(DateTimeParseException e){
            throw new DataInicialInvalidaException();
        }
        try{
            LocalDate.parse(data_final, formatter);
        }
        catch(DateTimeParseException e){
            throw new DataFinalInvalidaException();
        }
        if(LocalDate.parse(data_inicial, formatter).isAfter(LocalDate.parse(data_final, formatter))) throw new DatasInvalidasException();
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
    public String getHorasExtrasTrabalhadas(String id,String data_inicial,String data_final) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        if(!(banco.empregados.get(id) instanceof Horista)) throw new NaoHoristaException();
        try{
            LocalDate.parse(data_inicial, formatter);
        }
        catch(DateTimeParseException e){
            throw new DataInicialInvalidaException();
        }
        try{
            LocalDate.parse(data_final, formatter);
        }
        catch(DateTimeParseException e){
            throw new DataFinalInvalidaException();
        }
        if(LocalDate.parse(data_inicial, formatter).isAfter(LocalDate.parse(data_final, formatter))) throw new DatasInvalidasException();
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
    public void lancaVenda(String id,String data,String valor) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(banco.bancoDeHoras.containsKey(id) && banco.empregados.get(id) instanceof Comissionado){
            banco.bancoDeHoras.get(id).put(data,valor);
        }else{
            throw new EmpregadoNaoExisteException();
        }
    }
    //------------------------------------------------------
    public String getVendasRealizadas(String id,String data_inicial,String data_final) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
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
    public void lancaTaxaServico (String id,String data,String valor) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(banco.sindicato.containsKey(id)){
            banco.sindicato.get(id).put(data,valor);
        }else{
            throw new EmpregadoNaoExisteException();
        }
    }
    //----------------------------------------------------------------------
    public String getTaxasServico(String id,String data_inicial,String data_final) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        double total_d = 0;
        String id_s = banco.empregados.get(id).id_sindicato;
        if(banco.empregados.get(id).sindicalizado && banco.sindicato.containsKey(id_s)){
            for(Map.Entry<String,String> e: banco.sindicato.get(id_s).entrySet()){
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

