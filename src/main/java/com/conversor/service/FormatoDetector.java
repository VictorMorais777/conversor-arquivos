package com.conversor.service;

import com.conversor.model.FormatoArquivo;
import org.apache.tika.Tika;

import java.io.File;
import java.io.IOException;

public class FormatoDetector {

    private final Tika tika = new Tika();

    public FormatoArquivo detectar(File arquivo) throws IOException {
        if (arquivo == null || !arquivo.exists()) {
            throw new IOException("Arquivo não encontrado: " + arquivo);
        }

        String mimeType = tika.detect(arquivo);
        FormatoArquivo formato = porMimeType(mimeType);

        if (formato != null) {
            return formato;
        }
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