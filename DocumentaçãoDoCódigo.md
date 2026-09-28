# Documentação do Sistema `wepayu`

Sistema de folha de pagamento (payroll) do pacote `br.ufal.ic.p2.wepayu`, composto por um modelo de domínio de empregados, uma camada de persistência/histórico (`Banco`) e uma fachada (`Facade`) que expõe as operações do sistema.

## 1. Visão geral e arquitetura

O sistema segue o padrão **Facade**: toda a interação externa (testes, EasyAccept, etc.) passa pela classe `Facade`, que coordena o `Banco` (dados) e o modelo de domínio (`Empregado` e suas subclasses).

```
Facade  ──usa──>  Banco  ──contém──>  Empregado (+ Horista, Comissionado)
   │                  │
   └── undo/redo      └── persistência em XML (XStream)
```

Principais responsabilidades:

| Camada | Classe(s) | Responsabilidade |
| --- | --- | --- |
| Domínio | `Empregado`, `Horista`, `Comissionado` | Representam os dados de cada funcionário e regras simples de formatação |
| Dados/Persistência | `Banco` | Armazena empregados, cartões de ponto/vendas e sindicatos; salva/carrega em XML; cria snapshots para undo/redo |
| Fachada | `Facade` | Valida entradas, lança exceções de negócio, orquestra o `Banco` e calcula a folha de pagamento |

## 2. Modelo de domínio

### 2.1 `Empregado` (classe base)

Representa um empregado **assalariado** (mensalista) quando instanciado diretamente; `Horista` e `Comissionado` estendem essa classe.

**Atributos:**

| Atributo | Tipo | Descrição |
| --- | --- | --- |
| `nome`, `endereco`, `tipo` | `String` | Dados cadastrais e o tipo textual (`"assalariado"`, `"horista"`, `"comissionado"`) |
| `salario` | `String` | Salário mensal (assalariado) ou salário/hora (horista) ou salário fixo quinzenal (comissionado), sempre no formato `"0,00"` |
| `sindicalizado` | `boolean` | Indica filiação sindical |
| `id_sindicato`, `taxa_sindical` | `String` | Preenchidos apenas se sindicalizado |
| `metodoPagamento` | `String` | `"emMaos"` (padrão), `"correios"` ou `"banco"` |
| `banco`, `agencia`, `contaCorrente` | `String` | Preenchidos apenas se `metodoPagamento == "banco"` |

**Comportamento notável:** o construtor e os getters (`getSalario`, `getTaxa`) garantem que valores numéricos sem separador decimal (ex.: `"1000"`) sejam normalizados para `"1000,00"`, evitando inconsistência de formato brasileiro (`,` como separador decimal) em toda a aplicação.

### 2.2 `Horista`

Subclasse de `Empregado` sem atributos adicionais — apenas marca o tipo por meio de `instanceof`. Seus registros de horas trabalhadas ficam em `Banco.bancoDeHoras`, indexados por data.

### 2.3 `Comissionado`

Subclasse de `Empregado` que adiciona:

- `comissao` (`String`): percentual de comissão sobre vendas, também normalizado para `"0,00"` via `getComissao()`.

Suas vendas realizadas também são registradas em `Banco.bancoDeHoras` (a mesma estrutura é reaproveitada para horas *ou* vendas, dependendo do tipo do empregado).

## 3. `Banco` — persistência e histórico

`Banco` mantém três mapas ordenados (`LinkedHashMap`), todos indexados por um identificador `idN` gerado sequencialmente (contador `instancia`):

- `empregados: Map<id, Empregado>`
- `bancoDeHoras: Map<id, Map<data, valor>>` — cartões de ponto (horistas) ou vendas (comissionados); assalariados não possuem entrada aqui
- `sindicato: Map<idSindicato, Map<data, valor>>` — taxas de serviço lançadas contra o sindicato do empregado

**Principais métodos:**

| Método | Função |
| --- | --- |
| `add(Empregado)` | Gera novo `id`, insere no mapa e cria a entrada em `bancoDeHoras` se o empregado não for assalariado puro |
| `trocar_tipo(id, tipoNovo, valor)` | Recria o empregado como outro tipo (`Empregado`/`Horista`/`Comissionado`), preservando dados sindicais e de pagamento, e ajusta `bancoDeHoras` conforme o novo tipo |
| `salvar()` / `carregar()` | Serializam/desserializam o estado completo do `Banco` em `persistencia.XML` usando **XStream** |
| `snapshot()` / `restaurarSnapshot(xml)` | Serializam o estado atual em uma `String` XML (ou o restauram a partir dela) — usados pela `Facade` para implementar **undo/redo** |
| `clear()` | Zera todas as estruturas e apaga o arquivo de persistência |

A escolha de serializar o `Banco` inteiro a cada comando (em `snapshot()`) é o mecanismo central do undo/redo: cada operação da fachada guarda o XML "antes" e "depois" em uma pilha (`Deque<RegistroHistorico>`).

## 4. `Facade` — operações do sistema

Todo método de escrita segue o mesmo padrão:

```java
String estadoAnterior = iniciarComando();  // valida se o sistema não foi encerrado; tira snapshot
// ... validações de negócio (lançam exceções específicas) ...
// ... alteração no banco ...
finalizarComando(estadoAnterior);          // tira novo snapshot, empilha em undo, limpa redo
```

Isso garante que **toda** operação que muda estado seja desfazível.

### 4.1 Cadastro e consulta de empregados

