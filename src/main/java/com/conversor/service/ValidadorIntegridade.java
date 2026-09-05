package com.conversor.service;

import com.conversor.model.FormatoArquivo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.zip.ZipFile;

public class ValidadorIntegridade {

    private static final Set<FormatoArquivo> FORMATOS_ZIP = Set.of(
            FormatoArquivo.DOCX, FormatoArquivo.XLSX, FormatoArquivo.PPTX,
            FormatoArquivo.ODT, FormatoArquivo.ODS
    );

    public void validar(File arquivo, FormatoArquivo formato) throws ConversaoException {
        if (!arquivo.exists() || arquivo.length() == 0) {
            throw new ConversaoException("O arquivo está vazio ou não foi encontrado: " + arquivo.getName());
        }

        if (formato == FormatoArquivo.PDF) {
            validarAssinaturaPdf(arquivo);
            return;
        }

        if (FORMATOS_ZIP.contains(formato)) {
            validarZip(arquivo);
        }
    }

    private void validarAssinaturaPdf(File arquivo) throws ConversaoException {
        byte[] cabecalho = new byte[5];
        try (FileInputStream fis = new FileInputStream(arquivo)) {
            int lidos = fis.read(cabecalho);
            String assinatura = lidos > 0 ? new String(cabecalho) : "";
            if (!assinatura.startsWith("%PDF")) {
                throw new ConversaoException("O arquivo não parece ser um PDF válido: " + arquivo.getName());
            }
        } catch (IOException e) {
            throw new ConversaoException("Não foi possível ler o arquivo: " + arquivo.getName());
        }
    }

    private void validarZip(File arquivo) throws ConversaoException {
        try (ZipFile zip = new ZipFile(arquivo)) {
            if (zip.size() == 0) {
                throw new ConversaoException("O arquivo parece estar corrompido: " + arquivo.getName());
            }
        } catch (IOException e) {
            throw new ConversaoException("O arquivo parece estar corrompido e não pôde ser aberto: " + arquivo.getName());
        }
    }
}