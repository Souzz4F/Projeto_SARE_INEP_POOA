# SARE INEP - Sistema de Análise da Rede Escolar

> **Projeto Prático de API REST com Spring Boot e Interface Web**  
> **Disciplina:** Programação Avançada Orientada a Objetos (POOA)  
> **Avaliação:** Projeto P2  
> **Instituição:** FATEC Zona Leste  
> **Docente:** Profa. Me. Sirley Ambrosia Vitorio Addão  

---

## 👥 Integrantes do Grupo

- **Samuel Felipe de Souza** - RA: 
- **Amandha França Belo Ribeiro** - RA: 
- **Felipe Oshiro de Paula** - RA: 

---

## 🎯 1. Objetivo do Projeto

O **SARE INEP (Sistema de Análise da Rede Escolar)** é uma aplicação web desenvolvida para importar, persistir, consultar, alterar, excluir, filtrar e analisar graficamente microdados oficiais da educação básica brasileira disponibilizados pelo **Inep (Instituto Nacional de Estudos e Pesquisas Educacionais Anísio Teixeira)**.

A plataforma transforma dados abertos complexos em informações analíticas acessíveis por meio de um painel interativo, permitindo análises sobre dependência administrativa, distribuição geográfica (urbana/rural), situação de funcionamento e conectividade à Internet nas escolas.

---

## 🏛️ 2. Fonte Oficial dos Dados

