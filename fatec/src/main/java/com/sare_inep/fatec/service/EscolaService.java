
package com.sare_inep.fatec.service;

import com.sare_inep.fatec.model.Escola;
import com.sare_inep.fatec.repository.EscolaRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EscolaService {

    private final EscolaRepository escolaRepository;

    public EscolaService(EscolaRepository escolaRepository) {
        this.escolaRepository = escolaRepository;
    }

    public List<Escola> listarTodas() {
        return escolaRepository.findAll();
    }

    public List<Escola> buscarPorUf(String uf) {
        return escolaRepository.findByUfIgnoreCase(uf);
    }

    public List<Escola> filtrarEscolas(
            String municipio,
            String uf,
            String localizacao,
            String dependencia) {

        return escolaRepository.filtrarEscolas(
                normalizarFiltro(municipio),
                normalizarFiltro(uf),
                normalizarFiltro(localizacao),
                normalizarFiltro(dependencia)
        );
    }

    private String normalizarFiltro(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }

        return valor.trim();
    }

    public Optional<Escola> buscarPorId(Long id) {
        return escolaRepository.findById(id);
    }

    public Escola salvar(Escola escola) {
        return escolaRepository.save(escola);
    }

    public void excluir(Long id) {
        escolaRepository.deleteById(id);
    }
}

