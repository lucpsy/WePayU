package br.ufal.ic.p2.wepayu.persistencia;

import br.ufal.ic.p2.wepayu.excecoes.ErroDeArquivoException;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.StaxDriver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/** Converte o repositório de e para XML (XStream): no arquivo de persistência e em texto, para o undo/redo. */
public class ArmazenamentoXml {
    public static final String ARQUIVO = "persistencia.XML";

    private final XStream xstream = criarXStream();

    private static XStream criarXStream() {
        XStream xstream = new XStream(new StaxDriver());
        xstream.allowTypesByWildcard(new String[]{
                "br.ufal.ic.p2.wepayu.modelo.**",
                "br.ufal.ic.p2.wepayu.persistencia.**",
                "java.util.**",
                "java.math.**",
                "java.time.**"
        });
        return xstream;
    }

    /** Lê o arquivo de persistência; se ele ainda não existe, devolve um repositório vazio. */
    public RepositorioEmpregados carregar() throws ErroDeArquivoException {
        File arquivo = new File(ARQUIVO);
        if (!arquivo.exists()) {
            return new RepositorioEmpregados();
        }
        try {
            return (RepositorioEmpregados) xstream.fromXML(arquivo);
        } catch (RuntimeException e) {
            throw new ErroDeArquivoException("Erro ao carregar persistencia.", e);
        }
    }

    public void salvar(RepositorioEmpregados repositorio) throws ErroDeArquivoException {
        try (FileOutputStream saida = new FileOutputStream(ARQUIVO)) {
            xstream.toXML(repositorio, saida);
        } catch (IOException | RuntimeException e) {
            throw new ErroDeArquivoException("Erro ao salvar persistencia.", e);
        }
    }

    public void apagarArquivo() {
        new File(ARQUIVO).delete();
    }

    public String serializar(RepositorioEmpregados repositorio) {
        return xstream.toXML(repositorio);
    }

    public RepositorioEmpregados desserializar(String xml) {
        return (RepositorioEmpregados) xstream.fromXML(xml);
    }
}
