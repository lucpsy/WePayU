package br.ufal.ic.p2.wepayu.relatorio;

import br.ufal.ic.p2.wepayu.excecoes.ErroDeArquivoException;
import br.ufal.ic.p2.wepayu.modelo.Assalariado;
import br.ufal.ic.p2.wepayu.modelo.Comissionado;
import br.ufal.ic.p2.wepayu.modelo.Contracheque;
import br.ufal.ic.p2.wepayu.modelo.Empregado;
import br.ufal.ic.p2.wepayu.modelo.Horista;
import br.ufal.ic.p2.wepayu.modelo.Periodo;
import br.ufal.ic.p2.wepayu.util.Formato;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Monta o texto da folha de pagamento de um dia: uma seção para cada tipo de
 * empregado (em ordem de nome), com os totais da seção e o total da folha.
 */
public class RelatorioFolha {
    private static final int LARGURA = 127;

    /** Soma das colunas de valores de uma seção. */
    private static class Totais {
        BigDecimal bruto = BigDecimal.ZERO;
        BigDecimal descontos = BigDecimal.ZERO;
        BigDecimal liquido = BigDecimal.ZERO;

        void somar(Contracheque contracheque) {
            bruto = bruto.add(contracheque.getBruto());
            descontos = descontos.add(contracheque.getDescontos());
            liquido = liquido.add(contracheque.getLiquido());
        }
    }

    private final LocalDate data;
    private final List<Horista> horistas;
    private final List<Assalariado> assalariados;
    private final List<Comissionado> comissionados;

    /** Recebe apenas os empregados que são pagos na data. */
    public RelatorioFolha(LocalDate data, Collection<Empregado> pagos) {
        this.data = data;
        this.horistas = separar(pagos, Horista.class);
        this.assalariados = separar(pagos, Assalariado.class);
        this.comissionados = separar(pagos, Comissionado.class);
    }

    public void gravar(String caminho) throws ErroDeArquivoException {
        try {
            Files.writeString(Path.of(caminho), montar());
        } catch (IOException e) {
            throw new ErroDeArquivoException("Erro ao gravar a folha em " + caminho + ".", e);
        }
    }

    public String montar() {
        StringBuilder texto = new StringBuilder();
        texto.append("FOLHA DE PAGAMENTO DO DIA ").append(data).append("\n");
        texto.append("====================================\n");
        BigDecimal total = secaoHoristas(texto)
                .add(secaoAssalariados(texto))
                .add(secaoComissionados(texto));
        texto.append(String.format("\nTOTAL FOLHA: %s\n", Formato.moeda(total)));
        return texto.toString();
    }

    private BigDecimal secaoHoristas(StringBuilder texto) {
        abrirSecao(texto, "HORISTAS",
                "Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo",
                "==================================== ===== ===== ============= ========= =============== ======================================");
        Totais totais = new Totais();
        BigDecimal totalHoras = BigDecimal.ZERO;
        BigDecimal totalExtras = BigDecimal.ZERO;
        for (Horista horista : horistas) {
            Periodo periodo = horista.periodoDeApuracao(data);
            BigDecimal horas = horista.horasNormais(periodo);
            BigDecimal extras = horista.horasExtras(periodo);
            Contracheque contracheque = horista.calcularContracheque(data);
            totais.somar(contracheque);
            totalHoras = totalHoras.add(horas);
            totalExtras = totalExtras.add(extras);
            texto.append(String.format(Locale.US, "%-36s %5d %5d %13s %9s %15s %s\n",
                    horista.getNome(), horas.intValue(), extras.intValue(),
                    Formato.moeda(contracheque.getBruto()), Formato.moeda(contracheque.getDescontos()),
                    Formato.moeda(contracheque.getLiquido()), metodo(horista)));
        }
        texto.append(String.format(Locale.US, "\nTOTAL HORISTAS                       %5d %5d %13s %9s %15s\n",
                totalHoras.intValue(), totalExtras.intValue(), Formato.moeda(totais.bruto),
                Formato.moeda(totais.descontos), Formato.moeda(totais.liquido)));
        return totais.bruto;
    }

