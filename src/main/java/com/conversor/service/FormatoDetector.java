package com.conversor.service;

import com.conversor.model.FormatoArquivo;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;

/**
 * Detecta o formato real de um arquivo lendo seu conteúdo (magic number / mime type),
 * em vez de confiar apenas na extensão do nome do arquivo.
 * Isso evita problemas com arquivos renomeados incorretamente ou corrompidos.
 */
public class FormatoDetector {

    private final Tika tika = new Tika();

    /**
     * Detecta o formato real do arquivo lendo seu conteúdo.
     *
     * @param arquivo o arquivo a ser analisado
     * @return o FormatoArquivo correspondente
     * @throws IOException se não for possível ler o arquivo
     * @throws FormatoNaoSuportadoException se o tipo detectado não for suportado pelo conversor
     */
    public FormatoArquivo detectar(File arquivo) throws IOException {
        if (arquivo == null || !arquivo.exists()) {
            throw new IOException("Arquivo não encontrado: " + arquivo);
        }

        String mimeType = tika.detect(arquivo);
        FormatoArquivo formato = porMimeType(mimeType);

        if (formato != null) {
            return formato;
        }

        // Fallback: alguns mime types do Tika para arquivos Office antigos/csv
        // podem vir genéricos (ex: text/plain para csv). Nesse caso, tenta pela extensão.
        String extensao = obterExtensao(arquivo.getName());
        formato = FormatoArquivo.porExtensao(extensao);

        if (formato != null) {
            return formato;
        }

        throw new FormatoNaoSuportadoException(
                "Tipo de arquivo não suportado. Mime detectado: " + mimeType
                        + " | Extensão: " + extensao
        );
    }

    /**
     * Busca o FormatoArquivo cujo mimeType bate com o detectado pelo Tika.
     */
    private FormatoArquivo porMimeType(String mimeType) {
        if (mimeType == null) {
            return null;
        }
        for (FormatoArquivo formato : FormatoArquivo.values()) {
            if (formato.getMimeType().equalsIgnoreCase(mimeType)) {
                return formato;
            }
        }
        return null;
    }

    private String obterExtensao(String nomeArquivo) {
        int ponto = nomeArquivo.lastIndexOf('.');
        if (ponto == -1 || ponto == nomeArquivo.length() - 1) {
            return "";
        }
        return nomeArquivo.substring(ponto + 1);
    }
}