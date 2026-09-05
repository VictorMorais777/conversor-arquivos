package com.conversor.service;

import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

public class PdfMergeService {

    public File juntar(List<File> arquivosPdf, File arquivoSaida) throws ConversaoException {
        if (arquivosPdf == null || arquivosPdf.size() < 2) {
            throw new ConversaoException("Selecione pelo menos 2 arquivos PDF para juntar.");
        }

        for (File pdf : arquivosPdf) {
            validarPdf(pdf);
        }

        PDFMergerUtility merger = new PDFMergerUtility();
        merger.setDestinationFileName(arquivoSaida.getAbsolutePath());

        try {
            for (File pdf : arquivosPdf) {
                merger.addSource(pdf);
            }

            merger.mergeDocuments(null);

            return arquivoSaida;

        } catch (IOException e) {
            throw new ConversaoException("Erro ao juntar os PDFs: " + e.getMessage(), e);
        }
    }

    private void validarPdf(File arquivo) {
        if (!arquivo.getName().toLowerCase().endsWith(".pdf")) {
            throw new ConversaoException("Todos os arquivos selecionados precisam ser PDF. Encontrado: " + arquivo.getName());
        }

        if (!arquivo.exists() || arquivo.length() == 0) {
            throw new ConversaoException("O arquivo está vazio ou não foi encontrado: " + arquivo.getName());
        }

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
}