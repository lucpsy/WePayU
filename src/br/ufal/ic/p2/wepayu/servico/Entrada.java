package br.ufal.ic.p2.wepayu.servico;

import br.ufal.ic.p2.wepayu.excecoes.*;
import br.ufal.ic.p2.wepayu.modelo.Periodo;
import br.ufal.ic.p2.wepayu.util.Datas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Transforma os textos recebidos pela Facade em valores de verdade, lançando a
 * exceção certa quando o texto é vazio, não é número ou tem sinal errado.
 */
final class Entrada {
    private static final Pattern NUMERO = Pattern.compile("-?\\d+([.,]\\d+)?");

    private Entrada() {
    }

    static boolean vazio(String texto) {
        return texto == null || texto.isEmpty();
    }

    static String nome(String texto) throws WePayUException {
        return obrigatorio(texto, NomeNuloException::new);
    }

    static String endereco(String texto) throws WePayUException {
        return obrigatorio(texto, EnderecoNuloException::new);
    }

    static String idSindicato(String texto) throws WePayUException {
        return obrigatorio(texto, IdentificacaoSindicatoNulaException::new);
    }

    /** Salário, comissão e taxa sindical podem ser zero, mas não negativos. */
    static BigDecimal salario(String texto) throws WePayUException {
        BigDecimal numero = numero(texto, SalarioNuloException::new, SalarioNaoNumericoException::new);
        if (numero.signum() < 0) {
            throw new SalarioNegativoException();
        }
        return numero;
    }

    static BigDecimal comissao(String texto) throws WePayUException {
        BigDecimal numero = numero(texto, ComissaoNulaException::new, ComissaoNaoNumericaException::new);
        if (numero.signum() < 0) {
            throw new ComissaoNegativaException();
        }
        return numero;
    }

    static BigDecimal taxaSindical(String texto) throws WePayUException {
        BigDecimal numero = numero(texto, TaxaSindicalNulaException::new, TaxaSindicalNaoNumericaException::new);
        if (numero.signum() < 0) {
            throw new TaxaSindicalNegativaException();
        }
        return numero;
    }

    /** Horas de um cartão de ponto: precisam ser positivas. */
    static BigDecimal horas(String texto) throws WePayUException {
        BigDecimal numero = numero(texto, HoraNaoNumericaException::new, HoraNaoNumericaException::new);
        if (numero.signum() <= 0) {
            throw new HoraNegativaException();
        }
        return numero;
    }

    /** Valor de uma venda ou de uma taxa de serviço: precisa ser positivo. */
    static BigDecimal valor(String texto) throws WePayUException {
        BigDecimal numero = numero(texto, ValorNaoNumericoException::new, ValorNaoNumericoException::new);
        if (numero.signum() <= 0) {
            throw new ValorNegativoException();
        }
        return numero;
    }

    static LocalDate data(String texto, Supplier<? extends WePayUException> erro) throws WePayUException {
        if (texto == null) {
            throw erro.get();
        }
        try {
            return Datas.interpretar(texto);
        } catch (DateTimeParseException e) {
            throw erro.get();
        }
    }

    /** Período das consultas: a data inicial entra e a final não entra. */
    static Periodo periodo(String dataInicial, String dataFinal) throws WePayUException {
        LocalDate inicio = data(dataInicial, DataInicialInvalidaException::new);
        LocalDate fim = data(dataFinal, DataFinalInvalidaException::new);
        if (inicio.isAfter(fim)) {
            throw new DatasInvalidasException();
        }
        return Periodo.de(inicio, fim);
    }

    private static String obrigatorio(String texto, Supplier<? extends WePayUException> erroSeVazio)
            throws WePayUException {
        if (vazio(texto)) {
            throw erroSeVazio.get();
        }
        return texto;
    }

    private static BigDecimal numero(String texto, Supplier<? extends WePayUException> erroSeVazio,
                                     Supplier<? extends WePayUException> erroSeNaoNumerico) throws WePayUException {
        obrigatorio(texto, erroSeVazio);
        if (!NUMERO.matcher(texto).matches()) {
            throw erroSeNaoNumerico.get();
        }
        return new BigDecimal(texto.replace(',', '.'));
    }
}
