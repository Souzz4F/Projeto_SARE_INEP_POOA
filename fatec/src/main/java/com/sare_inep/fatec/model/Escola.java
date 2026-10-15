
    
package com.sare_inep.fatec.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "escolas")
public class Escola {
    
@Id
@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long codigoEscola;
    private String nomeEscola;
    private String uf;
    private String municipio;
    private String dependenciaAdministrativa;
    private String localizacao;
    private String situacaoFuncionamento;
    private Boolean possuiInternet;

    public Escola() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCodigoEscola() {
        return codigoEscola;
    }

    public void setCodigoEscola(Long codigoEscola) {
        this.codigoEscola = codigoEscola;
    }

    public String getNomeEscola() {
        return nomeEscola;
    }

    public void setNomeEscola(String nomeEscola) {
        this.nomeEscola = nomeEscola;
    }

    public String getUf() {
        return uf;
    }

    public void setUf(String uf) {
        this.uf = uf;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getDependenciaAdministrativa() {
        return dependenciaAdministrativa;
    }

    public void setDependenciaAdministrativa(String dependenciaAdministrativa) {
        this.dependenciaAdministrativa = dependenciaAdministrativa;
    }

    public String getLocalizacao() {
        return localizacao;
    }

    public void setLocalizacao(String localizacao) {
        this.localizacao = localizacao;
    }

    public String getSituacaoFuncionamento() {
        return situacaoFuncionamento;
    }

    public void setSituacaoFuncionamento(String situacaoFuncionamento) {
        this.situacaoFuncionamento = situacaoFuncionamento;
    }

    public Boolean getPossuiInternet() {
        return possuiInternet;
    }

    public void setPossuiInternet(Boolean possuiInternet) {
        this.possuiInternet = possuiInternet;
    }
}


