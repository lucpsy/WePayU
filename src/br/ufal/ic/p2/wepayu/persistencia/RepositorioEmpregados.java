package br.ufal.ic.p2.wepayu.persistencia;

import br.ufal.ic.p2.wepayu.excecoes.EmpregadoNaoExisteException;
import br.ufal.ic.p2.wepayu.excecoes.IdentificacaoNulaException;
import br.ufal.ic.p2.wepayu.excecoes.WePayUException;
import br.ufal.ic.p2.wepayu.modelo.Empregado;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Guarda os empregados em memória, na ordem em que foram cadastrados. */
public class RepositorioEmpregados {
    private final Map<String, Empregado> empregados = new LinkedHashMap<>();
    private int proximoNumero = 1;

    /** Cadastra o empregado e devolve o id gerado (id1, id2, ...). */
    public String adicionar(Empregado empregado) {
        String id = "id" + proximoNumero;
        proximoNumero++;
        empregados.put(id, empregado);
        return id;
    }

    /** Busca pelo id, recusando id vazio e id que não existe. */
    public Empregado buscar(String id) throws WePayUException {
        if (id == null || id.isEmpty()) {
            throw new IdentificacaoNulaException();
        }
        Empregado empregado = empregados.get(id);
        if (empregado == null) {
            throw new EmpregadoNaoExisteException();
        }
        return empregado;
    }

    /** Troca o objeto de um id que já existe (usado quando o empregado muda de tipo). */
    public void substituir(String id, Empregado novo) {
        empregados.replace(id, novo);
    }

    public void remover(String id) throws WePayUException {
        buscar(id);
        empregados.remove(id);
    }

    public Collection<Empregado> todos() {
        return Collections.unmodifiableCollection(empregados.values());
    }

    /** Ids dos empregados com esse nome, na ordem de cadastro. */
    public List<String> idsComNome(String nome) {
        List<String> ids = new ArrayList<>();
        for (Map.Entry<String, Empregado> entrada : empregados.entrySet()) {
            if (entrada.getValue().getNome().equals(nome)) {
                ids.add(entrada.getKey());
            }
        }
        return ids;
    }

    public Optional<Empregado> buscarPorIdSindicato(String idSindicato) {
        return empregados.values().stream()
                .filter(e -> e.isSindicalizado() && e.getMembroSindicato().getIdentificacao().equals(idSindicato))
                .findFirst();
    }

    public boolean existeIdSindicato(String idSindicato) {
        return buscarPorIdSindicato(idSindicato).isPresent();
    }

    public int quantidade() {
        return empregados.size();
    }

    public void limpar() {
        empregados.clear();
        proximoNumero = 1;
    }

    /** Copia o conteúdo de outro repositório para este (usado ao carregar, desfazer e refazer). */
    public void substituirPor(RepositorioEmpregados outro) {
        empregados.clear();
        empregados.putAll(outro.empregados);
        proximoNumero = outro.proximoNumero;
    }
}
