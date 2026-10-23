
package com.sare_inep.fatec.repository;

import com.sare_inep.fatec.model.Escola;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EscolaRepository extends JpaRepository<Escola, Long> {

    List<Escola> findByUfIgnoreCase(String uf);

    boolean existsByCodigoEscola(Long codigoEscola);

    @Query("""
        SELECT e FROM Escola e
        WHERE (:municipio IS NULL OR LOWER(e.municipio) = LOWER(:municipio))
          AND (:uf IS NULL OR LOWER(e.uf) = LOWER(:uf))
          AND (:localizacao IS NULL OR LOWER(e.localizacao) = LOWER(:localizacao))
          AND (:dependencia IS NULL OR LOWER(e.dependenciaAdministrativa) = LOWER(:dependencia))
        ORDER BY e.nomeEscola
        """)
    List<Escola> filtrarEscolas(
            @Param("municipio") String municipio,
            @Param("uf") String uf,
            @Param("localizacao") String localizacao,
            @Param("dependencia") String dependencia
    );
}


