package com.sare_inep.fatec.service;

import com.sare_inep.fatec.model.Escola;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import com.sare_inep.fatec.repository.EscolaRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
public class EscolaImportacaoService {

    private final EscolaRepository escolaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public EscolaImportacaoService(EscolaRepository escolaRepository) {
        this.escolaRepository = escolaRepository;
    }

    @Transactional
    public Map<String, Object> importar(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Selecione um arquivo CSV válido. O arquivo enviado está vazio.");
        }

        String nomeOriginal = arquivo.getOriginalFilename();
        if (nomeOriginal == null || !nomeOriginal.toLowerCase().endsWith(".csv")) {
            throw new IllegalArgumentException("Formato de arquivo incompatível. Apenas arquivos com extensão .csv são aceitos.");
        }

        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Não foi possível ler o conteúdo do arquivo: " + e.getMessage(), e);
        }

        if (conteudo.length == 0) {
            throw new IllegalArgumentException("O arquivo selecionado está vazio.");
        }

        // Detecta charset e remove BOM se houver
        Charset charset = detectarCharset(conteudo);
        int offset = 0;
        if (conteudo.length >= 3 && (conteudo[0] & 0xFF) == 0xEF && (conteudo[1] & 0xFF) == 0xBB && (conteudo[2] & 0xFF) == 0xBF) {
            offset = 3;
        }

        // Lê a primeira linha para identificar o separador e validar se o arquivo tem conteúdo
        String primeiraLinha;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(conteudo, offset, conteudo.length - offset), charset))) {
            primeiraLinha = br.readLine();
        } catch (IOException e) {
            throw new IllegalArgumentException("Erro ao ler o cabeçalho do arquivo: " + e.getMessage(), e);
        }

        if (primeiraLinha == null || primeiraLinha.trim().isEmpty()) {
            throw new IllegalArgumentException("O arquivo CSV selecionado não possui linhas de dados ou cabeçalho.");
        }

        char delimitador = primeiraLinha.contains(";") ? ';' : ',';

        int lidas = 0;
        int importadas = 0;
        int rejeitadas = 0;
        int duplicadas = 0;
        Map<String, Integer> motivosRejeicao = new LinkedHashMap<>();


        List<Escola> loteParaSalvar = new ArrayList<>();

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setDelimiter(delimitador)
                .setHeader()
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .get();

        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(conteudo, offset, conteudo.length - offset), charset));
             CSVParser parser = formato.parse(leitor)) {

            Map<String, Integer> cabecalho = parser.getHeaderMap();
            if (cabecalho == null || cabecalho.isEmpty()) {
                throw new IllegalArgumentException("Cabeçalho ausente ou não reconhecido no arquivo CSV.");
            }

            // Validação estrutural do cabeçalho: verifica se existem colunas essenciais do Censo Escolar/INEP
            boolean temCodigo = cabecalhoContem(cabecalho, "Código INEP", "codigoEscola", "CO_ENTIDADE", "COD_ESCOLA", "CODIGO_INEP", "CO_ESCOLA");
            boolean temNome = cabecalhoContem(cabecalho, "Escola", "nomeEscola", "NO_ENTIDADE", "NOME", "NO_ESCOLA", "NOME_ESCOLA");
            boolean temUf = cabecalhoContem(cabecalho, "UF", "SG_UF", "ESTADO", "CO_UF");
            boolean temMunicipio = cabecalhoContem(cabecalho, "Município", "Municipio", "NO_MUNICIPIO", "CIDADE");
            boolean temDependencia = cabecalhoContem(cabecalho, "Dependência Administrativa", "Dependencia Administrativa", "TP_DEPENDENCIA", "REDE", "DEPENDENCIA_ADMINISTRATIVA");
            boolean temLocalizacao = cabecalhoContem(cabecalho, "Localização", "Localizacao", "TP_LOCALIZACAO", "LOCALIZACAO");

            if (!temCodigo || !temNome || !temUf || !temMunicipio || !temDependencia || !temLocalizacao) {
                throw new IllegalArgumentException(
                        "Erro estrutural no cabeçalho: O arquivo não possui colunas reconhecidas do Censo Escolar/INEP " +
                        "(eram esperadas colunas para código, nome, UF, município, dependência administrativa e localização)."
                );
            }

            Set<Long> codigosProcessados = new HashSet<>(escolaRepository.listarCodigosEscola());
            for (CSVRecord registro : parser) {
                lidas++;

                if (registro.size() != cabecalho.size()) {
                    rejeitadas++;
                    contabilizarRejeicao(motivosRejeicao, "Quantidade de colunas diferente do cabeçalho");
                    continue;
                }

                try {
                    String codigoTexto = extrairCampo(registro, "Código INEP", "codigoEscola", "CO_ENTIDADE", "COD_ESCOLA", "CODIGO_INEP", "CO_ESCOLA");
                    String nome = extrairCampo(registro, "Escola", "nomeEscola", "NO_ENTIDADE", "NOME", "NO_ESCOLA", "NOME_ESCOLA");
                    String uf = extrairCampo(registro, "UF", "SG_UF", "ESTADO", "CO_UF");
                    String municipio = extrairCampo(registro, "Município", "Municipio", "NO_MUNICIPIO", "CIDADE", "MUNICÍPIO");
                    String dependencia = extrairCampo(registro, "Dependência Administrativa", "Dependencia Administrativa", "TP_DEPENDENCIA", "REDE", "DEPENDENCIA_ADMINISTRATIVA");
                    String localizacao = extrairCampo(registro, "Localização", "Localizacao", "TP_LOCALIZACAO", "LOCALIZACAO");
                    String restricao = extrairCampo(registro, "Restrição de Atendimento", "Restricao de Atendimento", "restricaoAtendimento", "TP_RESTRICAO_ATENDIMENTO");
                    String situacao = extrairCampo(registro, "Situação de Funcionamento", "Situacao de Funcionamento", "TP_SITUACAO_FUNCIONAMENTO", "situacaoFuncionamento");
                    String internet = extrairCampo(registro, "Possui Internet", "Internet", "IN_INTERNET", "possuiInternet");

                    if (codigoTexto.isEmpty() || nome.isEmpty() || uf.isEmpty()
                            || municipio.isEmpty() || dependencia.isEmpty() || localizacao.isEmpty()) {
                        rejeitadas++;
                        contabilizarRejeicao(motivosRejeicao, "Campos obrigatórios ausentes");
                        continue;
                    }
                    if (!codigoTexto.matches("[0-9]+")) {
                        rejeitadas++;
                        contabilizarRejeicao(motivosRejeicao, "Código INEP inválido");
                        continue;
                    }

                    long codigo = Long.parseLong(codigoTexto);
                    if (codigo <= 0) {
                        rejeitadas++;
                        contabilizarRejeicao(motivosRejeicao, "Código INEP deve ser positivo");
                        continue;
                    }
                    if (codigosProcessados.contains(codigo)) {
                        duplicadas++;
                        continue;
                    }

                    Escola escola = new Escola();
                    escola.setCodigoEscola(codigo);
                    escola.setNomeEscola(nome.trim());
                    escola.setUf(decodificarUf(uf));
                    escola.setMunicipio(municipio.trim());
                    escola.setDependenciaAdministrativa(decodificarDependencia(dependencia));
                    escola.setLocalizacao(decodificarLocalizacao(localizacao));
                    escola.setRestricaoAtendimento(restricao.isEmpty() ? null : restricao.trim());
                    escola.setSituacaoFuncionamento(decodificarSituacao(situacao));
                    escola.setPossuiInternet(decodificarInternet(internet));
                    codigosProcessados.add(codigo);
                    loteParaSalvar.add(escola);
                    importadas++;
                } catch (NumberFormatException e) {
                    rejeitadas++;
                    contabilizarRejeicao(motivosRejeicao, "Código INEP fora do intervalo permitido");
                } catch (IllegalArgumentException e) {
                    rejeitadas++;
                    contabilizarRejeicao(motivosRejeicao, e.getMessage());
                }

                if (loteParaSalvar.size() >= 100) {
                    escolaRepository.saveAll(loteParaSalvar);
                    entityManager.flush();
                    entityManager.clear();
                    loteParaSalvar.clear();
                }
            }
            if (!loteParaSalvar.isEmpty()) {
                escolaRepository.saveAll(loteParaSalvar);
                entityManager.flush();
                entityManager.clear();
                loteParaSalvar.clear();
            }

        } catch (IOException e) {
            throw new IllegalArgumentException("Falha na leitura do arquivo CSV: " + e.getMessage(), e);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("linhasLidas", lidas);
        resultado.put("importadas", importadas);
        resultado.put("duplicadas", duplicadas);
        resultado.put("rejeitadas", rejeitadas);
        resultado.put("motivosRejeicao", motivosRejeicao);
        resultado.put("mensagem", String.format(
                "Processamento concluído: %d linhas lidas, %d importadas com sucesso, %d duplicadas ignoradas e %d rejeitadas.",
                lidas, importadas, duplicadas, rejeitadas
        ));

        return resultado;
    }

    private boolean cabecalhoContem(Map<String, Integer> cabecalho, String... aliases) {
        for (String alias : aliases) {
            for (String key : cabecalho.keySet()) {
                if (key != null && key.trim().equalsIgnoreCase(alias)) {
                    return true;
                }
            }
        }
        return false;
    }

    private String extrairCampo(CSVRecord registro, String... aliases) {
        for (String alias : aliases) {
            if (registro.isMapped(alias)) {
                String val = registro.get(alias);
                if (val != null && !val.isBlank()) {
                    return val.trim();
                }
            }
        }
        return "";
    }

    private void contabilizarRejeicao(Map<String, Integer> motivos, String motivo) {
        String descricao = motivo == null || motivo.isBlank() ? "Registro inválido" : motivo;
        motivos.merge(descricao, 1, Integer::sum);
    }

    private String decodificarUf(String valor) {
        String limpo = valor.trim().toUpperCase(Locale.ROOT);
        if (limpo.matches("[0-9]{1,2}")) {
            limpo = switch (limpo) {
                case "11" -> "RO";
                case "12" -> "AC";
                case "13" -> "AM";
                case "14" -> "RR";
                case "15" -> "PA";
                case "16" -> "AP";
                case "17" -> "TO";
                case "21" -> "MA";
                case "22" -> "PI";
                case "23" -> "CE";
                case "24" -> "RN";
                case "25" -> "PB";
                case "26" -> "PE";
                case "27" -> "AL";
                case "28" -> "SE";
                case "29" -> "BA";
                case "31" -> "MG";
                case "32" -> "ES";
                case "33" -> "RJ";
                case "35" -> "SP";
                case "41" -> "PR";
                case "42" -> "SC";
                case "43" -> "RS";
                case "50" -> "MS";
                case "51" -> "MT";
                case "52" -> "GO";
                case "53" -> "DF";
                default -> throw new IllegalArgumentException("Código de UF inválido: " + valor);
            };
        }
        if (Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO").contains(limpo)) {
            return limpo;
        }
        throw new IllegalArgumentException("UF inválida: " + valor);
    }

    private String decodificarDependencia(String valor) {
        return switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "1", "federal" -> "Federal";
            case "2", "estadual" -> "Estadual";
            case "3", "municipal" -> "Municipal";
            case "4", "privada" -> "Privada";
            default -> throw new IllegalArgumentException("Dependência administrativa inválida: " + valor);
        };
    }

    private String decodificarLocalizacao(String valor) {
        return switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "1", "urbana", "urbano" -> "Urbana";
            case "2", "rural" -> "Rural";
            default -> throw new IllegalArgumentException("Localização inválida: " + valor);
        };
    }

    private String decodificarSituacao(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "1", "em atividade", "em funcionamento" -> "Em Atividade";
            case "2", "paralisada" -> "Paralisada";
            case "3", "extinta", "extinta no ano do censo", "extinta no censo" -> "Extinta";
            case "4", "extinta em anos anteriores" -> "Extinta em anos anteriores";
            default -> throw new IllegalArgumentException("Situação de funcionamento inválida: " + valor);
        };
    }

    private Boolean decodificarInternet(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "1", "sim", "s", "true" -> true;
            case "0", "não", "nao", "n", "false" -> false;
            default -> throw new IllegalArgumentException("Indicador de internet inválido: " + valor);
        };
    }
    private Charset detectarCharset(byte[] bytes) {
        // Tenta decodificar como UTF-8
        try {
            StandardCharsets.UTF_8.newDecoder().decode(java.nio.ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (Exception e) {
            return StandardCharsets.ISO_8859_1;
        }
    }
}

