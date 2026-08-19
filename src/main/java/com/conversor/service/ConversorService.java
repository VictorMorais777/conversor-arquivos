package com.conversor.service;

import com.conversor.model.FormatoArquivo;
import com.conversor.model.ResultadoConversao;

import java.io.File;

public class ConversorService {

    private final FormatoDetector detector;
    private final LibreOfficeConverter libreOfficeConverter;
    private final ImagemConverter imagemConverter;

    public ConversorService() {
        this.detector = new FormatoDetector();
        this.libreOfficeConverter = new LibreOfficeConverter();
        this.imagemConverter = new ImagemConverter();
    }

    public ResultadoConversao converter(File arquivoOrigem, FormatoArquivo formatoDestino, File pastaDestino) {
        try {
            FormatoArquivo formatoOrigem = detector.detectar(arquivoOrigem);

            validarConversao(formatoOrigem, formatoDestino);

            File arquivoConvertido;

            if (formatoOrigem.isDocumento()) {
                arquivoConvertido = libreOfficeConverter.converter(arquivoOrigem, formatoDestino, pastaDestino);
            } else {
                arquivoConvertido = imagemConverter.converter(arquivoOrigem, formatoDestino, pastaDestino);
            }

            return ResultadoConversao.sucesso(arquivoConvertido, formatoOrigem, formatoDestino);

        } catch (FormatoNaoSuportadoException | ConversaoException e) {
            return ResultadoConversao.erro(e.getMessage());
        } catch (Exception e) {
            return ResultadoConversao.erro("Erro inesperado ao converter o arquivo: " + e.getMessage());
        }
    }

    public FormatoArquivo[] formatosDestinoDisponiveis(File arquivoOrigem) throws Exception {
        FormatoArquivo formatoOrigem = detector.detectar(arquivoOrigem);

        return java.util.Arrays.stream(FormatoArquivo.values())
                .filter(formato -> formato != formatoOrigem)
                .filter(formato -> formatoOrigem.isDocumento() ? formato.isDocumento() : formato.isImagem())
                .toArray(FormatoArquivo[]::new);
    }

    private void validarConversao(FormatoArquivo origem, FormatoArquivo destino) {
        boolean origemEhDocumento = origem.isDocumento();
        boolean destinoEhDocumento = destino.isDocumento();

        if (origemEhDocumento != destinoEhDocumento) {
            throw new ConversaoException(
                    "Não é possível converter " + origem + " (documento: " + origemEhDocumento + ")"
                            + " para " + destino + " (documento: " + destinoEhDocumento + "). "
                            + "Documentos só podem ser convertidos para outros documentos, e imagens para outras imagens."
            );
        }

        if (origem == destino) {
            throw new ConversaoException("O formato de origem e destino são iguais (" + origem + "). Escolha um formato diferente.");
        }
    }
}