package br.ufal.ic.p2.wepayu.servico;

import br.ufal.ic.p2.wepayu.excecoes.*;
import br.ufal.ic.p2.wepayu.modelo.Comissionado;
import br.ufal.ic.p2.wepayu.modelo.Empregado;
import br.ufal.ic.p2.wepayu.modelo.Horista;
import br.ufal.ic.p2.wepayu.modelo.Periodo;
import br.ufal.ic.p2.wepayu.persistencia.RepositorioEmpregados;
import br.ufal.ic.p2.wepayu.util.Formato;

import java.time.LocalDate;

/** Cartões de ponto, vendas e taxas de serviço, com as consultas por período. */
public class ServicoLancamento {
    private final RepositorioEmpregados repositorio;

    public ServicoLancamento(RepositorioEmpregados repositorio) {
        this.repositorio = repositorio;
    }

    /** Se já existe cartão na mesma data, as horas se somam. */
    public void lancarCartao(String id, String data, String horas) throws WePayUException {
        Horista horista = buscarHorista(id);
        LocalDate dia = Entrada.data(data, DataInvalidaException::new);
        horista.lancarCartao(dia, Entrada.horas(horas));
    }

    public String horasNormais(String id, String dataInicial, String dataFinal) throws WePayUException {
        Horista horista = buscarHorista(id);
        Periodo periodo = Entrada.periodo(dataInicial, dataFinal);
        return Formato.horas(horista.horasNormais(periodo));
    }

    public String horasExtras(String id, String dataInicial, String dataFinal) throws WePayUException {
        Horista horista = buscarHorista(id);
        Periodo periodo = Entrada.periodo(dataInicial, dataFinal);
        return Formato.horas(horista.horasExtras(periodo));
    }

    public void lancarVenda(String id, String data, String valor) throws WePayUException {
        Comissionado comissionado = buscarComissionado(id);
        LocalDate dia = Entrada.data(data, DataInvalidaException::new);
        comissionado.lancarVenda(dia, Entrada.valor(valor));
    }

    public String vendasRealizadas(String id, String dataInicial, String dataFinal) throws WePayUException {
        Comissionado comissionado = buscarComissionado(id);
        Periodo periodo = Entrada.periodo(dataInicial, dataFinal);
        return Formato.valor(comissionado.totalVendas(periodo));
    }

    /** Aqui o id é o do membro no sindicato, e não o do empregado. */
    public void lancarTaxaServico(String idSindicato, String data, String valor) throws WePayUException {
        if (Entrada.vazio(idSindicato)) {
            throw new MembroNuloException();
        }
        Empregado membro = repositorio.buscarPorIdSindicato(idSindicato)
                .orElseThrow(MembroInexistenteException::new);
        LocalDate dia = Entrada.data(data, DataInvalidaException::new);
        membro.getMembroSindicato().lancarTaxaServico(dia, Entrada.valor(valor));
    }

    /** Aqui o id é o do empregado, que precisa ser sindicalizado. */
    public String taxasServico(String id, String dataInicial, String dataFinal) throws WePayUException {
        if (Entrada.vazio(id)) {
            throw new MembroNuloException();
        }
        Empregado empregado = repositorio.buscar(id);
        if (!empregado.isSindicalizado()) {
            throw new NaoSindicalizadoException();
        }
        Periodo periodo = Entrada.periodo(dataInicial, dataFinal);
        return Formato.valor(empregado.getMembroSindicato().totalTaxasServico(periodo));
    }

    private Horista buscarHorista(String id) throws WePayUException {
        if (!(repositorio.buscar(id) instanceof Horista horista)) {
            throw new NaoHoristaException();
        }
        return horista;
    }

    private Comissionado buscarComissionado(String id) throws WePayUException {
        if (!(repositorio.buscar(id) instanceof Comissionado comissionado)) {
            throw new NaoComissionadoException();
        }
        return comissionado;
    }
}
