package br.ufal.ic.p2.wepayu;
import br.ufal.ic.p2.wepayu.models.Comissionado;
import br.ufal.ic.p2.wepayu.models.Horista;
import com.thoughtworks.xstream.XStream;
import br.ufal.ic.p2.wepayu.models.Assalariado;
import br.ufal.ic.p2.wepayu.models.Empregado;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.*;

public class Banco {
    private static final String ARQUIVO = "persistencia.XML";
    private int instancia = 1;
    SequencedMap<String, Empregado> empregados = new LinkedHashMap<>();
    Map<String, Map<String, String>> bancoDeHoras = new LinkedHashMap<>();
    public Banco(){
        carregar();
    }
    public void clear(){
        empregados.clear();
        bancoDeHoras.clear();
        instancia = 1;
    }
    public void add(Empregado e){
        String id = "id" + instancia;
        instancia++;
        empregados.put(id,e);
        if(!(e instanceof Assalariado)){
            bancoDeHoras.put(id,new LinkedHashMap<>());
        }
    }
    private XStream configurarXStream() {

        XStream xstream = new XStream(new com.thoughtworks.xstream.io.xml.StaxDriver());
        XStream.setupDefaultSecurity(xstream);
        xstream.allowTypes(new Class[] {
                Horista.class,
                Assalariado.class,
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
        } catch (Exception e) {
            throw new RuntimeException("Erro ao carregar persistencia.", e);
        }
    }
}