    private BigDecimal secaoAssalariados(StringBuilder texto) {
        abrirSecao(texto, "ASSALARIADOS",
                "Nome                                             Salario Bruto Descontos Salario Liquido Metodo",
                "================================================ ============= ========= =============== ======================================");
        Totais totais = new Totais();
        for (Assalariado assalariado : assalariados) {
            Contracheque contracheque = assalariado.calcularContracheque(data);
            totais.somar(contracheque);
            texto.append(String.format(Locale.US, "%-48s %13s %9s %15s %s\n",
                    assalariado.getNome(), Formato.moeda(contracheque.getBruto()),
                    Formato.moeda(contracheque.getDescontos()), Formato.moeda(contracheque.getLiquido()),
                    metodo(assalariado)));
        }
        texto.append(String.format(Locale.US, "\nTOTAL ASSALARIADOS                               %13s %9s %15s\n",
                Formato.moeda(totais.bruto), Formato.moeda(totais.descontos), Formato.moeda(totais.liquido)));
        return totais.bruto;
    }

    private BigDecimal secaoComissionados(StringBuilder texto) {
        abrirSecao(texto, "COMISSIONADOS",
                "Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo",
                "===================== ======== ======== ======== ============= ========= =============== ======================================");
        Totais totais = new Totais();
        BigDecimal totalFixo = BigDecimal.ZERO;
        BigDecimal totalVendas = BigDecimal.ZERO;
        BigDecimal totalComissao = BigDecimal.ZERO;
        for (Comissionado comissionado : comissionados) {
            Periodo periodo = comissionado.periodoDeApuracao(data);
            BigDecimal fixo = comissionado.calcularFixo();
            BigDecimal vendas = comissionado.totalVendas(periodo);
            BigDecimal comissao = comissionado.calcularComissao(periodo);
            Contracheque contracheque = comissionado.calcularContracheque(data);
            totais.somar(contracheque);
            totalFixo = totalFixo.add(fixo);
            totalVendas = totalVendas.add(vendas);
            totalComissao = totalComissao.add(comissao);
            texto.append(String.format(Locale.US, "%-21s %8s %8s %8s %13s %9s %15s %s\n",
                    comissionado.getNome(), Formato.moeda(fixo), Formato.moeda(vendas), Formato.moeda(comissao),
                    Formato.moeda(contracheque.getBruto()), Formato.moeda(contracheque.getDescontos()),
                    Formato.moeda(contracheque.getLiquido()), metodo(comissionado)));
        }
        texto.append(String.format(Locale.US, "\nTOTAL COMISSIONADOS   %8s %8s %8s %13s %9s %15s\n",
                Formato.moeda(totalFixo), Formato.moeda(totalVendas), Formato.moeda(totalComissao),
                Formato.moeda(totais.bruto), Formato.moeda(totais.descontos), Formato.moeda(totais.liquido)));
        return totais.bruto;
    }

    private void abrirSecao(StringBuilder texto, String titulo, String colunas, String separadores) {
        String linha = "=".repeat(LARGURA);
        String faixaTitulo = "=".repeat(21) + " " + titulo + " " + "=".repeat(LARGURA - 23 - titulo.length());
        texto.append("\n").append(linha).append("\n").append(faixaTitulo).append("\n").append(linha).append("\n");
        texto.append(colunas).append("\n").append(separadores).append("\n");
    }

    private String metodo(Empregado empregado) {
        return empregado.getMetodoPagamento().descrever(empregado.getEndereco());
    }

    private static <T extends Empregado> List<T> separar(Collection<Empregado> pagos, Class<T> tipo) {
        return pagos.stream()
                .filter(tipo::isInstance)
                .map(tipo::cast)
                .sorted(Comparator.comparing(Empregado::getNome))
                .toList();
    }
}
