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

/**
 * Fachada (padrão Facade) do sistema de folha de pagamento "wepayu".
 * <p>
 * Concentra toda a lógica de negócio: cadastro/consulta/alteração de empregados,
 * lançamento de cartões de ponto, vendas e taxas de serviço, cálculo e geração
 * da folha de pagamento, além do mecanismo de undo/redo baseado em snapshots XML
 * do {@link Banco}.
 * <p>
 * Convenção geral: todo método que MODIFICA o estado do sistema segue o padrão
 * {@code iniciarComando()} (tira um snapshot "antes") ... validações e alteração
 * ... {@code finalizarComando(estadoAnterior)} (tira snapshot "depois" e empilha
 * no histórico de undo). Isso garante que qualquer operação de escrita possa ser
 * desfeita com {@link #undo()}.
 */
public class Facade {

    /**
     * Par de snapshots (XML) do {@link Banco} antes e depois de um comando,
     * usado para implementar undo/redo.
     */
    private static class RegistroHistorico {
        String antes;
        String depois;
        RegistroHistorico(String antes, String depois) {
            this.antes = antes;
            this.depois = depois;
        }
    }

    // Camada de dados/persistência (empregados, cartões de ponto/vendas, sindicatos).
    Banco banco = new Banco();

    // Pilhas de histórico: undo guarda comandos já executados; redo guarda comandos desfeitos.
    Deque<RegistroHistorico> undo = new ArrayDeque<>();
    Deque<RegistroHistorico> redo = new ArrayDeque<>();

