package com.sare_inep.fatec.controller;

import com.sare_inep.fatec.model.Escola;
import com.sare_inep.fatec.service.EscolaImportacaoService;
import com.sare_inep.fatec.service.EscolaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/escolas")
public class EscolaController {

    private final EscolaService escolaService;
    private final EscolaImportacaoService escolaImportacaoService;

    public EscolaController(
            EscolaService escolaService,
            EscolaImportacaoService escolaImportacaoService) {
        this.escolaService = escolaService;
        this.escolaImportacaoService = escolaImportacaoService;
    }

    // 1. GET /api/escolas - Listar todos os registros (com suporte opcional a parâmetros de busca)
    @GetMapping
    public ResponseEntity<List<Escola>> listarTodas(
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) String localizacao,
            @RequestParam(required = false) String dependencia,
            @RequestParam(required = false) String termo) {

        if (municipio != null || uf != null || localizacao != null || dependencia != null || termo != null) {
            return ResponseEntity.ok(
                    escolaService.filtrarEscolas(municipio, uf, localizacao, dependencia, termo)
            );
        }

        return ResponseEntity.ok(escolaService.listarTodas());
    }

    // 2. GET /api/escolas/filtro - Endpoint obrigatório da Seção 8 do Edital
    @GetMapping("/filtro")
    public ResponseEntity<List<Escola>> filtrar(
            @RequestParam(required = false) String municipio,
            @RequestParam(required = false) String uf,
            @RequestParam(required = false) String localizacao,
            @RequestParam(required = false) String dependencia,
            @RequestParam(required = false) String termo) {
        return ResponseEntity.ok(
                escolaService.filtrarEscolas(municipio, uf, localizacao, dependencia, termo)
        );
    }

    // 3. GET /api/escolas/{id} - Buscar registro por ID (200 OK ou 404 Not Found com Optional)
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        return escolaService.buscarPorId(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of(
                                "status", 404,
                                "erro", "Não Encontrado",
                                "mensagem", "Escola com ID " + id + " não foi encontrada no sistema."
                        )));
    }

    // 4. POST /api/escolas - Cadastrar registro (201 Created ou 400 Bad Request)
    @PostMapping
    public ResponseEntity<?> salvar(@RequestBody Escola escola) {
        try {
            if (escola != null) escola.setId(null);
            Escola salva = escolaService.salvar(escola);
            return ResponseEntity.status(HttpStatus.CREATED).body(salva);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "status", 400,
                            "erro", "Dados Inválidos",
                            "mensagem", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "erro", "Erro no Cadastro",
                            "mensagem", "Não foi possível cadastrar a escola devido a um erro interno."
                    ));
        }
    }

    // 5. PUT /api/escolas/{id} - Atualizar registro (200 OK, 400 Bad Request ou 404 Not Found)
    @PutMapping("/{id}")
    public ResponseEntity<?> atualizar(
            @PathVariable Long id,
            @RequestBody Escola escola) {

        if (escolaService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "status", 404,
                            "erro", "Não Encontrado",
                            "mensagem", "Não é possível atualizar: Escola com ID " + id + " não existe."
                    ));
        }

        try {
            if (escola != null) escola.setId(id);
            Escola atualizada = escolaService.salvar(escola);
            return ResponseEntity.ok(atualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "status", 400,
                            "erro", "Dados Inválidos",
                            "mensagem", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "erro", "Erro na Atualização",
                            "mensagem", "Não foi possível atualizar a escola devido a um erro interno."
                    ));
        }
    }

    // 6. DELETE /api/escolas/{id} - Excluir registro (204 No Content ou 404 Not Found)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> excluir(@PathVariable Long id) {
        if (escolaService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(
                            "status", 404,
                            "erro", "Não Encontrado",
                            "mensagem", "Não é possível excluir: Escola com ID " + id + " não existe."
                    ));
        }

        escolaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    // 7. POST /api/escolas/importar - Importar arquivo CSV multipart/form-data (200 OK ou 400 Bad Request)
    @PostMapping(
            value = "/importar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> importar(@RequestParam("arquivo") MultipartFile arquivo) {
        try {
            Map<String, Object> resultado = escolaImportacaoService.importar(arquivo);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of(
                            "status", 400,
                            "erro", "Falha na Importação",
                            "mensagem", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", 500,
                            "erro", "Erro na Importação",
                            "mensagem", "Não foi possível concluir a importação devido a um erro interno."
                    ));
        }
    }

    // 8. GET /api/escolas/uf/{uf} - Buscar escolas por UF (200 OK)
    @GetMapping("/uf/{uf}")
    public ResponseEntity<List<Escola>> buscarPorUf(@PathVariable String uf) {
        return ResponseEntity.ok(escolaService.buscarPorUf(uf));
    }
}
