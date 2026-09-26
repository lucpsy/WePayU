package br.ufal.ic.p2.wepayu;
import br.ufal.ic.p2.wepayu.models.Comissionado;
import br.ufal.ic.p2.wepayu.models.Horista;
import com.thoughtworks.xstream.XStream;
import br.ufal.ic.p2.wepayu.models.Empregado;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.*;

public class Banco {
    public static final String ARQUIVO = "persistencia.XML";
    private int instancia = 1;
    SequencedMap<String, Empregado> empregados = new LinkedHashMap<>();
    Map<String, Map<String, String>> bancoDeHoras = new LinkedHashMap<>();
    Map<String, Map<String, String>> sindicato = new LinkedHashMap<>();
    public Banco(){
        carregar();
    }
    public void clear(){
        empregados.clear();
        bancoDeHoras.clear();
        sindicato.clear();
        instancia = 1;
        File arquivo = new File(ARQUIVO);
        arquivo.delete();
    }
    public void add(Empregado e){
        String id = "id" + instancia;
        instancia++;
        empregados.put(id,e);
        if(e.getClass() != Empregado.class){
            bancoDeHoras.put(id,new LinkedHashMap<>());
        }
    }
    public void re_add(String id,Empregado e){
        empregados.put(id,e);
    }
    public void add_sindicato(String id_sindicato){
        sindicato.put(id_sindicato,new  LinkedHashMap<>());
    }
    public void trocar_tipo(String id,String tipo_novo,String valor){
        Empregado e = empregados.get(id);
        Empregado novo;
        if(!(e.tipo.equals(tipo_novo))){
            if(tipo_novo.equals("assalariado")){
                novo = new Empregado(e.nome,e.endereco,tipo_novo,valor);
                bancoDeHoras.remove(id);
            }
            else if(tipo_novo.equals("horista")){
                novo = new Horista(e.nome,e.endereco,tipo_novo,valor);
                bancoDeHoras.put(id, new LinkedHashMap<>());
            }else{
                novo = new Comissionado(e.nome,e.endereco,tipo_novo,e.salario,valor);
                bancoDeHoras.put(id, new LinkedHashMap<>());
            }
            novo.sindicalizado = e.sindicalizado;
            novo.id_sindicato = e.id_sindicato;
            novo.taxa_sindical =  e.taxa_sindical;
            novo.metodoPagamento = e.metodoPagamento;
            novo.banco = e.banco;
            novo.agencia = e.agencia;
            novo.contaCorrente = e.contaCorrente;
            re_add(id,novo);
        }
    }
    private XStream configurarXStream() {

        XStream xstream = new XStream(new com.thoughtworks.xstream.io.xml.StaxDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.allowTypes(new Class[] {
                Horista.class,
                Comissionado.class,
                Empregado.class,
                Banco.class
        });
        xstream.allowTypesByWildcard(
                new String[] {
                        "java.util.**"
                }
        );

        return xstream;
    }
    public void salvar() {
        XStream xstream = configurarXStream();
        try (FileOutputStream arquivo = new FileOutputStream(ARQUIVO)) {
            xstream.toXML(this, arquivo);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar persistencia.", e);
        }
    }
    public void carregar() {
        File arquivo = new File(ARQUIVO);
        if (!arquivo.exists()) {
            return;
        }
        try {
            XStream xstream = configurarXStream();
            Banco bancoSalvo = (Banco) xstream.fromXML(arquivo);
            this.instancia = bancoSalvo.instancia;
            this.empregados = bancoSalvo.empregados;
            this.bancoDeHoras = bancoSalvo.bancoDeHoras;
            this.sindicato = bancoSalvo.sindicato;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar persistencia.", e);
        }
    }
}