    // Formato de data usado em todo o sistema: dia/mês/ano, com validação estrita
    // (ex.: "31/2/2024" é rejeitado em vez de "corrigido" para outra data).
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);

    // Fica true após encerrarSistema(); nenhum outro comando pode ser executado depois disso.
    boolean encerrado = false;

    //-------------------------------------------------------
    /**
     * Deve ser chamado no início de todo comando que altera o estado do sistema.
     * Garante que o sistema não foi encerrado e captura o snapshot "antes" do
     * {@link Banco}, que será usado por {@link #finalizarComando(String)} para
     * registrar a operação no histórico de undo.
     *
     * @return snapshot XML do estado do banco antes da execução do comando
     * @throws Exception se o sistema já foi encerrado (encerrarSistema() já foi chamado)
     */
    private String iniciarComando() throws Exception {
        if (encerrado) {
            throw new Exception("Nao pode dar comandos depois de encerrarSistema.");
        }

        return banco.snapshot();
    }
    //-------------------------------------------------------------------------------
    /**
     * Deve ser chamado ao final de todo comando que altera o estado do sistema.
     * Tira um novo snapshot ("depois"), empilha o par (antes, depois) na pilha de
     * undo e limpa a pilha de redo (uma nova ação invalida qualquer "refazer" pendente).
     *
     * @param estadoAnterior snapshot XML capturado por {@link #iniciarComando()}
     */
    private void finalizarComando(String estadoAnterior) {
        String estadoDepois = banco.snapshot();
        undo.push(new RegistroHistorico(estadoAnterior, estadoDepois));
        redo.clear();
    }
    //--------------------------------------------------------------------------------
    /**
     * Formata um valor double no padrão brasileiro (vírgula como separador decimal,
     * duas casas decimais), usado para montar as colunas da folha de pagamento.
     */
    private String formatar(double valor) {
        return String.format(Locale.US, "%.2f", valor).replace(".", ",");
    }
    //-----------------------------------------------------------------------------------
    /**
     * Verifica se um horista teria recebido pagamento na folha rodada em {@code dataFolha},
     * isto é, se existe algum cartão de ponto com horas > 0 lançado nos 7 dias anteriores
     * (exclusivo) a essa data. Usado por {@link #ultimaDataPagamentoHorista} para localizar
     * a última sexta-feira em que o horista efetivamente trabalhou/recebeu.
     *
     * @param id        identificador do empregado horista
     * @param dataFolha data candidata a ser verificada como data de pagamento
     * @return true se há horas lançadas no período (dataFolha-7, dataFolha)
     */
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
    /**
     * Retrocede a partir de {@code dataFolha}, de 7 em 7 dias, procurando a última data
     * em que o horista teria recebido pagamento (ver {@link #recebeuNaFolhaHorista}).
     * Usado para calcular quantos dias de taxa sindical descontar de um horista: o
     * desconto é proporcional ao tempo desde o último pagamento efetivo, e não apenas
     * desde a última sexta-feira "de calendário".
     *
     * @param id        identificador do empregado horista
     * @param dataFolha data da folha atual
     * @return a última data (anterior a dataFolha) em que houve pagamento, ou null se
     *         nunca houve pagamento anterior à primeira data registrada
     */
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
    /**
     * Encerra o sistema: persiste o estado atual do {@link Banco} em disco
     * (arquivo {@code persistencia.XML}) e bloqueia qualquer novo comando.
     * Esta é a única operação de escrita que NÃO passa por
     * {@link #iniciarComando()}/{@link #finalizarComando(String)} nem entra
     * no histórico de undo/redo.
     */
    public void encerrarSistema(){
        banco.salvar();
        encerrado = true;
    }
    //--------------------------------------------------------
    /**
     * Zera completamente o sistema (remove todos os empregados, cartões de
     * ponto/vendas, sindicatos e apaga o arquivo de persistência). Operação
     * desfazível via undo.
     *
     * @throws Exception se o sistema já foi encerrado
     */
    public void zerarSistema() throws Exception {
        String estadoAnterior = iniciarComando();
        banco.clear();
        finalizarComando(estadoAnterior);
    }
    //----------------------------------------------------------------------------------------
    /**
     * Cria um empregado assalariado ou horista.
     * <p>
     * Validações, na ordem: nome não vazio, endereço não vazio, salário não vazio,
     * salário numérico, salário positivo, tipo válido ({@code "assalariado"} ou
     * {@code "horista"}; {@code "comissionado"} é rejeitado por não se aplicar a
     * esta sobrecarga, pois exige comissão).
     *
     * @param nome     nome do empregado
     * @param endereco endereço do empregado
     * @param tipo     "assalariado" ou "horista"
     * @param salario  salário mensal (assalariado) ou por hora (horista), no formato "0,00"
     * @return o identificador (id) gerado para o novo empregado
     * @throws Exception uma das exceções de validação de negócio (nome/endereço/salário/tipo)
     */
    public String criarEmpregado(String nome,String endereco,String tipo,String salario) throws Exception{
        String estadoAnterior = iniciarComando();
        if(nome.isEmpty()) throw new NomeNuloException();
        if(endereco.isEmpty()) throw new EnderecoNuloException();
        if(salario.isEmpty()) throw new SalarioNuloException();
        if(salario.replace(",",".").chars().anyMatch(Character::isLetter)) throw new SalarioNaoNumericoException();
        if(Double.parseDouble(salario.replace(",",".")) <= 0) throw new SalarioNegativoException();
        if(!(tipo.equals("assalariado") || tipo.equals("horista"))){
            if(tipo.equals("comissionado")){
                // "comissionado" é um tipo válido no sistema, mas não se aplica a esta
                // sobrecarga (que não recebe comissão) -> erro específico, não "tipo inválido".
                throw new TipoNaoAplicavelException();
            }else{
                throw new TipoInvalidoException();
            }
        }
        if(tipo.equals("horista")){
            banco.add(new Horista(nome,endereco,tipo,salario));
        }else{
            // Empregado "assalariado" é representado pela própria classe base Empregado.
            banco.add(new Empregado(nome,endereco,tipo,salario));
        }
        finalizarComando(estadoAnterior);
        return  banco.empregados.lastEntry().getKey();
    }
    //-------------------------------------------------------
    /**
     * Cria um empregado comissionado.
     * <p>
     * Validações, na ordem: nome, endereço, salário (não vazio/numérico/positivo),
     * comissão (não vazia/numérica/positiva) e, por fim, tipo. Note que qualquer tipo
     * diferente de {@code "comissionado"} passado aqui é rejeitado como
     * {@link TipoNaoAplicavelException} (o tipo existe no sistema, mas não combina com
     * esta sobrecarga que exige comissão).
     *
     * @param nome     nome do empregado
     * @param endereco endereço do empregado
     * @param tipo     deve ser "comissionado"
     * @param salario  salário fixo (base para o cálculo quinzenal), no formato "0,00"
     * @param comissao percentual de comissão sobre vendas, no formato "0,00"
     * @return o identificador (id) gerado para o novo empregado
     * @throws Exception uma das exceções de validação de negócio
     */
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
    /**
     * Busca o identificador do N-ésimo empregado cadastrado com um dado nome
     * (útil quando há homônimos), na ordem de inserção no {@link Banco}.
     *
     * @param nome   nome a ser buscado
     * @param indice posição (1-based) entre os empregados com esse nome
     * @return o id do empregado encontrado
     * @throws Exception {@link SemNomeException} se não existir empregado com
     *                    esse nome nessa posição
     */
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
    /**
     * Leitura genérica de um atributo do empregado, dado seu nome como String.
     * Cada atributo tem sua própria regra de aplicabilidade (por exemplo,
     * "comissao" só é válido para {@link Comissionado}; "idSindicato" e
     * "taxaSindical" só se o empregado for sindicalizado; "banco"/"agencia"/
     * "contaCorrente" só se o método de pagamento for "banco").
     *
     * @param id       identificador do empregado
     * @param atributo nome do atributo a consultar
     * @return o valor do atributo, como String
     * @throws Exception {@link IdentificacaoNulaException}, {@link EmpregadoNaoExisteException},
     *                    {@link AtributoInexistenteException} ou uma exceção específica de
     *                    inaplicabilidade do atributo ao tipo/estado do empregado
     */
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
    /**
     * Altera um atributo "simples" do empregado: nome, endereço, salário,
     * comissão, tipo (troca de tipo mantendo o mesmo valor de salário) ou
     * método de pagamento para "emMaos"/"correios".
     * <p>
     * Para desfazer a sindicalização, use {@code atributo = "sindicalizado"}
     * com {@code valor = "false"} (o caso "true", que exige dados adicionais
     * de sindicato, é tratado pela sobrecarga de 5 parâmetros).
     *
     * @throws Exception exceção de validação específica do atributo, ou
     *                    {@link AtributoInexistenteException} se o nome do
     *                    atributo não for reconhecido
     */
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
                // Troca de tipo reaproveitando o salário atual como valor do novo tipo
                // (delegado ao Banco, que recria o objeto Empregado com a subclasse correta).
                banco.trocar_tipo(id, valor, e.salario);
            }
            case "sindicalizado" -> {
                // Só é permitido "desfiliar" (false) por aqui; "true" precisa de
                // idSindicato/taxaSindical e é tratado pela outra sobrecarga.
                if (!valor.equals("false")) throw new SindicalizadoInvalidoException();
                e.sindicalizado = false;
                banco.sindicato.remove(e.id_sindicato);
                e.id_sindicato = null;
                e.taxa_sindical = null;
            }
            case "metodoPagamento" -> {
                if (!(valor.equals("emMaos") || valor.equals("correios"))) throw new MetodoInvalidoException();
                e.metodoPagamento = valor;
                // Ao sair do método "banco", os dados bancários deixam de fazer sentido.
                e.banco = null;
                e.agencia = null;
                e.contaCorrente = null;
            }
            default -> throw new AtributoInexistenteException();
        }
        finalizarComando(estadoAnterior);
    }
    /**
     * Sobrecarga de {@code alteraEmpregado} usada exclusivamente para SINDICALIZAR
     * um empregado (atributo "sindicalizado" = "true"), informando os dados do
     * sindicato (id e taxa). O id do sindicato deve ser único no sistema.
     *
     * @param id             identificador do empregado
     * @param atributo       deve corresponder a "sindicalizado"
     * @param valor          deve ser "true"
     * @param id_sindicato   identificador do sindicato (não pode já existir)
     * @param taxa_sindical  taxa sindical (numérica, positiva)
     * @throws Exception validações de campos vazios/numéricos/negativos, empregado
     *                    inexistente ou id de sindicato já em uso
     */
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
    /**
     * Sobrecarga de {@code alteraEmpregado} usada exclusivamente para configurar o
     * pagamento via depósito em BANCO, informando banco, agência e conta-corrente.
     *
     * @param id             identificador do empregado
     * @param metodo         nome do parâmetro histórico (não usado na lógica)
     * @param valor1         deve ser "banco"
     * @param banco_Nome     nome do banco
     * @param agencia        agência
     * @param contaCorrente  conta-corrente
     * @throws Exception validações de campos vazios/valor inválido ou empregado inexistente
     */
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
    /**
     * Sobrecarga de {@code alteraEmpregado} usada exclusivamente para TROCAR O TIPO
     * do empregado (assalariado/horista/comissionado) informando explicitamente o
     * valor (salário ou comissão) a ser usado pelo novo tipo — diferente da
     * sobrecarga de 3 parâmetros com atributo "tipo", que reaproveita o salário atual.
     *
     * @param id        identificador do empregado
     * @param atributo  nome do parâmetro histórico (não usado na lógica; a ação é sempre trocar o tipo)
     * @param tipo_novo novo tipo: "assalariado", "horista" ou "comissionado"
     * @param valor     novo salário (ou comissão, se o novo tipo for comissionado — ver Banco.trocar_tipo)
     * @throws Exception {@link IdentificacaoNulaException}, {@link EmpregadoNaoExisteException}
     *                    ou {@link TipoInvalidoException}
     */
    public void alteraEmpregado(String id, String atributo,String tipo_novo,String valor) throws Exception{
        String estadoAnterior = iniciarComando();
        if(id.isEmpty()) throw new IdentificacaoNulaException();
        if(!banco.empregados.containsKey(id)) throw new EmpregadoNaoExisteException();
        if(!(tipo_novo.equals("assalariado") || tipo_novo.equals("horista") || tipo_novo.equals("comissionado"))) throw new TipoInvalidoException();
        banco.trocar_tipo(id,tipo_novo,valor);
        finalizarComando(estadoAnterior);
    }
    //--------------------------------------------------------
    /**
     * Remove um empregado do sistema, junto com seu eventual sindicato associado
     * a seu histórico de cartões de ponto/vendas.
     *
     * @throws Exception {@link IdentificacaoNulaException} ou {@link EmpregadoNaoExisteException}
     */
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
    /**
     * Lança um cartão de ponto (horas trabalhadas numa data) para um empregado
     * HORISTA. Só é permitido lançar mais de um cartão por data (o mapa é sobrescrito
     * pela chave "data", então lançamentos repetidos na mesma data substituem o anterior).
     *
     * @param id   identificador do empregado (deve ser horista)
     * @param data data no formato d/M/uuuu
     * @param hora quantidade de horas (numérica, positiva)
     * @throws Exception validações de id/tipo/data/valor
     */
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
    /**
     * Soma as horas NORMAIS trabalhadas por um horista no intervalo
     * {@code [data_inicial, data_final)} (o dia final é exclusivo). Horas acima
     * de 8 em um mesmo cartão são limitadas a 8 (o excedente é hora extra, ver
     * {@link #getHorasExtrasTrabalhadas}).
     * <p>
     * Observação: os cartões são percorridos na ordem de inserção do mapa (não
     * ordenados por data), e o laço interrompe (break) assim que encontra uma
     * data não anterior a {@code data_final}, o que pressupõe lançamentos em
     * ordem cronológica crescente.
     *
     * @return total de horas normais, formatado no padrão brasileiro sem zeros
     *         decimais supérfluos (ex.: "8" em vez de "8,0")
     * @throws Exception validações de id/tipo/datas
     */
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
    /**
     * Soma as horas EXTRAS (excedente acima de 8h por cartão) trabalhadas por um
     * horista no intervalo {@code [data_inicial, data_final)}. Ver observações de
     * {@link #getHorasNormaisTrabalhadas}.
     *
     * @return total de horas extras, formatado no padrão brasileiro
     * @throws Exception validações de id/tipo/datas
     */
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
    /**
     * Lança uma venda realizada por um empregado COMISSIONADO numa data.
     * Reaproveita a mesma estrutura {@code banco.bancoDeHoras}, que para
     * comissionados guarda valores de venda (não horas).
     *
     * @throws Exception validações de id/tipo/data/valor
     */
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
    /**
     * Soma o valor das vendas realizadas por um comissionado no intervalo
     * {@code [data_inicial, data_final)}.
     *
     * @return total de vendas, como String (formato numérico "0,00", sem
     *         normalização completa de zeros — ver implementação)
     * @throws Exception validações de id/tipo/datas
     */
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
    /**
     * Lança uma taxa de serviço cobrada pelo SINDICATO (identificado por
     * {@code id}, que aqui é o id do sindicato, não do empregado) numa data.
     *
     * @param id    identificador do sindicato (deve já existir, criado ao
     *              sindicalizar algum empregado)
     * @param data  data no formato d/M/uuuu
     * @param valor valor da taxa (numérico, positivo)
     * @throws Exception {@link MembroNuloException}, {@link MembroInexistenteException},
     *                    ou validações de data/valor
     */
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
    /**
     * Soma as taxas de serviço lançadas contra o sindicato de um EMPREGADO
     * (aqui {@code id} é o id do empregado, ao contrário de
     * {@link #lancaTaxaServico}) no intervalo {@code [data_inicial, data_final)}.
     *
     * @throws Exception {@link MembroNuloException}, {@link EmpregadoNaoExisteException},
     *                    {@link NaoSindicalizadoException}, {@link MembroInexistenteException}
     *                    ou validações de data
     */
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
    /**
     * Executa a rotina de folha de pagamento para uma data e escreve o relatório
     * completo (texto formatado em colunas, separado por seção HORISTAS /
     * ASSALARIADOS / COMISSIONADOS, com totais por seção e total geral) no arquivo
     * indicado por {@code saida}.
     * <p>
     * Regras de elegibilidade por data:
     * <ul>
     *   <li><b>Assalariados</b>: pagos apenas em fim de mês.</li>
     *   <li><b>Comissionados</b>: pagos a cada 14 dias, calculado como
     *       {@code (dias desde 2005-01-01) % 14 == 13} (quinzena fixa).</li>
     *   <li><b>Horistas</b>: pagos toda sexta-feira.</li>
     * </ul>
     * Regras de cálculo por tipo:
     * <ul>
     *   <li><b>Horista</b>: soma horas dos cartões lançados nos últimos 7 dias
     *       (período aberto-fechado: {@code 0 < periodo < 7}); horas acima de 8h
     *       por cartão pagam 50% de adicional. Desconto sindical proporcional aos
     *       dias desde o último pagamento efetivo (ver
     *       {@link #ultimaDataPagamentoHorista}), mais taxas de serviço do período.
     *       Se o líquido ficaria negativo, líquido e desconto são zerados.</li>
     *   <li><b>Assalariado</b>: salário integral do mês; desconto sindical
     *       proporcional aos dias do mês corrente mais taxas de serviço lançadas
     *       dentro do mesmo mês/ano da folha (excluindo a própria data da folha).</li>
     *   <li><b>Comissionado</b>: parte fixa = {@code salario anual * 24/52}
     *       (arredondada para baixo em centavos) + comissão sobre vendas dos
     *       últimos 14 dias; desconto sindical de 14 dias mais taxas de serviço
     *       do período.</li>
     * </ul>
     *
     * @param data  data de referência da folha (d/M/uuuu)
     * @param saida caminho do arquivo onde o relatório será escrito
     * @throws Exception se o sistema estiver encerrado, ou erro de I/O ao escrever o arquivo
     */
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
        // Empregados elegíveis para esta folha, separados por categoria e já
        // preparados para ordenação por nome.
        SequencedMap<String,Empregado> horistas = new LinkedHashMap<>();
        SequencedMap<String,Empregado> assalariados = new LinkedHashMap<>();
        SequencedMap<String,Empregado> comissionados = new LinkedHashMap<>();
        if(data_folha.getDayOfMonth() == data_folha.lengthOfMonth()) fim_de_mes = true;
        // Quinzena "fixa" ancorada em 01-01-2005: dias múltiplos de 14 (resto 13) marcam
        // o fim de uma quinzena de pagamento para os comissionados.
        if(ChronoUnit.DAYS.between(LocalDate.of(2005,1,1), data_folha) % 14 == 13) quinzena = true;
        if(data_folha.getDayOfWeek() == DayOfWeek.FRIDAY)  sexta = true;
        // Filtra os empregados elegíveis nesta data, por categoria.
        for(Map.Entry<String, Empregado> id_emp : banco.empregados.entrySet()){
            String id = id_emp.getKey();
            Empregado e = id_emp.getValue();
            if(e instanceof  Horista && sexta){
                horistas.put(id,e);
            }else if(e.getClass().equals(Empregado.class) && fim_de_mes){
                // getClass().equals(Empregado.class) exclui subclasses (Horista/Comissionado):
                // aqui só entram os assalariados "puros".
                assalariados.put(id,e);
            }else if(e instanceof Comissionado && quinzena){
                comissionados.put(id,e);
            }
        }
        // Ordena cada categoria por nome para exibição no relatório.
        List<Map.Entry<String, Empregado>> listaHoristas = new ArrayList<>(horistas.entrySet());
        listaHoristas.sort(Comparator.comparing(entry -> entry.getValue().nome));
        List<Map.Entry<String, Empregado>> listaAssalariados = new ArrayList<>(assalariados.entrySet());
        listaAssalariados.sort(Comparator.comparing(entry -> entry.getValue().nome));
        List<Map.Entry<String, Empregado>> listaComissionados = new ArrayList<>(comissionados.entrySet());
        listaComissionados.sort(Comparator.comparing(entry -> entry.getValue().nome));

        // ---------------------- SEÇÃO HORISTAS ----------------------
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
            // Soma horas normais/extras dos cartões lançados nos últimos 7 dias
            // (0 < periodo < 7, ou seja, estritamente após a última folha e antes da atual).
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
                        // Horas extras pagam 50% a mais (1.5x) sobre o valor da hora normal.
                        salario_bruto += salario_hora * 8 + salario_hora * 1.5 * (hrs - 8);
                    }else{
                        horas += hrs;
                        total_hora += hrs;
                        salario_bruto += salario_hora * hrs;
                    }
                }
            }
            if(e.sindicalizado && salario_bruto > 0){
                // Desconto sindical só é aplicado se o horista efetivamente recebeu
                // bruto > 0 nesta folha; é proporcional ao número de dias desde o
                // último pagamento efetivo (não apenas desde a última sexta-feira).
                double taxa = Double.parseDouble(e.taxa_sindical.replace(",","."));
                LocalDate ultimaDataPagamento = ultimaDataPagamentoHorista(id,data_folha);
                if(ultimaDataPagamento == null){
                    // Nunca houve pagamento anterior: desconta desde a primeira data
                    // de cartão de ponto registrada (inclusive) até a folha atual.
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
                // Soma taxas de serviço extras lançadas contra o sindicato nos últimos 7 dias.
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
                // Regra de negócio: líquido nunca fica negativo; nesse caso o desconto
                // também é zerado (o empregado não "deve" ao sindicato).
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

        // ---------------------- SEÇÃO ASSALARIADOS ----------------------
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
            // Assalariado recebe o salário mensal integral (sem cálculo de horas/vendas).
            salario_bruto = Double.parseDouble(e.salario.replace(",","."));
            if(e.sindicalizado){
                // Desconto sindical proporcional ao número de dias do mês corrente,
                // mais taxas de serviço lançadas dentro do mesmo mês/ano da folha
                // (excluindo a própria data da folha, para não contar duas vezes o "hoje").
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

        // ---------------------- SEÇÃO COMISSIONADOS ----------------------
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
            // Parte fixa da quinzena: salário anual convertido para 24 quinzenas de 52 semanas/ano.
            double fixo = Math.floor((salario_ano * 24 / 52) * 100) / 100;
            double vendas = 0;
            // Soma vendas lançadas nos últimos 14 dias (0 < periodo < 14).
            for(Map.Entry<String, String> data_valor : banco.bancoDeHoras.get(id).entrySet()){
                LocalDate data_i =  LocalDate.parse(data_valor.getKey(),formatter);
                long periodo = ChronoUnit.DAYS.between(data_i,data_folha);
                if(periodo > 0 && periodo < 14){
                    double venda = Double.parseDouble(data_valor.getValue().replace(",","."));
                    vendas += venda;
                }
            }
            if(e.sindicalizado){
                // Desconto sindical de 14 dias (quinzena cheia) mais taxas de
                // serviço lançadas no mesmo período das vendas.
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
            // Montante de comissão arredondado para baixo em centavos.
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
    /**
     * Calcula apenas o valor TOTAL bruto que seria pago na folha de uma data,
     * sem gerar relatório nem aplicar descontos sindicais. Reaproveita as mesmas
     * regras de elegibilidade (fim de mês / quinzena / sexta-feira) e de cálculo
     * de bruto usadas em {@link #rodaFolha(String, String)}, mas de forma mais
     * enxuta e sem efeitos colaterais (não é um comando desfazível — não chama
     * iniciarComando/finalizarComando pois não altera o estado do sistema).
     *
     * @param data data de referência (d/M/uuuu)
     * @return total bruto da folha, formatado no padrão brasileiro
     */
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
    /**
     * Desfaz o último comando executado, restaurando o {@link Banco} para o
     * snapshot "antes" registrado. O comando desfeito é movido da pilha de
     * undo para a pilha de redo.
     *
     * @throws Exception se o sistema estiver encerrado, ou não houver comando a desfazer
     */
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
    /**
     * Refaz o último comando desfeito por {@link #undo()}, restaurando o
     * {@link Banco} para o snapshot "depois" registrado. O comando refeito é
     * movido de volta da pilha de redo para a pilha de undo.
     *
     * @throws Exception se o sistema estiver encerrado, ou não houver comando a refazer
     */
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
    /**
     * @return a quantidade atual de empregados cadastrados no sistema, como String.
     */
    public String getNumeroDeEmpregados(){
        return String.valueOf(banco.empregados.size());
    }
}