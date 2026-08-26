package com.conversor.service;

import org.apache.pdfbox.multipdf.PDFMergerUtility;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class PdfMergeService {

    public File juntar(List<File> arquivosPdf, File arquivoSaida) throws ConversaoException {
        if (arquivosPdf == null || arquivosPdf.size() < 2) {
            throw new ConversaoException("Selecione pelo menos 2 arquivos PDF para juntar.");
        }

        validarTodosSaoPdf(arquivosPdf);

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

    private void validarTodosSaoPdf(List<File> arquivos) {
        for (File arquivo : arquivos) {
            if (!arquivo.getName().toLowerCase().endsWith(".pdf")) {
                throw new ConversaoException(
                        "Todos os arquivos selecionados precisam ser PDF. Encontrado: " + arquivo.getName()
                );
            }
        }
    }
}