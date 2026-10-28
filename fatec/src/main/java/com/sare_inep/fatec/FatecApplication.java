package com.sare_inep.fatec;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@SpringBootApplication
public class FatecApplication {

    public static void main(String[] args) {
        carregarDotEnv();
        SpringApplication.run(FatecApplication.class, args);
    }

    /**
     * Carrega automaticamente as variáveis do arquivo .env (caso exista)
     * para o System Properties antes do Spring Boot inicializar o DataSource.
     */
    private static void carregarDotEnv() {
        String[] caminhos = {".env", "fatec/.env", "../.env"};
        for (String caminho : caminhos) {
            File arquivo = new File(caminho);
            if (arquivo.exists() && arquivo.isFile()) {
                try {
                    List<String> linhas = Files.readAllLines(Paths.get(arquivo.getAbsolutePath()));
                    for (String linha : linhas) {
                        String limpa = linha.trim();
                        if (limpa.isEmpty() || limpa.startsWith("#")) {
                            continue;
                        }
                        int divisor = limpa.indexOf('=');
                        if (divisor > 0) {
                            String chave = limpa.substring(0, divisor).trim();
                            String valor = limpa.substring(divisor + 1).trim();

                            if ((valor.startsWith("\"") && valor.endsWith("\"")) ||
                                (valor.startsWith("'") && valor.endsWith("'"))) {
                                valor = valor.substring(1, valor.length() - 1);
                            }

                            // Tratamento especial para URLs do Neon/PostgreSQL
                            if ("DB_URL".equalsIgnoreCase(chave)) {
                                valor = normalizarUrlPostgres(valor);
                            }

                            if (System.getProperty(chave) == null && System.getenv(chave) == null) {
                                System.setProperty(chave, valor);
                            }
                        }
                    }
                    break;
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Garante que a URL do PostgreSQL possua o prefixo JDBC e extrai credenciais embutidas caso existam.
     */
    private static String normalizarUrlPostgres(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String limpa = url.trim();

        // Se for postgresql:// ou postgres://
        if (limpa.startsWith("postgresql://") || limpa.startsWith("postgres://")) {
            int schemeEnd = limpa.indexOf("://") + 3;
            String resto = limpa.substring(schemeEnd);

            // Verifica se possui usuário e senha embutidos (usuario:senha@host/db)
            int atIndex = resto.indexOf('@');
            if (atIndex > 0) {
                String credenciais = resto.substring(0, atIndex);
                String hostEDados = resto.substring(atIndex + 1);

                int colonIndex = credenciais.indexOf(':');
                if (colonIndex > 0) {
                    String usuario = credenciais.substring(0, colonIndex);
                    String senha = credenciais.substring(colonIndex + 1);

                    if (System.getProperty("DB_USERNAME") == null && System.getenv("DB_USERNAME") == null) {
                        System.setProperty("DB_USERNAME", usuario);
                    }
                    if (System.getProperty("DB_PASSWORD") == null && System.getenv("DB_PASSWORD") == null) {
                        System.setProperty("DB_PASSWORD", senha);
                    }
                }
                return "jdbc:postgresql://" + hostEDados;
            } else {
                return "jdbc:postgresql://" + resto;
            }
        }

        if (!limpa.startsWith("jdbc:")) {
            return "jdbc:" + limpa;
        }

        return limpa;
    }
}
