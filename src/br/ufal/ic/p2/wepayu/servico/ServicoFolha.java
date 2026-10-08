package br.ufal.ic.p2.wepayu.servico;

import br.ufal.ic.p2.wepayu.excecoes.DataInvalidaException;
import br.ufal.ic.p2.wepayu.excecoes.WePayUException;
import br.ufal.ic.p2.wepayu.modelo.Empregado;
import br.ufal.ic.p2.wepayu.persistencia.RepositorioEmpregados;
import br.ufal.ic.p2.wepayu.relatorio.RelatorioFolha;
import br.ufal.ic.p2.wepayu.util.Formato;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Descobre quem recebe em cada dia e gera a folha de pagamento. */
public class ServicoFolha {
    private final RepositorioEmpregados repositorio;

    public ServicoFolha(RepositorioEmpregados repositorio) {
        this.repositorio = repositorio;
    }

    /** Soma dos salários brutos de quem seria pago na data, sem gerar a folha. */
    public String totalFolha(String data) throws WePayUException {
        LocalDate dataFolha = Entrada.data(data, DataInvalidaException::new);
        BigDecimal total = BigDecimal.ZERO;
        for (Empregado empregado : quemRecebeEm(dataFolha)) {
            total = total.add(empregado.calcularBruto(dataFolha));
        }
        return Formato.moeda(total);
    }

    /** Escreve no arquivo {@code saida} a folha de pagamento da data. */
    public void rodarFolha(String data, String saida) throws WePayUException {
        LocalDate dataFolha = Entrada.data(data, DataInvalidaException::new);
        new RelatorioFolha(dataFolha, quemRecebeEm(dataFolha)).gravar(saida);
    }

    private List<Empregado> quemRecebeEm(LocalDate data) {
        return repositorio.todos().stream().filter(e -> e.recebeEm(data)).toList();
    }
}
