
        const API = "/api/escolas";

        const TAMANHO_PAGINA = 20;



        let todasEscolas = [];

        let escolasFiltradas = [];

        let pagina = 1;



        const corpoTabela = document.getElementById("corpoTabela");

        const status = document.getElementById("status");

        const mensagem = document.getElementById("mensagem");



        const filtroMunicipio = document.getElementById("filtroMunicipio");

        const filtroUf = document.getElementById("filtroUf");

        const filtroLocalizacao = document.getElementById("filtroLocalizacao");

        const filtroDependencia = document.getElementById("filtroDependencia");

        const pesquisa = document.getElementById("pesquisa");


        const CORES_GRAFICOS = ["#176B68", "#F4B183", "#8CBDB0", "#18364A", "#D7A58A", "#A9C8BD", "#6B8F89"];
        const graficos = {};

        // Exibe a porcentagem diretamente em cada gráfico.
        const pluginPorcentagens = {
            id: "rotulosPorcentagem",
            afterDatasetsDraw(chart) {
                const dataset = chart.data.datasets[0];
                if (!dataset || !dataset.data?.length) return;
                const total = dataset.data.reduce((soma, valor) => soma + (Number(valor) || 0), 0);
                if (!total) return;
                const ctx = chart.ctx;
                const tipo = chart.config.type;
                const meta = chart.getDatasetMeta(0);
                ctx.save();
                ctx.font = "600 11px Arial, sans-serif";
                ctx.textAlign = "center";
                ctx.textBaseline = "middle";
                meta.data.forEach((elemento, indice) => {
                    const valor = Number(dataset.data[indice]) || 0;
                    const percentual = (valor / total * 100).toLocaleString("pt-BR", { maximumFractionDigits: 1 }) + "%";
                    const posicao = elemento.tooltipPosition();
                    if (tipo === "doughnut" || tipo === "pie") {
                        // Mantém os rótulos legíveis e evita escrever em fatias muito pequenas.
                        if (valor / total < 0.045) return;
                        ctx.fillStyle = "#FFFFFF";
                        ctx.shadowColor = "rgba(0,0,0,0.25)";
                        ctx.shadowBlur = 2;
                        ctx.fillText(percentual, posicao.x, posicao.y);
                        ctx.shadowBlur = 0;
                    } else {
                        ctx.fillStyle = "#18364A";
                        if (chart.options.indexAxis === "y") {
                            ctx.textAlign = "left";
                            ctx.fillText(percentual, posicao.x + 6, posicao.y);
                        } else {
                            ctx.textAlign = "center";
                            ctx.fillText(percentual, posicao.x, posicao.y - 10);
                        }
                    }
                });
                ctx.restore();
            }
        };

        function contarPor(escolas, obterValor) {
            const contagem = new Map();
            escolas.forEach(escola => {
                const bruto = obterValor(escola);
                const valor = String(bruto ?? "").trim() || "Não informado";
                contagem.set(valor, (contagem.get(valor) || 0) + 1);
            });
            return [...contagem.entries()].sort((a, b) => b[1] - a[1]);
        }

        function corPorCategoria(id, rotulo, indice) {
            const chave = String(rotulo ?? "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();
            const coresFixas = {
                graficoLocalizacao: { urbana: "#176B68", rural: "#F4B183", "nao informado": "#8CBDB0" },
                graficoInternet: { "com internet": "#176B68", "sem internet": "#F4B183", "nao informado": "#8CBDB0" },
                graficoSituacao: { "em atividade": "#176B68", paralisada: "#F4B183", "nao informado": "#8CBDB0" },
                graficoDependencia: { municipal: "#176B68", estadual: "#F4B183", privada: "#8CBDB0", federal: "#18364A" }
            };
            const correspondencias = coresFixas[id] || {};
            return correspondencias[chave] || CORES_GRAFICOS[indice % CORES_GRAFICOS.length];
        }

        function criarOuAtualizarGrafico(id, tipo, dados, horizontal = false) {
            const canvas = document.getElementById(id);
            if (!canvas || typeof Chart === "undefined") return;

            const rotulos = dados.map(item => item[0]);
            const valores = dados.map(item => item[1]);
            const cores = rotulos.map((rotulo, i) => corPorCategoria(id, rotulo, i));
            const opcoes = {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: tipo === "doughnut" || tipo === "pie",
                        position: "bottom",
                        labels: { color: "#526B66", padding: 14, usePointStyle: true }
                    },
                    tooltip: { callbacks: { label: contexto => {
                        const valorBruto = contexto.raw;
                        const valor = Number(valorBruto) || 0;
                        const total = contexto.dataset.data.reduce((soma, item) => soma + (Number(item) || 0), 0);
                        const percentual = total ? (valor / total * 100).toLocaleString("pt-BR", { maximumFractionDigits: 1 }) : "0";
                        return ` ${contexto.label}: ${valor.toLocaleString("pt-BR")} escolas (${percentual}%)`;
                    } } }
                }
            };

            if (tipo === "bar") {
                opcoes.indexAxis = horizontal ? "y" : "x";
                opcoes.scales = {
                    x: { beginAtZero: true, ticks: { precision: 0, color: "#526B66" }, grid: { color: "#E8F3EF" } },
                    y: { beginAtZero: true, ticks: { color: "#526B66" }, grid: { color: "#E8F3EF" } }
                };
                opcoes.plugins.legend.display = false;
            }

            if (graficos[id]) {
                graficos[id].data.labels = rotulos;
                graficos[id].data.datasets[0].data = valores;
                graficos[id].data.datasets[0].backgroundColor = cores;
                graficos[id].update();
                return;
            }

            graficos[id] = new Chart(canvas, {
                type: tipo,
                data: { labels: rotulos, datasets: [{
                    label: "Quantidade de escolas",
                    data: valores,
                    backgroundColor: cores,
                    borderColor: "#FFFFFF",
                    borderWidth: tipo === "doughnut" ? 2 : 0,
                    borderRadius: tipo === "bar" ? 5 : 0
                }] },
                options: opcoes,
                plugins: [pluginPorcentagens]
            });
        }

        function atualizarGraficos(escolas) {
            const porMunicipio = contarPor(escolas, escola => escola.municipio).slice(0, 12);
            const porDependencia = contarPor(escolas, escola => escola.dependenciaAdministrativa);
            const porLocalizacao = contarPor(escolas, escola => escola.localizacao);
            const porSituacao = contarPor(escolas, escola => escola.situacaoFuncionamento);
            const porInternet = contarPor(escolas, escola => {
                const valor = escola.possuiInternet;
                if (valor === true || String(valor).toLowerCase() === "true" || String(valor).toLowerCase() === "sim") return "Com internet";
                if (valor === false || String(valor).toLowerCase() === "false" || String(valor).toLowerCase() === "não" || String(valor).toLowerCase() === "nao") return "Sem internet";
                return valor === null || valor === undefined || String(valor).trim() === "" ? "Não informado" : String(valor);
            });

            criarOuAtualizarGrafico("graficoMunicipios", "bar", porMunicipio, true);
            criarOuAtualizarGrafico("graficoDependencia", "doughnut", porDependencia);
            criarOuAtualizarGrafico("graficoLocalizacao", "doughnut", porLocalizacao);
            criarOuAtualizarGrafico("graficoSituacao", "bar", porSituacao, true);
            criarOuAtualizarGrafico("graficoInternet", "doughnut", porInternet);
        }



        function normalizar(valor) {

            return String(valor ?? "")

                .normalize("NFD")

                .replace(/[\u0300-\u036f]/g, "")

                .trim()

                .toLocaleLowerCase("pt-BR");

        }



        function mostrarMensagem(texto, tipo) {

            mensagem.textContent = texto;

            mensagem.className = tipo;

        }



        function limparMensagem() {

            mensagem.textContent = "";

            mensagem.className = "";

        }



        function obterMensagemErro(dados, padrao) {
            return dados?.mensagem || dados?.message || dados?.erro || dados?.error || padrao;
        }
        function preencherSelect(elemento, valores, textoInicial) {

            const valorAnterior = elemento.value;



            elemento.replaceChildren();



            const opcaoInicial = document.createElement("option");

            opcaoInicial.value = "";

            opcaoInicial.textContent = textoInicial;

            elemento.appendChild(opcaoInicial);



            const valoresUnicos = [...new Set(

                valores

                    .filter(valor => valor !== null && valor !== undefined)

                    .map(valor => String(valor).trim())

                    .filter(Boolean)

            )].sort((a, b) => a.localeCompare(b, "pt-BR"));



            valoresUnicos.forEach(valor => {

                const opcao = document.createElement("option");

                opcao.value = valor;

                opcao.textContent = valor;

                elemento.appendChild(opcao);

            });



            if (valoresUnicos.includes(valorAnterior)) {

                elemento.value = valorAnterior;

            }

        }



        function preencherFiltros() {

            preencherSelect(

                filtroMunicipio,

                todasEscolas.map(escola => escola.municipio),

                "Todos os municípios"

            );



            preencherSelect(

                filtroUf,

                todasEscolas.map(escola => escola.uf),

                "Todas as UFs"

            );



            preencherSelect(

                filtroLocalizacao,

                todasEscolas.map(escola => escola.localizacao),

                "Todas as localizações"

            );



            preencherSelect(

                filtroDependencia,

                todasEscolas.map(escola => escola.dependenciaAdministrativa),

                "Todas as dependências"

            );

        }



        function atualizarIndicadores(escolas = todasEscolas) {
            document.getElementById("totalEscolas").textContent =
                escolas.length.toLocaleString("pt-BR");
            document.getElementById("totalUrbanas").textContent =
                escolas.filter(escola => normalizar(escola.localizacao) === "urbana").length.toLocaleString("pt-BR");
            document.getElementById("totalRurais").textContent =
                escolas.filter(escola => normalizar(escola.localizacao) === "rural").length.toLocaleString("pt-BR");
        }

        const dialogEscola = document.getElementById("dialogEscola");
        const formEscola = document.getElementById("formEscola");
        let escolaEmEdicao = null;

        function abrirFormularioEscola(escola = null) {
            escolaEmEdicao = escola;
            formEscola.reset();
            document.getElementById("tituloFormularioEscola").textContent = escola ? "Editar escola" : "Nova escola";
            document.getElementById("campoCodigo").disabled = Boolean(escola);
            const campos = ["codigoEscola", "nomeEscola", "uf", "municipio", "dependenciaAdministrativa", "localizacao"];
            campos.forEach(campo => { const el = formEscola.elements[campo]; if (el) el.value = escola?.[campo] ?? ""; });

            const situacaoOriginal = normalizar(escola?.situacaoFuncionamento).replace(/[()]/g, "").replace(/\s+/g, " ");
            const situacoes = {
                "em atividade": "Em Atividade",
                "em funcionamento": "Em Atividade",
                "paralisada": "Paralisada",
                "extinta": "Extinta",
                "extinta ano do censo": "Extinta",
                "extinta em anos anteriores": "Extinta em anos anteriores"
            };
            const situacao = situacoes[situacaoOriginal] ?? escola?.situacaoFuncionamento ?? "";
            const campoSituacao = formEscola.elements.situacaoFuncionamento;
            if (situacao && !Array.from(campoSituacao.options).some(opcao => opcao.value === situacao)) {
                campoSituacao.add(new Option(situacao, situacao));
            }
            campoSituacao.value = situacao;
            const internet = formEscola.elements.possuiInternet;
            internet.value = escola?.possuiInternet == null ? "" : String(Boolean(escola.possuiInternet));
            dialogEscola.showModal();
        }

        document.getElementById("botaoNovaEscola").addEventListener("click", () => abrirFormularioEscola());
        document.getElementById("cancelarFormularioEscola").addEventListener("click", () => dialogEscola.close());

        formEscola.addEventListener("submit", async evento => {
            evento.preventDefault();
            const botao = document.getElementById("salvarEscola");
            const valor = nome => formEscola.elements[nome].value.trim();
            const codigoInformado = Number(valor("codigoEscola"));
            const nomeInformado = valor("nomeEscola");
            if (!Number.isSafeInteger(codigoInformado) || codigoInformado <= 0) {
                mostrarMensagem("O código INEP deve ser um número inteiro positivo.", "erro");
                return;
            }
            const duplicada = todasEscolas.find(escola =>
                (!escolaEmEdicao || String(escola.id) !== String(escolaEmEdicao.id)) &&
                Number(escola.codigoEscola) === codigoInformado
            );
            if (duplicada) {
                const motivo = "Já existe uma escola cadastrada com esse código INEP.";
                mostrarMensagem(motivo, "erro");
                window.alert(motivo);
                return;
            }
            const payload = {
                codigoEscola: codigoInformado,
                nomeEscola: nomeInformado, uf: valor("uf").toUpperCase(),
                municipio: valor("municipio"), dependenciaAdministrativa: valor("dependenciaAdministrativa"),
                localizacao: valor("localizacao"), situacaoFuncionamento: valor("situacaoFuncionamento") || null,
                // Preserva eventual dado antigo sem exigir que o usuário preencha esse campo.
                restricaoAtendimento: escolaEmEdicao?.restricaoAtendimento ?? null,
                possuiInternet: valor("possuiInternet") === "" ? null : valor("possuiInternet") === "true"
            };
            botao.disabled = true; botao.textContent = "Salvando...";
            try {
                const url = escolaEmEdicao ? API + "/" + escolaEmEdicao.id : API;
                const resposta = await fetch(url, { method: escolaEmEdicao ? "PUT" : "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload) });
                if (!resposta.ok) {
                    let detalhe = "HTTP " + resposta.status;
                    try { detalhe = obterMensagemErro(await resposta.json(), detalhe); } catch (_) {}
                    throw new Error(detalhe);
                }
                dialogEscola.close();
                await carregarEscolas();
                mostrarMensagem(escolaEmEdicao ? "Escola atualizada com sucesso." : "Escola cadastrada com sucesso.", "sucesso");
            } catch (erro) {
                const textoErro = "Não foi possível salvar a escola: " + erro.message;
                mostrarMensagem(textoErro, "erro");
                window.alert(textoErro);
            } finally { botao.disabled = false; botao.textContent = "Salvar escola"; }
        });

        async function excluirEscola(escola) {
            if (!confirm(`Deseja realmente excluir a escola "${escola.nomeEscola}"? Esta ação não pode ser desfeita.`)) return;
            try {
                const resposta = await fetch(API + "/" + escola.id, { method: "DELETE" });
                if (!resposta.ok) { let corpo = {}; try { corpo = await resposta.json(); } catch (_) {} throw new Error(obterMensagemErro(corpo, "HTTP " + resposta.status)); }
                await carregarEscolas();
                mostrarMensagem("Escola excluída com sucesso.", "sucesso");
            } catch (erro) { mostrarMensagem("Não foi possível excluir a escola: " + erro.message, "erro"); }
        }

        async function carregarEscolas() {

            corpoTabela.innerHTML =

                '<tr><td colspan="7" class="vazio">Carregando escolas...</td></tr>';



            status.textContent = "Consultando a base de dados...";



            try {

                const resposta = await fetch(API);



                if (!resposta.ok) {

                    throw new Error(

                        "A API retornou o status HTTP " + resposta.status

                    );

                }



                const dados = await resposta.json();



                if (!Array.isArray(dados)) {

                    throw new Error("A resposta da API não contém uma lista de escolas.");

                }



                todasEscolas = dados;



                preencherFiltros();

                atualizarIndicadores();

                aplicarFiltros();



                limparMensagem();

            } catch (erro) {

                corpoTabela.innerHTML =

                    '<tr><td colspan="6" class="vazio">Não foi possível carregar as escolas.</td></tr>';



                status.textContent = "Erro ao consultar a API.";



                mostrarMensagem(

                    "Não foi possível consultar as escolas. Verifique se o Spring Boot está em execução. Detalhe: " +

                    erro.message,

                    "erro"

                );

            }

        }



        function aplicarFiltros() {

            const termo = normalizar(pesquisa.value);

            const municipio = normalizar(filtroMunicipio.value);

            const uf = normalizar(filtroUf.value);

            const localizacao = normalizar(filtroLocalizacao.value);

            const dependencia = normalizar(filtroDependencia.value);



            escolasFiltradas = todasEscolas.filter(escola => {

                const nome = normalizar(escola.nomeEscola);

                const codigo = normalizar(escola.codigoEscola);

                const municipioEscola = normalizar(escola.municipio);

                const ufEscola = normalizar(escola.uf);

                const localizacaoEscola = normalizar(escola.localizacao);

                const dependenciaEscola =

                    normalizar(escola.dependenciaAdministrativa);



                const correspondeTexto =

                    !termo ||

                    nome.includes(termo) ||

                    codigo.includes(termo);



                const correspondeMunicipio =

                    !municipio || municipioEscola === municipio;



                const correspondeUf =

                    !uf || ufEscola === uf;



                const correspondeLocalizacao =

                    !localizacao || localizacaoEscola === localizacao;



                const correspondeDependencia =

                    !dependencia || dependenciaEscola === dependencia;



                return correspondeTexto &&

                    correspondeMunicipio &&

                    correspondeUf &&

                    correspondeLocalizacao &&

                    correspondeDependencia;

            });



            pagina = 1;
            atualizarContadorFiltros();
            atualizarIndicadores(escolasFiltradas);
            atualizarGraficos(escolasFiltradas);
            renderizarTabela();

        }



        function atualizarContadorFiltros() {

            const quantidade = [

                filtroMunicipio.value,

                filtroUf.value,

                filtroLocalizacao.value,

                filtroDependencia.value,

                pesquisa.value.trim()

            ].filter(Boolean).length;



            document.getElementById("contadorFiltros").textContent =

                quantidade === 0

                    ? "Nenhum filtro aplicado"

                    : quantidade + (quantidade === 1

                        ? " critério aplicado"

                        : " critérios aplicados");

        }



        function criarCelula(valor, classe = "") {

            const celula = document.createElement("td");

            celula.textContent =

                valor !== null && valor !== undefined && valor !== ""

                    ? valor

                    : "—";



            if (classe) {

                celula.className = classe;

            }



            return celula;

        }



        function criarCelulaSelo(valor) {

            const celula = document.createElement("td");



            if (valor === null || valor === undefined || valor === "") {

                celula.textContent = "—";

                return celula;

            }



            const selo = document.createElement("span");

            selo.className = "selo";

            selo.textContent = valor;

            celula.appendChild(selo);



            return celula;

        }



        function renderizarTabela() {

            corpoTabela.replaceChildren();



            const total = escolasFiltradas.length;

            const totalPaginas = Math.max(

                1,

                Math.ceil(total / TAMANHO_PAGINA)

            );



            if (pagina > totalPaginas) {

                pagina = totalPaginas;

            }



            const inicio = (pagina - 1) * TAMANHO_PAGINA;

            const escolasPagina = escolasFiltradas.slice(

                inicio,

                inicio + TAMANHO_PAGINA

            );



            if (escolasPagina.length === 0) {

                const linha = document.createElement("tr");

                const celula = document.createElement("td");



                celula.colSpan = 7;

                celula.className = "vazio";

                celula.textContent = todasEscolas.length === 0

                    ? "Não há escolas cadastradas."

                    : "Nenhuma escola encontrada com os filtros selecionados.";



                linha.appendChild(celula);

                corpoTabela.appendChild(linha);

            } else {

                escolasPagina.forEach(escola => {

                    const linha = document.createElement("tr");



                    linha.appendChild(

                        criarCelula(escola.codigoEscola, "codigo")

                    );

                    linha.appendChild(

                        criarCelula(escola.nomeEscola, "nome-escola")

                    );

                    linha.appendChild(criarCelula(escola.municipio));

                    linha.appendChild(criarCelulaSelo(escola.uf));

                    linha.appendChild(

                        criarCelula(escola.dependenciaAdministrativa)

                    );

                    linha.appendChild(

                        criarCelulaSelo(escola.localizacao)

                    );
                    const celulaAcoes = document.createElement("td");
                    const grupoAcoes = document.createElement("div");
                    grupoAcoes.className = "acoes-tabela";
                    const editar = document.createElement("button");
                    editar.type = "button"; editar.className = "botao-pequeno"; editar.textContent = "Editar";
                    editar.addEventListener("click", () => abrirFormularioEscola(escola));
                    const excluir = document.createElement("button");
                    excluir.type = "button"; excluir.className = "botao-pequeno excluir"; excluir.textContent = "Excluir";
                    excluir.addEventListener("click", () => excluirEscola(escola));
                    grupoAcoes.append(editar, excluir); celulaAcoes.appendChild(grupoAcoes); linha.appendChild(celulaAcoes);

                    corpoTabela.appendChild(linha);

                });

            }



            status.textContent =

                total.toLocaleString("pt-BR") +

                (total === 1

                    ? " escola encontrada"

                    : " escolas encontradas");



            document.getElementById("paginaAtual").textContent =

                "Página " + pagina + " de " + totalPaginas;



            document.getElementById("botaoAnterior").disabled =

                pagina <= 1;



            document.getElementById("botaoProximo").disabled =

                pagina >= totalPaginas;

        }



        document.getElementById("botaoPesquisar")

            .addEventListener("click", aplicarFiltros);



        pesquisa.addEventListener("keydown", evento => {

            if (evento.key === "Enter") {

                evento.preventDefault();

                aplicarFiltros();

            }

        });



        [

            filtroMunicipio,

            filtroUf,

            filtroLocalizacao,

            filtroDependencia

        ].forEach(filtro => {

            filtro.addEventListener("change", aplicarFiltros);

        });



        document.getElementById("botaoLimpar")

            .addEventListener("click", () => {

                pesquisa.value = "";

                filtroMunicipio.value = "";

                filtroUf.value = "";

                filtroLocalizacao.value = "";

                filtroDependencia.value = "";



                aplicarFiltros();

            });



        document.getElementById("botaoAnterior")

            .addEventListener("click", () => {

                if (pagina > 1) {

                    pagina--;

                    renderizarTabela();

                }

            });



        document.getElementById("botaoProximo")

            .addEventListener("click", () => {

                const totalPaginas = Math.ceil(

                    escolasFiltradas.length / TAMANHO_PAGINA

                );



                if (pagina < totalPaginas) {

                    pagina++;

                    renderizarTabela();

                }

            });



        document.getElementById("formImportacao")

            .addEventListener("submit", async evento => {

                evento.preventDefault();

                limparMensagem();



                const arquivo = document.getElementById("arquivo").files[0];



                if (!arquivo) {

                    mostrarMensagem("Selecione um arquivo CSV.", "erro");

                    return;

                }



                const botao = document.getElementById("botaoImportar");

                const dados = new FormData();



                dados.append("arquivo", arquivo);

                botao.disabled = true;

                botao.textContent = "Importando...";



                try {

                    const resposta = await fetch(API + "/importar", {

                        method: "POST",

                        body: dados

                    });



                    let resultado = {};
                    try { resultado = await resposta.json(); } catch (_) {}

                    if (!resposta.ok) {
                        throw new Error(obterMensagemErro(
                            resultado,
                            "Falha na importação (HTTP " + resposta.status + ")."
                        ));
                    }
                    document.getElementById("arquivo").value = "";

                    // Atualiza a tabela e os indicadores antes de exibir o retorno,
                    // pois carregarEscolas() limpa mensagens antigas.
                    await carregarEscolas();

                    const linhasLidas = resultado.linhasLidas ?? resultado.lidas ?? "—";
                    const importadas = resultado.importadas ?? 0;
                    const rejeitadas = resultado.rejeitadas ?? 0;
                    const duplicadas = resultado.duplicadas ?? 0;
                    const motivos = Object.entries(resultado.motivosRejeicao || {})
                        .map(([motivo, quantidade]) => motivo + ": " + quantidade)
                        .join("; ");
                    mostrarMensagem(
                        "Importação concluída! Linhas lidas: " + linhasLidas +
                        "; novas escolas importadas: " + importadas +
                        "; rejeitadas: " + rejeitadas +
                        "; duplicadas: " + duplicadas + "." +
                        (motivos ? " Motivos de rejeição: " + motivos + "." : ""),
                        "sucesso"
                    );
                } catch (erro) {

                    mostrarMensagem(

                        "Não foi possível concluir a importação: " +

                        erro.message,

                        "erro"

                    );

                } finally {

                    botao.disabled = false;

                    botao.textContent = "Importar arquivo";

                }

            });



        carregarEscolas();


        // Busca por ID (Demonstração 404 e detalhes de registro)
        const botaoBuscarId = document.getElementById('botaoBuscarId');
        const campoBuscaId = document.getElementById('campoBuscaId');

        if (botaoBuscarId && campoBuscaId) {
            async function buscarPorIdApi() {
                const id = campoBuscaId.value.trim();
                if (!id) {
                    mostrarMensagem('Informe um ID numérico para pesquisar.', 'erro');
                    return;
                }
                try {
                    const resposta = await fetch(API + '/' + id);
                    if (resposta.status === 404) {
                        mostrarMensagem('Escola com ID ' + id + ' NÃO encontrada no banco de dados! (Status HTTP 404 Not Found)', 'erro');
                        return;
                    }
                    if (!resposta.ok) {
                        mostrarMensagem('Erro na requisição: status HTTP ' + resposta.status, 'erro');
                        return;
                    }
                    const escola = await resposta.json();
                    escolasFiltradas = [escola];
                    pagina = 1;
                    renderizarTabela();
                    mostrarMensagem('Escola encontrada: #' + escola.id + ' - ' + escola.nomeEscola + ' (' + escola.municipio + '/' + escola.uf + ') - HTTP 200 OK', 'sucesso');
                } catch (e) {
                    mostrarMensagem('Erro ao buscar ID: ' + e.message, 'erro');
                }
            }

            botaoBuscarId.addEventListener('click', buscarPorIdApi);
            campoBuscaId.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    buscarPorIdApi();
                }
            });
        }
