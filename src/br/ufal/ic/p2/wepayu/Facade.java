package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.*;
import br.ufal.ic.p2.wepayu.models.Comissionado;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.Horista;

import java.io.PrintWriter;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Facade {
    private static class RegistroHistorico {
        String antes;
        String depois;
        RegistroHistorico(String antes, String depois) {
            this.antes = antes;
            this.depois = depois;
        }
    }
    Banco banco = new Banco();
    Deque<RegistroHistorico> undo = new ArrayDeque<>();
    Deque<RegistroHistorico> redo = new ArrayDeque<>();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    boolean encerrado = false;
    //-------------------------------------------------------
    private String iniciarComando() throws Exception {
        if (encerrado) {
            throw new Exception("Nao pode dar comandos depois de encerrarSistema.");
        }

        return banco.snapshot();
    }
    //-------------------------------------------------------------------------------
    private void finalizarComando(String estadoAnterior) {
        String estadoDepois = banco.snapshot();
        undo.push(new RegistroHistorico(estadoAnterior, estadoDepois));
        redo.clear();
    }
    //--------------------------------------------------------------------------------
    private String formatar(double valor) {
        return String.format(Locale.US, "%.2f", valor).replace(".", ",");
    }
    //-----------------------------------------------------------------------------------
    private boolean recebeuNaFolhaHorista(String id, LocalDate dataFolha) {
        Map<String, String> registros = banco.bancoDeHoras.get(id);
        if (registros == null) {
            return false;
        }
        for (Map.Entry<String, String> registro : registros.entrySet()) {
            LocalDate data = LocalDate.parse(registro.getKey(), formatter);
            long periodo = ChronoUnit.DAYS.between(data, dataFolha);
            if (periodo > 0 && periodo < 7) {
                double horas = Double.parseDouble(
                        registro.getValue().replace(",", ".")
                );
                if (horas > 0) {
                    return true;
                }
            }
        }
        return false;
    }
    //------------------------------------------------------------------------------------
    private LocalDate ultimaDataPagamentoHorista(String id, LocalDate dataFolha) {
        Map<String, String> registros = banco.bancoDeHoras.get(id);
        LocalDate primeiraData = null;
        for (String dataStr : registros.keySet()) {
            LocalDate data = LocalDate.parse(dataStr, formatter);

            if (primeiraData == null || data.isBefore(primeiraData)) {
                primeiraData = data;
            }
        }
        LocalDate candidata = dataFolha.minusDays(7);
        while (!candidata.isBefore(primeiraData)) {

            if (recebeuNaFolhaHorista(id, candidata)) {
                return candidata;
            }

            candidata = candidata.minusDays(7);
        }
        return null;
    }
    //-------------------------------------------------------
    public void encerrarSistema(){
        banco.salvar();
        encerrado = true;
    }
    //--------------------------------------------------------
    public void zerarSistema() throws Exception {
        String estadoAnterior = iniciarComando();
        banco.clear();
        finalizarComando(estadoAnterior);
    }
    //----------------------------------------------------------------------------------------
    public String criarEmpregado(String nome,String endereco,String tipo,String salario) throws Exception{
        String estadoAnterior = iniciarComando();
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
        finalizarComando(estadoAnterior);
        return  banco.empregados.lastEntry().getKey();
    }
    //-------------------------------------------------------
    public String criarEmpregado(String nome,String endereco,String tipo,String salario,String comissao) throws Exception{
        String estadoAnterior = iniciarComando();
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
        finalizarComando(estadoAnterior);
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
            switch (atributo) {
                case "nome" -> {
                    return e.getNome();
                }
                case "endereco" -> {
                    return e.getEndereco();
                }
                case "tipo" -> {
                    return e.getTipo();
                }
                case "salario" -> {
                    return e.getSalario();
                }
                case "comissao" -> {
                    if (!(e instanceof Comissionado)) throw new NaoComissionadoException();
                    return ((Comissionado) e).getComissao();
                }
                case "sindicalizado" -> {
                    return Boolean.toString(e.sindicalizado);
                }
                case "idSindicato" -> {
                    if (!e.sindicalizado) throw new NaoSindicalizadoException();
                    return e.id_sindicato;
                }
                case "taxaSindical" -> {
                    if (!e.sindicalizado) throw new NaoSindicalizadoException();
                    return e.getTaxa();
                }
                case "metodoPagamento" -> {
                    return e.metodoPagamento;
                }
                case "banco" -> {
                    if (!e.metodoPagamento.equals("banco")) throw new NaoBancoException();
                    return e.banco;
                }
                case "agencia" -> {
                    if (!e.metodoPagamento.equals("banco")) throw new NaoBancoException();
                    return e.agencia;
                }
                case "contaCorrente" -> {
                    if (!e.metodoPagamento.equals("banco")) throw new NaoBancoException();
                    return e.contaCorrente;
                }
                default -> throw new AtributoInexistenteException();
            }
        }else{
           throw new EmpregadoNaoExisteException();
        }
    }
    //-----------------------------------------------------
    public void alteraEmpregado(String id,String atributo,String valor)throws Exception{
        String estadoAnterior = iniciarComando();
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        Empregado e =  banco.empregados.get(id);
        switch (atributo) {
            case "nome" -> {
                if (valor.isEmpty()) throw new NomeNuloException();
                e.nome = valor;
            }
            case "endereco" -> {
                if (valor.isEmpty()) throw new EnderecoNuloException();
                e.endereco = valor;
            }
            case "salario" -> {
                if (valor.isEmpty()) throw new SalarioNuloException();
                if (valor.replace(",", "").chars().anyMatch(Character::isLetter))
                    throw new SalarioNaoNumericoException();
                if (Double.parseDouble(valor.replace(",", ".")) <= 0) throw new SalarioNegativoException();
                e.salario = valor;
            }
            case "comissao" -> {
                if (!(e instanceof Comissionado)) throw new NaoComissionadoException();
                if (valor.isEmpty()) throw new ComissaoNulaException();
                if (valor.replace(",", "").chars().anyMatch(Character::isLetter))
                    throw new ComissaoNaoNumericaException();
                if (Double.parseDouble(valor.replace(",", ".")) <= 0) throw new ComissaoNegativaException();
                ((Comissionado) e).comissao = valor;
            }
            case "tipo" -> {
                if (!(valor.equals("assalariado") || valor.equals("horista") || valor.equals("comissionado")))
                    throw new TipoInvalidoException();
                banco.trocar_tipo(id, valor, e.salario);
            }
            case "sindicalizado" -> {
                if (!valor.equals("false")) throw new SindicalizadoInvalidoException();
                e.sindicalizado = false;
                banco.sindicato.remove(e.id_sindicato);
                e.id_sindicato = null;
                e.taxa_sindical = null;
            }
            case "metodoPagamento" -> {
                if (!(valor.equals("emMaos") || valor.equals("correios"))) throw new MetodoInvalidoException();
                e.metodoPagamento = valor;
                e.banco = null;
                e.agencia = null;
                e.contaCorrente = null;
            }
            default -> throw new AtributoInexistenteException();
        }
        finalizarComando(estadoAnterior);
    }
    public void alteraEmpregado(String id,String atributo,String valor,String id_sindicato,String taxa_sindical) throws Exception{
        String estadoAnterior = iniciarComando();
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
        finalizarComando(estadoAnterior);
    }
    //-----------------------------------------------------
    public void alteraEmpregado(String id,String metodo,String valor1,String banco_Nome,String agencia,String contaCorrente) throws Exception{
        String estadoAnterior = iniciarComando();
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
        finalizarComando(estadoAnterior);
    }
    //------------------------------------------------------
    public void alteraEmpregado(String id, String atributo,String tipo_novo,String valor) throws Exception{
        String estadoAnterior = iniciarComando();
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        if(!(tipo_novo.equals("assalariado") || tipo_novo.equals("horista") || tipo_novo.equals("comissionado"))) throw new TipoInvalidoException();
        banco.trocar_tipo(id,tipo_novo,valor);
        finalizarComando(estadoAnterior);
    }
    //--------------------------------------------------------
    public void removerEmpregado(String id) throws Exception{
        String estadoAnterior = iniciarComando();
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(banco.empregados.containsKey(id)){
            banco.sindicato.remove(banco.empregados.get(id).id_sindicato);
            banco.bancoDeHoras.remove(id);
            banco.empregados.remove(id);
        }else {
            throw new EmpregadoNaoExisteException();
        }
        finalizarComando(estadoAnterior);
    }
    //--------------------------------------------------------
    public void lancaCartao(String id,String data,String hora) throws Exception{
        String estadoAnterior = iniciarComando();
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
        finalizarComando(estadoAnterior);
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
        String estadoAnterior = iniciarComando();
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
        finalizarComando(estadoAnterior);
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
        String estadoAnterior = iniciarComando();
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
        finalizarComando(estadoAnterior);
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
    //-------------------------------------------------------------------------------------
    public void rodaFolha(String data,String saida) throws Exception{
        String estadoAnterior = iniciarComando();
        DateTimeFormatter entrada = DateTimeFormatter.ofPattern("d/M/uuuu");
        LocalDate data_l = LocalDate.parse(data,entrada);
        StringBuilder folha = new StringBuilder();
        folha.append("FOLHA DE PAGAMENTO DO DIA ").append(data_l).append("\n");
        folha.append("====================================\n");
        LocalDate data_folha = LocalDate.parse(data,formatter);
        double total = 0;
        boolean fim_de_mes = false;
        boolean quinzena = false;
        boolean sexta = false;
        SequencedMap<String,Empregado> horistas = new LinkedHashMap<>();
        SequencedMap<String,Empregado> assalariados = new LinkedHashMap<>();
        SequencedMap<String,Empregado> comissionados = new LinkedHashMap<>();
        if(data_folha.getDayOfMonth() == data_folha.lengthOfMonth()) fim_de_mes = true;
        if(ChronoUnit.DAYS.between(LocalDate.of(2005,1,1), data_folha) % 14 == 13) quinzena = true;
        if(data_folha.getDayOfWeek() == DayOfWeek.FRIDAY)  sexta = true;
        for(Map.Entry<String, Empregado> id_emp : banco.empregados.entrySet()){
            String id = id_emp.getKey();
            Empregado e = id_emp.getValue();
            if(e instanceof  Horista && sexta){
                horistas.put(id,e);
            }else if(e.getClass().equals(Empregado.class) && fim_de_mes){
                assalariados.put(id,e);
            }else if(e instanceof Comissionado && quinzena){
                comissionados.put(id,e);
            }
        }
        List<Map.Entry<String, Empregado>> listaHoristas = new ArrayList<>(horistas.entrySet());
        listaHoristas.sort(Comparator.comparing(entry -> entry.getValue().nome));
        List<Map.Entry<String, Empregado>> listaAssalariados = new ArrayList<>(assalariados.entrySet());
        listaAssalariados.sort(Comparator.comparing(entry -> entry.getValue().nome));
        List<Map.Entry<String, Empregado>> listaComissionados = new ArrayList<>(comissionados.entrySet());
        listaComissionados.sort(Comparator.comparing(entry -> entry.getValue().nome));
        folha.append("""
                
                ===============================================================================================================================
                ===================== HORISTAS ================================================================================================
                ===============================================================================================================================
                Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo
                ==================================== ===== ===== ============= ========= =============== ======================================
                """);
        double total_hora = 0;
        double total_extra = 0;
        double total_bruto = 0;
        double total_desconto = 0;
        double total_liquido = 0;
        for(Map.Entry<String,Empregado> horista: listaHoristas){
            double salario_liquido;
            double salario_bruto = 0;
            double descontos = 0;
            String id = horista.getKey();
            Empregado e = horista.getValue();
            double horas = 0;
            double extras = 0;
            for(Map.Entry<String, String> data_valor : banco.bancoDeHoras.get(id).entrySet()){
                LocalDate data_i =  LocalDate.parse(data_valor.getKey(),formatter);
                long periodo = ChronoUnit.DAYS.between(data_i,data_folha);
                if(periodo > 0 && periodo < 7){
                    double hrs = Double.parseDouble(data_valor.getValue().replace(",","."));
                    double salario_hora = Double.parseDouble(e.salario.replace(",","."));
                    if(hrs > 8){
                        horas += 8;
                        total_hora += 8;
                        extras += hrs - 8;
                        total_extra += hrs - 8;
                        salario_bruto += salario_hora * 8 + salario_hora * 1.5 * (hrs - 8);
                    }else{
                        horas += hrs;
                        total_hora += hrs;
                        salario_bruto += salario_hora * hrs;
                    }
                }
            }
            if(e.sindicalizado && salario_bruto > 0){
                double taxa = Double.parseDouble(e.taxa_sindical.replace(",","."));
                LocalDate ultimaDataPagamento = ultimaDataPagamentoHorista(id,data_folha);
                if(ultimaDataPagamento == null){
                    LocalDate primeiraData = null;
                    for(String dataStr : banco.bancoDeHoras.get(id).keySet()){
                        LocalDate ponto = LocalDate.parse(dataStr,formatter);
                        if(primeiraData == null || ponto.isBefore(primeiraData)){
                            primeiraData = ponto;
                        }
                    }
                    assert primeiraData != null;
                    long dias = ChronoUnit.DAYS.between(primeiraData,data_folha) + 1;
                    descontos += taxa * dias;
                }else{
                    long dias = ChronoUnit.DAYS.between(ultimaDataPagamento,data_folha);
                    descontos += taxa * dias;
                }
                for(Map.Entry<String,String> data_valor : banco.sindicato.get(e.id_sindicato).entrySet()){
                    LocalDate data_i = LocalDate.parse(data_valor.getKey(),formatter);
                    long periodo = ChronoUnit.DAYS.between(data_i,data_folha);
                    if(periodo > 0 && periodo < 7){
                        double taxa_servico = Double.parseDouble(data_valor.getValue().replace(",","."));
                        descontos += taxa_servico;
                    }
                }
            }
            salario_liquido = salario_bruto - descontos;
            if(salario_liquido < 0){
                salario_liquido = 0;
                descontos = 0;
            }
            total += salario_bruto;
            total_bruto += salario_bruto;
            total_desconto += descontos;
            total_liquido += salario_liquido;
            String metodo;
            if(e.metodoPagamento.equals("emMaos")){
                metodo = "Em maos";
            }else if(e.metodoPagamento.equals("correios")){
                metodo = "Correios, " + e.endereco;
            }else{
                metodo = e.banco + ", Ag. " + e.agencia + " CC " + e.contaCorrente;
            }
            folha.append(String.format(
                    Locale.US, "%-36s %5d %5d %13s %9s %15s %s%n", e.nome, (int) horas, (int) extras,
                    formatar(salario_bruto), formatar(descontos), formatar(salario_liquido), metodo));
        }
        folha.append(String.format(
                Locale.US,
                "\nTOTAL HORISTAS                       %5s %5s %13s %9s %15s\n", (int)total_hora, (int)total_extra,
                formatar(total_bruto), formatar(total_desconto), formatar(total_liquido)));
        folha.append("""
                
                ===============================================================================================================================
                ===================== ASSALARIADOS ============================================================================================
                ===============================================================================================================================
                Nome                                             Salario Bruto Descontos Salario Liquido Metodo
                ================================================ ============= ========= =============== ======================================
                """);
        total_bruto = 0;
        total_desconto = 0;
        total_liquido = 0;
        for(Map.Entry<String,Empregado> assalariado: listaAssalariados){
            double salario_liquido;
            double salario_bruto;
            double descontos = 0;
            Empregado e = assalariado.getValue();
            salario_bruto = Double.parseDouble(e.salario.replace(",","."));
            if(e.sindicalizado){
                double taxa = Double.parseDouble(e.taxa_sindical.replace(",","."));
                descontos += taxa * data_folha.lengthOfMonth();
                for(Map.Entry<String, String> data_valor : banco.sindicato.get(e.id_sindicato).entrySet()){
                    LocalDate data_i =  LocalDate.parse(data_valor.getKey(),formatter);
                    if(!data_i.equals(data_folha) && YearMonth.from(data_i).equals(YearMonth.from(data_folha))){
                        double taxa_servico = Double.parseDouble(data_valor.getValue().replace(",","."));
                        descontos += taxa_servico;
                    }
                }
            }
            salario_liquido = salario_bruto - descontos;
            total += salario_bruto;
            total_bruto += salario_bruto;
            total_desconto += descontos;
            total_liquido += salario_liquido;
            String metodo;
            if(e.metodoPagamento.equals("emMaos")){
                metodo = "Em maos";
            }else if(e.metodoPagamento.equals("correios")){
                metodo = "Correios, " + e.endereco;
            }else{
                metodo = e.banco + ", Ag. " + e.agencia + " CC " + e.contaCorrente;
            }
            folha.append(String.format(
                    Locale.US, "%-48s %13s %9s %15s %s%n", e.nome, formatar(salario_bruto), formatar(descontos),
                    formatar(salario_liquido), metodo));
        }
        folha.append(String.format(
                Locale.US,
                "\nTOTAL ASSALARIADOS                               %13s %9s %15s\n", formatar(total_bruto),
                formatar(total_desconto), formatar(total_liquido)
        ));
        folha.append("""
                
                ===============================================================================================================================
                ===================== COMISSIONADOS ===========================================================================================
                ===============================================================================================================================
                Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo
                ===================== ======== ======== ======== ============= ========= =============== ======================================
                """);

        double total_fixo = 0;
        double total_vendas = 0;
        double total_comissao = 0;
        total_bruto = 0;
        total_desconto = 0;
        total_liquido = 0;
        for(Map.Entry<String,Empregado> comissionado: listaComissionados){
            double salario_liquido;
            double salario_bruto = 0;
            double descontos = 0;
            String id = comissionado.getKey();
            Empregado e = comissionado.getValue();
            double salario_ano = Double.parseDouble(e.salario.replace(",","."));
            double fixo = Math.floor((salario_ano * 24 / 52) * 100) / 100;
            double vendas = 0;
            for(Map.Entry<String, String> data_valor : banco.bancoDeHoras.get(id).entrySet()){
                LocalDate data_i =  LocalDate.parse(data_valor.getKey(),formatter);
                long periodo = ChronoUnit.DAYS.between(data_i,data_folha);
                if(periodo > 0 && periodo < 14){
                    double venda = Double.parseDouble(data_valor.getValue().replace(",","."));
                    vendas += venda;
                }
            }
            if(e.sindicalizado){
                double taxa = Double.parseDouble(e.taxa_sindical.replace(",","."));
                descontos += taxa * 14;
                for(Map.Entry<String, String> data_valor : banco.sindicato.get(e.id_sindicato).entrySet()){
                    LocalDate data_i =  LocalDate.parse(data_valor.getKey(),formatter);
                    long periodo = ChronoUnit.DAYS.between(data_i,data_folha);
                    if(periodo > 0 && periodo < 14){
                        double taxa_servico = Double.parseDouble(data_valor.getValue().replace(",","."));
                        descontos += taxa_servico;
                    }
                }
            }
            double comissao = Double.parseDouble(((Comissionado) e).comissao.replace(",","."));
            double montante = Math.floor((vendas * comissao) * 100) / 100;
            total_fixo += fixo;
            total_vendas += vendas;
            total_comissao += montante;
            salario_bruto += montante + fixo;
            salario_liquido = salario_bruto - descontos;
            total += salario_bruto;
            total_bruto += salario_bruto;
            total_desconto += descontos;
            total_liquido += salario_liquido;
            String metodo;
            if(e.metodoPagamento.equals("emMaos")){
                metodo = "Em maos";
            }else if(e.metodoPagamento.equals("correios")){
                metodo = "Correios, " + e.endereco;
            }else{
                metodo = e.banco + ", Ag. " + e.agencia + " CC " + e.contaCorrente;
            }
            folha.append(String.format(
                    Locale.US, "%-21s %8s %8s %8s %13s %9s %15s %s%n", e.nome, formatar(fixo), formatar(vendas),
                    formatar(montante), formatar(salario_bruto),
                    formatar(descontos), formatar(salario_liquido), metodo));
        }
        folha.append(String.format(
                Locale.US,
                "\nTOTAL COMISSIONADOS   %8s %8s %8s %13s %9s %15s\n", formatar(total_fixo), formatar(total_vendas),
                formatar(total_comissao), formatar(total_bruto), formatar(total_desconto), formatar(total_liquido)
        ));
        folha.append(String.format(Locale.US, "\nTOTAL FOLHA: %s\n", formatar(total)));
        try (PrintWriter arquivo = new PrintWriter(saida)) {
            arquivo.print(folha);
        }
        finalizarComando(estadoAnterior);
    }
    //---------------------------------------------------------------------------------
    public String totalFolha(String data){
        LocalDate data_folha = LocalDate.parse(data,formatter);
        double total = 0;
        boolean fim_de_mes = false;
        boolean quinzena = false;
        boolean sexta = false;
        if(data_folha.getDayOfMonth() == data_folha.lengthOfMonth()) fim_de_mes = true;
        if(ChronoUnit.DAYS.between(LocalDate.of(2005,1,1), data_folha) % 14 == 13) quinzena = true;
        if(data_folha.getDayOfWeek() == DayOfWeek.FRIDAY)  sexta = true;
        for(Map.Entry<String, Empregado> id_emp : banco.empregados.entrySet()){
            String id = id_emp.getKey();
            Empregado e = id_emp.getValue();
            if(e.getClass().equals(Empregado.class) && fim_de_mes){
                double salario = Double.parseDouble(e.salario.replace(",","."));
                total += salario;
            }else if(e instanceof Comissionado && quinzena){
                double salario = Double.parseDouble(e.salario.replace(",","."));
                total += Math.floor((salario * 24 / 52) * 100) / 100;
                for(Map.Entry<String, String> data_valor : banco.bancoDeHoras.get(id).entrySet()){
                    LocalDate data_i =  LocalDate.parse(data_valor.getKey(),formatter);
                    long periodo = ChronoUnit.DAYS.between(data_i,data_folha);
                    if(periodo > 0 && periodo < 14){
                        double venda = Double.parseDouble(data_valor.getValue().replace(",","."));
                        double comissao = Double.parseDouble(((Comissionado) e).comissao.replace(",","."));
                        total += Math.floor((venda * comissao) * 100) / 100;
                    }
                }
            }else if(e instanceof  Horista && sexta){
                for(Map.Entry<String, String> data_valor : banco.bancoDeHoras.get(id).entrySet()){
                    LocalDate data_i =  LocalDate.parse(data_valor.getKey(),formatter);
                    long periodo = ChronoUnit.DAYS.between(data_i,data_folha);
                    if(periodo > 0 && periodo < 7){
                        double hrs = Double.parseDouble(data_valor.getValue().replace(",","."));
                        double salario_hora = Double.parseDouble(e.salario.replace(",","."));
                        if(hrs > 8){
                            total += salario_hora * 8;
                            total += salario_hora * 1.5 * (hrs - 8);
                        }else{
                            total += salario_hora * hrs;
                        }
                    }
                }
            }
        }
        return String.format(Locale.US, "%.2f", total).replace(".", ",");
    }
    public void undo() throws Exception {
        if (encerrado) {
            throw new Exception("Nao pode dar comandos depois de encerrarSistema.");
        }
        if (undo.isEmpty()) {
            throw new Exception("Nao ha comando a desfazer.");
        }
        RegistroHistorico registro = undo.pop();
        banco.restaurarSnapshot(registro.antes);
        redo.push(registro);
    }
    public void redo() throws Exception {
        if (encerrado) {
            throw new Exception("Nao pode dar comandos depois de encerrarSistema.");
        }
        if (redo.isEmpty()) {
            throw new Exception("Nao ha comando a refazer.");
        }
        RegistroHistorico registro = redo.pop();
        banco.restaurarSnapshot(registro.depois);
        undo.push(registro);
    }
    public String getNumeroDeEmpregados(){
        return String.valueOf(banco.empregados.size());
    }
}