- **Órgão responsável:** INEP / Ministério da Educação (MEC).
- **Fonte oficial:** [Microdados do Censo Escolar](https://www.gov.br/inep/pt-br/acesso-a-informacao/dados-abertos/microdados/censo-escolar). A página lista a edição 2025, atualizada em julho de 2026.
- **Pacote oficial:** [microdados_censo_escolar_2025_.zip](https://download.inep.gov.br/dados_abertos/microdados_censo_escolar_2025_.zip).
- **Arquivo utilizado:** `Tabela_Escola_2025_V2.csv`, tabela de escolas do Censo Escolar 2025.
- **Dicionário:** `dicionário_dados_educação_básica.xlsx`, aba `Tabela_de_Escola`; essa aba contém as definições e categorias da tabela usada.
- **Características verificadas do CSV local:** separado por ponto e vírgula, lido em ISO-8859-1, 290 colunas, 214.192 registros e 141.425.160 bytes. A análise encontrou zero códigos INEP repetidos e um registro sem nome de município; esse registro será rejeitado pela validação de campos obrigatórios. O arquivo tem 33.652 valores vazios em `IN_INTERNET`, que são preservados como sem informação.
- **Consulta à página oficial nesta revisão:** 27/10/2026. A data em que o grupo baixou o arquivo deve ser confirmada.
- **Licença:** confirmar e registrar a licença indicada na publicação oficial antes da entrega.
## 🛠️ 3. Tecnologias Utilizadas

- **Linguagem & Plataforma:** Java 21 (LTS)
- **Framework Principal:** Spring Boot com Maven
- **Camada de Persistência:** Spring Data JPA / Hibernate
- **Banco de Dados:** PostgreSQL hospedado na nuvem ([Neon Serverless Postgres](https://neon.tech))
- **Parser de Dados:** Apache Commons CSV (1.14.1)
- **Interface Frontend:** HTML5, CSS3 moderno modularizado (`static/css/style.css`) e JavaScript vanilla (`static/js/app.js` com `fetch` API)
- **Visualização Gráfica:** Chart.js via CDN

---

## 📐 4. Arquitetura da Solução

O projeto segue rigorosamente a separação em camadas conforme especificado no edital:

```
fatec/src/main/java/com/sare_inep/fatec/
│
├── model/                  # Entidade JPA (Escola)
├── repository/             # Interface Spring Data JPA com queries de filtro
├── service/                # Regras de negócio, validações e importador CSV
└── controller/             # Endpoints REST e controle de requisições HTTP

fatec/src/main/resources/
│
├── application.properties  # Configurações do Spring com variáveis de ambiente
└── static/                 # Frontend SPA integrado
    ├── index.html          # Estrutura semântica da interface
    ├── css/style.css       # Estilos visuais e responsividade
    └── js/app.js           # Lógica do painel, gráficos e fetch()
```

---

## 📡 5. Tabela de Endpoints da API REST

A API atende aos verbos, status HTTP e convenções REST obrigatórias sob o prefixo `/api/escolas`:

| Método | Endpoint | Finalidade | Status de Sucesso | Status de Erro |
| :--- | :--- | :--- | :--- | :--- |
| `GET` | `/api/escolas` | Listar todas as escolas cadastradas | `200 OK` | `500 Internal Error` |
| `GET` | `/api/escolas/{id}` | Buscar escola por ID único | `200 OK` | `404 Not Found` |
| `POST` | `/api/escolas` | Cadastrar novo estabelecimento | `201 Created` | `400 Bad Request` |
| `PUT` | `/api/escolas/{id}` | Atualizar estabelecimento existente | `200 OK` | `400 Bad Request` / `404 Not Found` |
| `DELETE` | `/api/escolas/{id}` | Remover estabelecimento | `204 No Content` | `404 Not Found` |
| `POST` | `/api/escolas/importar` | Importar arquivo CSV multipart/form-data | `200 OK` | `400 Bad Request` / `413 Payload Too Large` |
| `GET` | `/api/escolas/filtro` | Filtrar por UF, município, dependência ou localização | `200 OK` | `400 Bad Request` |

---

## 📝 6. Exemplos de JSON (Entrada e Saída)

> Os exemplos de CRUD abaixo são ilustrativos e não fazem parte dos microdados oficiais importados. 

### Cadastrar Escola (`POST /api/escolas`):

**Requisição:**
```json
{
  "codigoEscola": 35012345,
  "nomeEscola": "EMEF Professora Sirley Addão",
  "uf": "SP",
  "municipio": "São Paulo",
  "dependenciaAdministrativa": "Municipal",
  "localizacao": "Urbana",
  "situacaoFuncionamento": "Em Atividade",
  "restricaoAtendimento": "Sem restrição",
  "possuiInternet": true
}
```

**Resposta de Sucesso (`201 Created`):**
```json
{
  "id": 1,
  "codigoEscola": 35012345,
  "nomeEscola": "EMEF Professora Sirley Addão",
  "uf": "SP",
  "municipio": "São Paulo",
  "dependenciaAdministrativa": "Municipal",
  "localizacao": "Urbana",
  "situacaoFuncionamento": "Em Atividade",
  "restricaoAtendimento": "Sem restrição",
  "possuiInternet": true
}
```

### Resposta da Importação (`POST /api/escolas/importar`):
```json
{
  "linhasLidas": 550,
  "importadas": 542,
  "duplicadas": 5,
  "rejeitadas": 3,
  "motivosRejeicao": { "Campos obrigatórios ausentes": 3 },
  "mensagem": "Processamento concluído: 550 linhas lidas, 542 importadas com sucesso, 5 duplicadas ignoradas e 3 rejeitadas."
}
```

### Exemplo de Erro (`404 Not Found`):
```json
{
  "status": 404,
  "erro": "Não Encontrado",
  "mensagem": "Escola com ID 9999 não foi encontrada no sistema."
}
```

---

## ⚙️ 7. Configuração do Ambiente e Execução

### Pré-requisitos
- **Java JDK 21** instalado
- Acesso à internet para o PostgreSQL no Neon

### Passo a Passo

1. **Configurar as variáveis de ambiente.** No PowerShell, entre na pasta fatec e crie o arquivo .env sem sobrescrever um arquivo existente:

        cd fatec
        if (-not (Test-Path .env)) { Copy-Item .env.example .env }
        notepad .env

   Preencha DB_URL, DB_USERNAME e DB_PASSWORD com as credenciais do Neon. O arquivo .env é carregado pelo aplicativo e não deve ser enviado ao GitHub.

2. **Compilar e executar a aplicação:** O limite de upload está configurado para 200 MB, suficiente para o CSV 2025 desta edição.

        .\mvnw.cmd spring-boot:run

3. **Acessar a Aplicação Web:**
   Abra o navegador no endereço:
   ```
   http://localhost:8080
   ```


## 8. Mapeamento e política de importação

O CSV de 2025 usa ponto e vírgula e contém os cabeçalhos `CO_ENTIDADE`, `NO_ENTIDADE`, `SG_UF` (com `CO_UF` como alternativa), `NO_MUNICIPIO`, `TP_DEPENDENCIA`, `TP_LOCALIZACAO`, `TP_SITUACAO_FUNCIONAMENTO` e `IN_INTERNET`. Os campos obrigatórios são código, nome, UF, município, dependência e localização; situação e internet são opcionais. O parser aceita também CSV separado por vírgula e tenta UTF-8 antes de ISO-8859-1.

Para a edição 2025, a aplicação importa código, nome, UF, município, dependência administrativa, localização, situação de funcionamento e acesso à internet. O arquivo não possui `TP_RESTRICAO_ATENDIMENTO`; por isso `restricaoAtendimento` fica nulo na importação e não é usado nas análises. As demais colunas não são persistidas porque não fazem parte das consultas atuais. O relatório deve justificar os oito campos selecionados e os campos descartados com base no arquivo e no dicionário.

Valores categóricos conhecidos são convertidos para rótulos. A UF pode vir como sigla ou código IBGE; dependência usa 1 a 4; localização usa 1 (urbana) e 2 (rural); situação usa 1 (em atividade), 2 (paralisada), 3 (extinta no ano do Censo) e 4 (extinta em anos anteriores), conforme a aba `Tabela_de_Escola`; internet usa 0 (não) e 1 (sim). Campos opcionais vazios permanecem sem informação. Códigos inválidos, campos obrigatórios ausentes e registros com quantidade incorreta de colunas são rejeitados e contabilizados por motivo.

A reimportação não atualiza registros existentes: códigos INEP duplicados no arquivo ou no banco são ignorados e contabilizados como duplicados. A API informa linhas lidas, importadas, rejeitadas, duplicadas e os motivos de rejeição.

## 9. Perguntas respondidas pelos gráficos

Os gráficos agrupam a seleção atual por município, dependência administrativa, localização urbana/rural, situação de funcionamento e disponibilidade de internet. Eles permitem comparar a composição da rede escolar e são recalculados quando os filtros mudam. O relatório técnico deve apresentar a pergunta de negócio, o agrupamento e uma interpretação apoiada nos dados importados.

## 10. Pendências da entrega documental

Antes da entrega, o grupo ainda precisa confirmar a data em que baixou o arquivo e a licença indicada na publicação; justificar a seleção dos oito campos diante das 290 colunas; preencher os RAs; e produzir o relatório técnico em PDF com evidências reais da interface, dos testes da API e da persistência no Neon.