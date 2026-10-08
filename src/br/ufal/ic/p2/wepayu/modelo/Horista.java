package br.ufal.ic.p2.wepayu.modelo;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Recebe por hora trabalhada, toda sexta-feira. O que passa de 8 horas no dia vale 1,5 vez. */
public class Horista extends Empregado {
    public static final String TIPO = "horista";

    private static final BigDecimal LIMITE_HORAS_NORMAIS = BigDecimal.valueOf(8);
    private static final BigDecimal ADICIONAL_HORA_EXTRA = new BigDecimal("1.5");

    public Horista(String nome, String endereco, BigDecimal salarioPorHora) {
        super(nome, endereco, salarioPorHora);
    }

    public Horista(Empregado origem, BigDecimal salarioPorHora) {
        super(origem, salarioPorHora);
    }

    @Override
    public String getTipo() {
        return TIPO;
    }

    public void lancarCartao(LocalDate data, BigDecimal horas) {
        if (horas == null || horas.signum() <= 0) {
            throw new IllegalArgumentException("As horas do cartão precisam ser positivas.");
        }
        adicionarCartao(new CartaoPonto(data, horas));
    }

    /** Horas normais (até 8 por dia) dentro do período. Cartões da mesma data são somados. */
    public BigDecimal horasNormais(Periodo periodo) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal horasDoDia : horasPorDia(periodo).values()) {
            total = total.add(horasDoDia.min(LIMITE_HORAS_NORMAIS));
        }
        return total;
    }

    /** Horas extras (o que passa de 8 no dia) dentro do período. */
    public BigDecimal horasExtras(Periodo periodo) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal horasDoDia : horasPorDia(periodo).values()) {
            total = total.add(horasDoDia.subtract(LIMITE_HORAS_NORMAIS).max(BigDecimal.ZERO));
        }
        return total;
    }

    @Override
    public boolean recebeEm(LocalDate data) {
        return data.getDayOfWeek() == DayOfWeek.FRIDAY;
    }

    /**
     * Começa depois da última sexta em que houve pagamento de verdade (com horas
     * trabalhadas na semana). Antes do primeiro pagamento, conta desde o primeiro
     * cartão lançado, que é quando o horista é considerado contratado.
     */
    @Override
    public Periodo periodoDeApuracao(LocalDate dataPagamento) {
        LocalDate fim = dataPagamento.plusDays(1);
        Optional<LocalDate> primeiroCartao = dataDoPrimeiroCartao();
        if (primeiroCartao.isEmpty() || primeiroCartao.get().isAfter(dataPagamento)) {
            return Periodo.de(dataPagamento.minusDays(6), fim);
        }
        LocalDate contratacao = primeiroCartao.get();
        for (LocalDate sexta = dataPagamento.minusWeeks(1); !sexta.isBefore(contratacao); sexta = sexta.minusWeeks(1)) {
            if (trabalhouNaSemanaDe(sexta)) {
                return Periodo.de(sexta.plusDays(1), fim);
            }
        }
        return Periodo.de(contratacao, fim);
    }

    @Override
    protected BigDecimal brutoDoPeriodo(Periodo periodo) {
        BigDecimal valorNormal = getSalario().multiply(horasNormais(periodo));
        BigDecimal valorExtra = getSalario().multiply(ADICIONAL_HORA_EXTRA).multiply(horasExtras(periodo));
        return valorNormal.add(valorExtra);
    }

    /** O horista nunca recebe valor negativo: se as taxas passam do bruto, a folha sai zerada. */
    @Override
    public Contracheque calcularContracheque(LocalDate dataPagamento) {
        Periodo periodo = periodoDeApuracao(dataPagamento);
        BigDecimal bruto = brutoDoPeriodo(periodo);
        if (bruto.signum() == 0) {
            return Contracheque.de(bruto, BigDecimal.ZERO);
        }
        Contracheque contracheque = Contracheque.de(bruto, descontoSindical(periodo));
        if (contracheque.getLiquido().signum() < 0) {
            return Contracheque.zerado(bruto);
        }
        return contracheque;
    }

    private Map<LocalDate, BigDecimal> horasPorDia(Periodo periodo) {
        Map<LocalDate, BigDecimal> horasPorDia = new HashMap<>();
        for (CartaoPonto cartao : cartoes()) {
            if (periodo.contem(cartao.getData())) {
                horasPorDia.merge(cartao.getData(), cartao.getHoras(), BigDecimal::add);
            }
        }
        return horasPorDia;
    }

    private Optional<LocalDate> dataDoPrimeiroCartao() {
        return cartoes().stream().map(CartaoPonto::getData).min(Comparator.naturalOrder());
    }

    private boolean trabalhouNaSemanaDe(LocalDate sexta) {
        Periodo semana = Periodo.de(sexta.minusDays(6), sexta.plusDays(1));
        return cartoes().stream().anyMatch(cartao -> semana.contem(cartao.getData()));
    }
}
