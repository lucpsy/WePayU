# Documentação do sistema `wepayu`

Sistema de folha de pagamento (user stories 1 a 8). Toda a lógica de negócio fica atrás de uma `Facade`, que é a única classe usada pelos testes de aceitação (EasyAccept).

## 1. Organização dos pacotes

```
br.ufal.ic.p2.wepayu
├── Facade                  só encaminha os pedidos
├── servico                 regras de cada grupo de comandos
├── modelo                  empregados e tudo que pertence a eles
├── persistencia            guarda os dados em memória e em arquivo XML
├── relatorio               texto da folha de pagamento
├── util                    datas e formatação de números
└── excecoes                uma classe para cada erro de negócio
```

Fluxo de um comando: `Facade` → `GerenciadorComandos` (histórico, undo/redo) → serviço → modelo/repositório.

## 2. Modelo (`modelo`)

`Empregado` é abstrata e guarda o que todo empregado tem: nome, endereço, salário, método de pagamento, filiação ao sindicato e o histórico de cartões e vendas. Os três tipos estendem essa classe e cada um sabe **quando** é pago e **como** calcula o próprio pagamento:

| Tipo | Quando recebe | Salário bruto |
| --- | --- | --- |
| `Horista` | toda sexta-feira | horas normais x salário por hora; o que passa de 8 horas no dia vale 1,5 vez |
| `Assalariado` | último dia útil do mês | salário mensal |
| `Comissionado` | a cada 14 dias, a partir de 1/1/2005 | salário x 12 / 52 x 2 (truncado em centavos) + comissão sobre as vendas do período |

Os métodos principais de `Empregado` são `recebeEm(data)`, `periodoDeApuracao(data)`, `calcularBruto(data)` e `calcularContracheque(data)`. O período de apuração é o intervalo desde o último pagamento (incluindo a própria data): é dele que saem as horas, as vendas, os dias de taxa sindical e as taxas de serviço de cada pagamento.

Outras classes do pacote:

- `CartaoPonto`, `Venda` e `TaxaServico` (filhas de `Lancamento`): data e valor.
- `MembroSindicato`: identificação, taxa sindical diária e taxas de serviço. Calcula o desconto de um período.
- `MetodoPagamento` (`EmMaos`, `Correios`, `DepositoBancario`): forma de receber e texto da coluna "Metodo" da folha.
- `Periodo`: intervalo de datas que inclui o início e não inclui o fim, como nas consultas do sistema.
- `Contracheque`: bruto, descontos e líquido de um pagamento.

Valores em dinheiro, comissão e horas são `BigDecimal` e só viram texto na hora de mostrar (`util.Formato`). Todos os atributos são privados e os setters recusam valores inválidos.

### Regras que merecem atenção

- **Horista:** vários cartões na mesma data se somam, e as horas extras são calculadas sobre o total do dia. O horista conta como contratado no dia do primeiro cartão. A taxa sindical vale por dia desde o último pagamento em que ele realmente trabalhou, e as taxas de serviço lançadas nesse intervalo também são cobradas. Se os descontos passam do bruto, a linha sai zerada.
- **Assalariado:** a taxa sindical vale por dia desde o último pagamento (o último dia útil do mês anterior), então em abril de 2005 (pago em 29/4) são contados 29 dias.
- **Data de contratação:** não existe comando para informá-la. Por regra do enunciado dos testes, assalariados e comissionados valem como contratados em 1/1/2005 (constante `Empregado.DATA_CONTRATACAO_PADRAO`).
- **Troca de tipo:** cartões, vendas, método de pagamento e sindicato acompanham o empregado, inclusive se ele voltar ao tipo anterior. Trocar para o mesmo tipo atualiza o valor informado.

## 3. Persistência (`persistencia`)

- `RepositorioEmpregados`: mapa de id para empregado, na ordem de cadastro. Gera os ids (`id1`, `id2`, ...) e responde às buscas (por id, por nome, por identificação no sindicato).
- `ArmazenamentoXml`: usa o XStream para gravar e ler o repositório em `persistencia.XML` e para transformá-lo em texto (usado no undo/redo).

Se o arquivo `persistencia.XML` de uma versão anterior do projeto estiver na pasta, apague-o antes de rodar: o formato mudou.

## 4. Serviços (`servico`)

| Classe | Responsabilidade |
| --- | --- |
| `ServicoEmpregado` | criar, consultar, alterar (inclusive sindicato, banco e tipo) e remover empregados |
| `ServicoLancamento` | cartões de ponto, vendas, taxas de serviço e suas consultas por período |
| `ServicoFolha` | descobrir quem recebe na data, `totalFolha` e `rodaFolha` |
| `GerenciadorComandos` | undo/redo, encerramento do sistema e proteção contra comandos que falham |
| `Entrada` | converte e valida os textos recebidos (vazio, não numérico, negativo, data inválida) |

`GerenciadorComandos` executa todo comando que altera dados: guarda o estado antes e depois (XML) para o `undo`/`redo` e, se o comando falhar, restaura o estado anterior. Depois de `encerrarSistema` nenhum comando é aceito.

## 5. Folha de pagamento (`relatorio`)

`RelatorioFolha` recebe a data e os empregados que são pagos nela e monta o texto: seções de horistas, assalariados e comissionados (ordenadas por nome), totais de cada seção e o total da folha. O total mostrado por `totalFolha` usa o mesmo cálculo de bruto da folha.

## 6. Exceções (`excecoes`)

`WePayUException` é a classe-mãe de todas as exceções de negócio, cada uma em seu arquivo e com a mensagem esperada pelos testes (por exemplo `NomeNuloException`, `SalarioNaoNumericoException`, `NaoHoristaException`). Também existem `SistemaEncerradoException`, `NadaParaDesfazerException`, `NadaParaRefazerException` e `ErroDeArquivoException` (que guarda a causa original). Entradas inválidas viram uma dessas exceções, nunca erros do Java como `NumberFormatException`.

Salário, comissão e taxa sindical aceitam zero (a mensagem diz "não-negativo"); horas, valor de venda e valor de taxa de serviço precisam ser positivos.

## 7. Como rodar os testes

`Main` roda todos os roteiros de `tests/` (us1 a us8, na ordem, com as continuações `_1` que conferem a persistência). Também aceita nomes de roteiros como argumento, por exemplo `Main us7`.
