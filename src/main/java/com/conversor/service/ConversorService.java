package com.conversor.service;

import com.conversor.model.FormatoArquivo;
import com.conversor.model.ResultadoConversao;

import java.io.File;

/**
 * Ponto único de entrada para conversão de arquivos.
 * Detecta o formato de origem, valida se a conversão faz sentido,
 * e decide qual motor usar (LibreOffice para documentos, ImageIO para imagens).
 * É essa classe que a UI (controller) deve chamar — ela não precisa saber
 * como cada conversão é feita por baixo dos panos.
 */
public class ConversorService {

    private final FormatoDetector detector;
    private final LibreOfficeConverter libreOfficeConverter;
    private final ImagemConverter imagemConverter;

    public ConversorService() {
        this.detector = new FormatoDetector();
        this.libreOfficeConverter = new LibreOfficeConverter();
        this.imagemConverter = new ImagemConverter();
    }

    /**
     * Converte o arquivo de origem para o formato de destino desejado.
     * Detecta automaticamente o formato real de origem antes de converter.
     *
     * @param arquivoOrigem  arquivo selecionado pelo usuário
     * @param formatoDestino formato desejado de saída
     * @param pastaDestino   pasta onde o arquivo convertido será salvo
     * @return ResultadoConversao com sucesso/erro e o arquivo gerado (se sucesso)
     */
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

    /**
     * Retorna a lista de formatos de destino válidos para um determinado arquivo de origem.
     * Usada para popular o dropdown da UI dinamicamente: documentos só podem virar
     * outros documentos, imagens só podem virar outras imagens.
     */
    public FormatoArquivo[] formatosDestinoDisponiveis(File arquivoOrigem) throws Exception {
        FormatoArquivo formatoOrigem = detector.detectar(arquivoOrigem);

        return java.util.Arrays.stream(FormatoArquivo.values())
                .filter(formato -> formato != formatoOrigem)
                .filter(formato -> formatoOrigem.isDocumento() ? formato.isDocumento() : formato.isImagem())
                .toArray(FormatoArquivo[]::new);
    }

    /**
     * Garante que a conversão faz sentido: documento não pode virar imagem e vice-versa.
     * Essa regra existe porque LibreOffice e ImageIO não têm como converter entre esses grupos.
     */
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