- `criarEmpregado(...)` — duas sobrecargas (com e sem comissão), validando nome/endereço/salário/tipo (e comissão, quando aplicável) antes de instanciar `Empregado`, `Horista` ou `Comissionado`.
- `getEmpregadoPorNome(nome, indice)` — busca o N-ésimo empregado com aquele nome.
- `getAtributoEmpregado(id, atributo)` — leitura genérica de qualquer atributo via `switch`, com exceções específicas quando o atributo não se aplica ao tipo do empregado (ex.: `comissao` em um horista).
- `alteraEmpregado(...)` — quatro sobrecargas, uma para cada grupo de atributos: atributos simples (nome, endereço, salário, comissão, tipo, método `emMaos`/`correios`), sindicalização (`sindicalizado=true` + dados do sindicato), pagamento em banco (dados bancários) e troca de tipo com valor associado (salário/comissão do novo tipo).
- `removerEmpregado(id)` — remove o empregado e as estruturas associadas (`bancoDeHoras`, `sindicato`).

### 4.2 Cartões de ponto, vendas e taxas de serviço

| Método | Aplica-se a | Descrição |
| --- | --- | --- |
| `lancaCartao(id, data, horas)` | `Horista` | Registra horas trabalhadas em uma data |
| `getHorasNormaisTrabalhadas` / `getHorasExtrasTrabalhadas` | `Horista` | Somam horas (normais limitadas a 8h/dia; extras = excedente) num intervalo `[inicial, final)` |
| `lancaVenda(id, data, valor)` | `Comissionado` | Registra uma venda |
| `getVendasRealizadas` | `Comissionado` | Soma vendas num intervalo |
| `lancaTaxaServico(idSindicato, data, valor)` | Sindicalizados | Registra taxa de serviço cobrada pelo sindicato |
| `getTaxasServico` | Sindicalizados | Soma taxas num intervalo |

Todos validam formato de data (`d/M/uuuu`), valores numéricos positivos e a compatibilidade do tipo do empregado com a operação.

### 4.3 Cálculo da folha de pagamento — `rodaFolha(data, arquivoSaida)`

Método central do sistema; determina, para a data informada, quais empregados devem ser pagos:

- **Assalariados**: pagos apenas em **fim de mês**.
- **Comissionados**: pagos a cada **14 dias** (quinzena, calculada a partir de uma data-base fixa `2005-01-01`).
- **Horistas**: pagos toda **sexta-feira**.

Para cada grupo, calcula bruto, descontos sindicais e líquido, e escreve um relatório textual formatado (colunas alinhadas) no arquivo de saída, com totais por grupo e total geral da folha. Regras de cálculo:

- **Horista**: soma horas por cartão de ponto nos últimos 7 dias; horas acima de 8h/dia pagam adicional de 50% (`1.5x`). Descontos sindicais são proporcionais ao número de dias desde o **último pagamento efetivo** (calculado por `ultimaDataPagamentoHorista`, que retrocede de 7 em 7 dias procurando a última data em que houve horas > 0), mais taxas de serviço lançadas no período. Se o líquido ficaria negativo, líquido e desconto são zerados.
- **Assalariado**: salário integral do mês; desconto sindical proporcional aos dias do mês corrente, mais taxas de serviço lançadas dentro do mesmo mês da folha.
- **Comissionado**: parte fixa = `salário anual × 24/52` (arredondado para baixo em centavos) + comissão sobre vendas dos últimos 14 dias; desconto sindical de 14 dias mais taxas de serviço do período.
- `totalFolha(data)` — reaproveita a mesma lógica de elegibilidade e cálculo de bruto (sem descontos) apenas para somar o total pago na data, sem gerar arquivo.

### 4.4 Desfazer / refazer e ciclo de vida

- `undo()` / `redo()` — usam as pilhas `undo`/`redo` de `RegistroHistorico` (par de snapshots XML) para restaurar o `Banco` a um estado anterior ou posterior.
- `encerrarSistema()` — persiste o `Banco` em disco (`salvar()`) e marca `encerrado = true`; após isso, qualquer novo comando lança exceção.
- `zerarSistema()` — limpa todo o estado (`Banco.clear()`), também desfazível.

## 5. Tratamento de erros

Todas as validações de entrada (campos vazios, valores não numéricos ou negativos, tipos inválidos, datas mal formatadas, operações incompatíveis com o tipo do empregado, IDs inexistentes, etc.) lançam exceções específicas do pacote `br.ufal.ic.p2.wepayu.Exception` (por exemplo `NomeNuloException`, `SalarioNaoNumericoException`, `NaoHoristaException`), permitindo mensagens de erro precisas para quem consome a fachada.

## 6. Observações de projeto

- **Formato numérico**: todos os valores monetários/numéricos trafegam como `String` no padrão brasileiro (vírgula decimal); a conversão para `double` é feita pontualmente com `.replace(",", ".")`.
- **Persistência via XStream**: simples e direta, mas serializa a íntegra do objeto `Banco` a cada snapshot — adequado para o volume de dados de um exercício acadêmico, porém custoso em escala.
- **Reaproveitamento de `bancoDeHoras`**: a mesma estrutura guarda horas (horista) ou vendas (comissionado); o significado depende do tipo do empregado associado ao `id`, o que exige atenção ao ler o código.
- **Duplicação de lógica**: `rodaFolha` e `totalFolha` repetem grande parte das regras de elegibilidade e cálculo — candidato natural a refatoração (extrair métodos de cálculo por tipo de empregado).
