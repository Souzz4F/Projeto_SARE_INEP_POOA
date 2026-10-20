
package com.sare_inep.fatec.importacao;

import com.sare_inep.fatec.model.Escola;
import com.sare_inep.fatec.repository.EscolaRepository;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class EscolaImportacaoService {

    private final EscolaRepository escolaRepository;

    public EscolaImportacaoService(EscolaRepository escolaRepository) {
        this.escolaRepository = escolaRepository;
    }

    @Transactional
    public Map<String, Integer> importar(MultipartFile arquivo) {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "Selecione um arquivo CSV válido."
            );
        }

        int lidas = 0;
        int importadas = 0;
        int rejeitadas = 0;
        int duplicadas = 0;

        Set<Long> codigosDoArquivo = new HashSet<>();

        CSVFormat formato = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setIgnoreHeaderCase(true)
                .setTrim(true)
                .setSkipHeaderRecord(true)
                .get();

        try (Reader leitorOriginal = new InputStreamReader(
                    arquivo.getInputStream(), StandardCharsets.UTF_8);
             BufferedReader leitor = new BufferedReader(leitorOriginal)) {

            // Remove o BOM, se existir, antes de interpretar o cabeçalho.
            leitor.mark(1);
            int primeiroCaractere = leitor.read();

            if (primeiroCaractere != 0xFEFF && primeiroCaractere != -1) {
                leitor.reset();
            }

            try (CSVParser parser = formato.parse(leitor)) {

                for (CSVRecord registro : parser) {
                    lidas++;

                    try {
                        String codigoTexto =
                                registro.get("Código INEP").trim();
                        String nome =
                                registro.get("Escola").trim();
                        String uf =
                                registro.get("UF").trim();
                        String municipio =
                                registro.get("Município").trim();
                        String dependencia =
                                registro.get("Dependência Administrativa").trim();
                        String localizacao =
                                registro.get("Localização").trim();
                        String restricao =
                                registro.get("Restrição de Atendimento").trim();

                        if (codigoTexto.isEmpty()
                                || nome.isEmpty()
                                || uf.isEmpty()
                                || municipio.isEmpty()
                                || dependencia.isEmpty()
                                || localizacao.isEmpty()) {
                            rejeitadas++;
                            continue;
                        }

                        long codigo = Long.parseLong(codigoTexto);

                        if (!codigosDoArquivo.add(codigo)
                                || escolaRepository.existsByCodigoEscola(codigo)) {
                            duplicadas++;
                            continue;
                        }

                        Escola escola = new Escola();
                        escola.setCodigoEscola(codigo);
                        escola.setNomeEscola(nome);
                        escola.setUf(uf);
                        escola.setMunicipio(municipio);
                        escola.setDependenciaAdministrativa(dependencia);
                        escola.setLocalizacao(localizacao);
                        escola.setRestricaoAtendimento(restricao);

                        escolaRepository.save(escola);
                        importadas++;

                    } catch (NumberFormatException e) {
                        rejeitadas++;
                    } catch (IllegalArgumentException e) {
                        rejeitadas++;
                    }
                }
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "Não foi possível ler o arquivo CSV.", e
            );
        }

        Map<String, Integer> resultado = new LinkedHashMap<>();
        resultado.put("linhasLidas", lidas);
        resultado.put("importadas", importadas);
        resultado.put("rejeitadas", rejeitadas);
        resultado.put("duplicadas", duplicadas);

        return resultado;
    }
}
