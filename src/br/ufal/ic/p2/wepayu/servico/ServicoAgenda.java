package br.ufal.ic.p2.wepayu.servico;

import br.ufal.ic.p2.wepayu.excecoes.*;
import br.ufal.ic.p2.wepayu.modelo.AgendaPagamento;
import br.ufal.ic.p2.wepayu.modelo.Mensal;
import br.ufal.ic.p2.wepayu.modelo.Semanal;
import br.ufal.ic.p2.wepayu.persistencia.RepositorioEmpregados;

import java.util.List;

public class ServicoAgenda{
    RepositorioEmpregados repositorio;
    public static final AgendaPagamento[] PADROES = {new Semanal(1,5),new Semanal(2,5),new Mensal("$")};
    public ServicoAgenda(RepositorioEmpregados repositorio){
        this.repositorio = repositorio;
    }
    AgendaPagamento de(String texto) throws AgendaNaoDisponivelException {
        for (AgendaPagamento a : repositorio.agendas()){
            if (a.descricao().equals(texto)) return a;
        }
        throw new AgendaNaoDisponivelException();
    }
    public void lancar_agenda(String descricao) throws WePayUException {
        AgendaPagamento agenda = Entrada.agenda(descricao);
        for(AgendaPagamento a : repositorio.agendas()){
            if(a.descricao().equals(agenda.descricao())){
                throw new AgendaJaExisteException();
            }
        }
        repositorio.agendas().add(agenda);
    }
}
