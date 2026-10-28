package com.sare_inep.fatec.service;

import com.sare_inep.fatec.model.Escola;
import com.sare_inep.fatec.repository.EscolaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Optional;

@Service
public class EscolaService {

    private final EscolaRepository escolaRepository;

    public EscolaService(EscolaRepository escolaRepository) {
        this.escolaRepository = escolaRepository;
    }

    public List<Escola> listarTodas() {
        return escolaRepository.findAll(Sort.by(Sort.Direction.ASC, "nomeEscola"));
    }

    public List<Escola> buscarPorUf(String uf) {
        if (uf == null || uf.trim().isEmpty()) {
            return listarTodas();
        }
        return escolaRepository.findByUfIgnoreCase(uf.trim());
    }

    public List<Escola> filtrarEscolas(
            String municipio,
            String uf,
            String localizacao,
            String dependencia,
            String termo) {

        String m = normalizarFiltro(municipio);
        String u = normalizarFiltro(uf);
        String l = normalizarFiltro(localizacao);
        String d = normalizarFiltro(dependencia);
        String t = normalizarFiltro(termo);

        if (m == null && u == null && l == null && d == null && t == null) {
            return listarTodas();
        }

        return escolaRepository.filtrarEscolas(m, u, l, d, t);
    }

    public Optional<Escola> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return escolaRepository.findById(id);
    }

    public Escola salvar(Escola escola) {
        validar(escola);
        padronizar(escola);
        return escolaRepository.save(escola);
    }

    public void excluir(Long id) {
        escolaRepository.deleteById(id);
    }

    public long contarTotal() {
        return escolaRepository.count();
    }

    public void validar(Escola escola) {
        if (escola == null) {
            throw new IllegalArgumentException("Os dados do estabelecimento escolar não foram informados.");
        }
        if (escola.getCodigoEscola() == null || escola.getCodigoEscola() <= 0) {
            throw new IllegalArgumentException("Código INEP é obrigatório e deve ser numérico positivo.");
        }
        if (escola.getNomeEscola() == null || escola.getNomeEscola().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome da escola é obrigatório.");
        }
        if (escola.getUf() == null || !Set.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO").contains(escola.getUf().trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Informe uma sigla válida de UF.");
        }
        if (escola.getMunicipio() == null || escola.getMunicipio().trim().isEmpty()) {
            throw new IllegalArgumentException("Município da escola é obrigatório.");
        }
        normalizarDependencia(escola.getDependenciaAdministrativa());
        normalizarLocalizacao(escola.getLocalizacao());
        normalizarSituacao(escola.getSituacaoFuncionamento());

        if (escola.getId() == null) {
            if (escolaRepository.existsByCodigoEscola(escola.getCodigoEscola())) {
                throw new IllegalArgumentException("Já existe uma escola cadastrada com o código INEP " + escola.getCodigoEscola() + ".");
            }
        } else if (escolaRepository.existsByCodigoEscolaAndIdNot(escola.getCodigoEscola(), escola.getId())) {
            throw new IllegalArgumentException("Já existe outra escola cadastrada com o código INEP " + escola.getCodigoEscola() + ".");
        }
    }

    private void padronizar(Escola escola) {
        escola.setNomeEscola(escola.getNomeEscola().trim());
        escola.setUf(escola.getUf().trim().toUpperCase(Locale.ROOT));
        escola.setMunicipio(escola.getMunicipio().trim());
        escola.setDependenciaAdministrativa(normalizarDependencia(escola.getDependenciaAdministrativa()));
        escola.setLocalizacao(normalizarLocalizacao(escola.getLocalizacao()));
        escola.setSituacaoFuncionamento(normalizarSituacao(escola.getSituacaoFuncionamento()));
        if (escola.getRestricaoAtendimento() != null) {
            escola.setRestricaoAtendimento(escola.getRestricaoAtendimento().trim());
        }
    }

    private String normalizarDependencia(String valor) {
        if (valor == null) throw new IllegalArgumentException("Dependência administrativa é obrigatória.");
        return switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "federal" -> "Federal";
            case "estadual" -> "Estadual";
            case "municipal" -> "Municipal";
            case "privada" -> "Privada";
            default -> throw new IllegalArgumentException("Dependência administrativa inválida.");
        };
    }

    private String normalizarLocalizacao(String valor) {
        if (valor == null) throw new IllegalArgumentException("Localização é obrigatória.");
        return switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "urbana", "urbano" -> "Urbana";
            case "rural" -> "Rural";
            default -> throw new IllegalArgumentException("Localização inválida; informe Urbana ou Rural.");
        };
    }

    private String normalizarSituacao(String valor) {
        if (valor == null || valor.isBlank()) return null;
        return switch (valor.trim().toLowerCase(Locale.ROOT)) {
            case "em atividade", "em funcionamento" -> "Em Atividade";
            case "paralisada" -> "Paralisada";
            case "extinta", "extinta no ano do censo", "extinta no censo" -> "Extinta";
            case "extinta em anos anteriores" -> "Extinta em anos anteriores";
            default -> throw new IllegalArgumentException("Situação de funcionamento inválida.");
        };
    }
    private String normalizarFiltro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}

