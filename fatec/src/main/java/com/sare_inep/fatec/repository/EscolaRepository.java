package com.sare_inep.fatec.repository;

import com.sare_inep.fatec.model.Escola;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface EscolaRepository extends JpaRepository<Escola, Long> {

    List<Escola> findByUfIgnoreCase(String uf);

    boolean existsByCodigoEscola(Long codigoEscola);

    @Query("select e.codigoEscola from Escola e")
    Set<Long> listarCodigosEscola();

    boolean existsByCodigoEscolaAndIdNot(Long codigoEscola, Long id);

    @Query("""
        SELECT e FROM Escola e
        WHERE (CAST(:municipio AS string) IS NULL OR LOWER(e.municipio) = LOWER(CAST(:municipio AS string)))
          AND (CAST(:uf AS string) IS NULL OR LOWER(e.uf) = LOWER(CAST(:uf AS string)))
          AND (CAST(:localizacao AS string) IS NULL OR LOWER(e.localizacao) = LOWER(CAST(:localizacao AS string)))
          AND (CAST(:dependencia AS string) IS NULL OR LOWER(e.dependenciaAdministrativa) = LOWER(CAST(:dependencia AS string)))
          AND (CAST(:termo AS string) IS NULL OR LOWER(e.nomeEscola) LIKE LOWER(CONCAT('%', CAST(:termo AS string), '%'))
                                               OR CAST(e.codigoEscola AS string) LIKE CONCAT('%', CAST(:termo AS string), '%'))
        ORDER BY e.nomeEscola
        """)
    List<Escola> filtrarEscolas(
            @Param("municipio") String municipio,
            @Param("uf") String uf,
            @Param("localizacao") String localizacao,
            @Param("dependencia") String dependencia,
            @Param("termo") String termo
    );

    long countByDependenciaAdministrativaIgnoreCase(String dependencia);

    long countByLocalizacaoIgnoreCase(String localizacao);
}
