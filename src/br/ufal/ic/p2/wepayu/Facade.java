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
            }else if(atributo.equals("comissao")){
                if(!(e instanceof Comissionado)) throw new NaoComissionadoException();
                else return ((Comissionado)e).getComissao();
            }else if (atributo.equals("sindicalizado")){
                return Boolean.toString(e.sindicalizado);
            }else if(atributo.equals("idSindicato")){
                if(!e.sindicalizado) throw new NaoSindicalizadoException();
                else return e.id_sindicato;
            }else if(atributo.equals("taxaSindical")){
                if(!e.sindicalizado) throw new NaoSindicalizadoException();
                else return e.getTaxa();
            }else if(atributo.equals("metodoPagamento")){
                return e.metodoPagamento;
            }else if(atributo.equals("banco")){
                if(!e.metodoPagamento.equals("banco")) throw new NaoBancoException();
                else return e.banco;
            }else if(atributo.equals("agencia")){
                if(!e.metodoPagamento.equals("banco")) throw new NaoBancoException();
                else return e.agencia;
            }else if(atributo.equals("contaCorrente")){
                if(!e.metodoPagamento.equals("banco")) throw new NaoBancoException();
                else return e.contaCorrente;
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
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        Empregado e =  banco.empregados.get(id);
        if(atributo.equals("nome")){
            if(valor.isEmpty()) throw new NomeNuloException();
            e.nome = valor;
        }
        else if(atributo.equals("endereco")){
            if(valor.isEmpty()) throw new EnderecoNuloException();
            e.endereco = valor;
        }
        else if(atributo.equals("salario")){
            if(valor.isEmpty()) throw new SalarioNuloException();
            if(valor.replace(",","").chars().anyMatch(Character::isLetter)) throw new SalarioNaoNumericoException();
            if(Double.parseDouble(valor.replace(",",".")) <= 0) throw new SalarioNegativoException();
            e.salario = valor;
        }else if(atributo.equals("comissao")){
            if(!(e instanceof Comissionado)) throw new NaoComissionadoException();
            if(valor.isEmpty()) throw new ComissaoNulaException();
            if(valor.replace(",","").chars().anyMatch(Character::isLetter)) throw new ComissaoNaoNumericaException();
            if(Double.parseDouble(valor.replace(",",".")) <= 0) throw new ComissaoNegativaException();
            ((Comissionado) e).comissao = valor;
        }else if(atributo.equals("tipo")){
            if(!(valor.equals("assalariado") || valor.equals("horista") || valor.equals("comissionado"))) throw new TipoInvalidoException();
            banco.trocar_tipo(id,valor,e.salario);
        }else if(atributo.equals("sindicalizado")){
            if(!valor.equals("false")) throw new SindicalizadoInvalidoException();
            e.sindicalizado = false;
            banco.sindicato.remove(e.id_sindicato);
            e.id_sindicato = null;
            e.taxa_sindical = null;
        }else if(atributo.equals("metodoPagamento")){
            if(!(valor.equals("emMaos") || valor.equals("correios"))) throw new MetodoInvalidoException();
            e.metodoPagamento = valor;
            e.banco = null;
            e.agencia = null;
            e.contaCorrente = null;
        }else{
            throw new AtributoInexistenteException();
        }
    }
    public void alteraEmpregado(String id,String atributo,String valor,String id_sindicato,String taxa_sindical) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!valor.equals("true")) throw new SindicalizadoInvalidoException();
        if(id_sindicato.isEmpty()) throw new IdentificacaoSindicatoNulaException();
        if(taxa_sindical.isEmpty()) throw new TaxaSindicalNulaException();
        if(taxa_sindical.replace(",","").chars().anyMatch(Character::isLetter)) throw new TaxaSindicalNaoNumericaException();
        if(Double.parseDouble(taxa_sindical.replace(",",".")) <= 0) throw new TaxaSindicalNegativaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        if(banco.sindicato.containsKey(id_sindicato)) throw new IdentificacaoSindicatoJaExistenteException();
        Empregado e = banco.empregados.get(id);
        e.sindicalizado = true;
        e.id_sindicato = id_sindicato;
        e.taxa_sindical = taxa_sindical;
        banco.add_sindicato(id_sindicato);
    }
    //-----------------------------------------------------
    public void alteraEmpregado(String id,String metodo,String valor1,String banco_Nome,String agencia,String contaCorrente) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        if(!valor1.equals("banco")) throw new MetodoInvalidoException();
        if(banco_Nome.isEmpty()) throw new BancoNuloException();
        if(agencia.isEmpty()) throw new AgenciaNulaException();
        if(contaCorrente.isEmpty()) throw new ContaNulaException();
        Empregado e = banco.empregados.get(id);
        e.metodoPagamento = valor1;
        e.banco = banco_Nome;
        e.agencia = agencia;
        e.contaCorrente = contaCorrente;
    }
    //------------------------------------------------------
    public void alteraEmpregado(String id, String atributo,String tipo_novo,String valor) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        if(!(tipo_novo.equals("assalariado") || tipo_novo.equals("horista") || tipo_novo.equals("comissionado"))) throw new TipoInvalidoException();
        banco.trocar_tipo(id,tipo_novo,valor);
    }
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
        return String.valueOf(total).replace(".",",").replace(",0","");
    }
    //------------------------------------------------------
    public void lancaVenda(String id,String data,String valor) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        if(!(banco.empregados.get(id) instanceof Comissionado)) throw new NaoComissionadoException();
        try{
            LocalDate.parse(data, formatter);
        }
        catch(DateTimeParseException e){
            throw new DataInvalidaException();
        }
        if(valor.replace(",","").chars().anyMatch(Character::isLetter)) throw new ValorNaoNumericoException();
        if(Double.parseDouble(valor.replace(",",".")) <= 0) throw new ValorNegativoException();
        banco.bancoDeHoras.get(id).put(data,valor);
    }
    //------------------------------------------------------
    public String getVendasRealizadas(String id,String data_inicial,String data_final) throws Exception{
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        if(!(banco.empregados.get(id) instanceof Comissionado)) throw new NaoComissionadoException();
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
        double total_d = 0;
        for(Map.Entry<String,String> e: banco.bancoDeHoras.get(id).entrySet()){
            LocalDate data =  LocalDate.parse(e.getKey(),formatter);
            if(!data.isBefore(LocalDate.parse(data_final,formatter))){
                break;
            }
            if(!data.isBefore(LocalDate.parse(data_inicial,formatter))){
                total_d += Double.parseDouble(e.getValue().replace(",","."));
            }
        }
        String total_s = String.valueOf(total_d);
        return total_s.replace(".",",") + "0";
    }
    //-----------------------------------------------------------
    public void lancaTaxaServico (String id,String data,String valor) throws Exception{
        if(id.isEmpty()) throw new MembroNuloException();
        if(!banco.sindicato.containsKey(id)) throw new MembroInexistenteException();
        try{
            LocalDate.parse(data, formatter);
        }
        catch(DateTimeParseException e){
            throw new DataInvalidaException();
        }
        if(valor.replace(",","").chars().anyMatch(Character::isLetter)) throw new ValorNaoNumericoException();
        if(Double.parseDouble(valor.replace(",",".")) <= 0) throw new ValorNegativoException();
        banco.sindicato.get(id).put(data,valor);
    }
    //----------------------------------------------------------------------
    public String getTaxasServico(String id,String data_inicial,String data_final) throws Exception{
        if(id.isEmpty()) throw new MembroNuloException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        Empregado emp = banco.empregados.get(id);
        if(!emp.sindicalizado) throw new NaoSindicalizadoException();
        String id_s = emp.id_sindicato;
        if(!banco.sindicato.containsKey(id_s)) throw new MembroInexistenteException();
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
        double total_d = 0;
        for(Map.Entry<String,String> e: banco.sindicato.get(id_s).entrySet()){
            LocalDate data =  LocalDate.parse(e.getKey(),formatter);
            if(!data.isBefore(LocalDate.parse(data_final,formatter))){
                break;
            }
            if(!data.isBefore(LocalDate.parse(data_inicial,formatter))){
                total_d += Double.parseDouble(e.getValue().replace(",","."));
            }
        }
        String total_s = String.valueOf(total_d);
        return total_s.replace(".",",") + "0";
    }
    //--------------------------------------------------------------------------------

}

