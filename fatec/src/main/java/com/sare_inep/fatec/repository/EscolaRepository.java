
package com.sare_inep.fatec.repository;

import com.sare_inep.fatec.model.Escola;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EscolaRepository extends JpaRepository<Escola, Long> {

    List<Escola> findByUfIgnoreCase(String uf);

    boolean existsByCodigoEscola(Long codigoEscola);
}

