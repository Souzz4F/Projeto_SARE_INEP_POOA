
package com.sare_inep.fatec.controller;

import com.sare_inep.fatec.importacao.EscolaImportacaoService;
import com.sare_inep.fatec.model.Escola;
import com.sare_inep.fatec.service.EscolaService;

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

    @GetMapping
    public ResponseEntity<List<Escola>> listarTodas() {
        return ResponseEntity.ok(escolaService.listarTodas());
    }

    @GetMapping("/uf/{uf}")
    public ResponseEntity<List<Escola>> buscarPorUf(
            @PathVariable String uf) {
        return ResponseEntity.ok(escolaService.buscarPorUf(uf));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Escola> buscarPorId(
            @PathVariable Long id) {
        return escolaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Escola> salvar(
            @RequestBody Escola escola) {
        Escola escolaSalva = escolaService.salvar(escola);
        return ResponseEntity.ok(escolaSalva);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Escola> atualizar(
            @PathVariable Long id,
            @RequestBody Escola escola) {

        if (escolaService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        escola.setId(id);
        Escola escolaAtualizada = escolaService.salvar(escola);

        return ResponseEntity.ok(escolaAtualizada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id) {

        if (escolaService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        escolaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(
            value = "/importar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Map<String, Integer>> importar(
            @RequestParam("arquivo") MultipartFile arquivo) {

        return ResponseEntity.ok(
                escolaImportacaoService.importar(arquivo)
        );
    }
}
