package com.conversor.service;

import com.conversor.model.CategoriaArquivo;
import com.conversor.model.FormatoArquivo;
import com.conversor.model.ResultadoConversao;

import java.io.File;
import java.util.Arrays;

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

        return Arrays.stream(FormatoArquivo.values())
                .filter(formato -> formato != formatoOrigem)
                .filter(formato -> conversaoFazSentido(formatoOrigem, formato))
                .toArray(FormatoArquivo[]::new);
    }

    private void validarConversao(FormatoArquivo origem, FormatoArquivo destino) {
        if (origem == destino) {
            throw new ConversaoException("O formato de origem e destino são iguais (" + origem + "). Escolha um formato diferente.");
        }

        if (!conversaoFazSentido(origem, destino)) {
            throw new ConversaoException(
                    "Não é possível converter " + origem + " (" + origem.getCategoria() + ")"
                            + " para " + destino + " (" + destino.getCategoria() + "). "
                            + "Formatos de texto, planilha e apresentação só podem ser convertidos "
                            + "dentro do próprio grupo, ou para PDF."
            );
        }
    }

    private boolean conversaoFazSentido(FormatoArquivo origem, FormatoArquivo destino) {
        CategoriaArquivo categoriaOrigem = origem.getCategoria();
        CategoriaArquivo categoriaDestino = destino.getCategoria();

        if (categoriaOrigem == CategoriaArquivo.IMAGEM || categoriaDestino == CategoriaArquivo.IMAGEM) {
            return categoriaOrigem == CategoriaArquivo.IMAGEM && categoriaDestino == CategoriaArquivo.IMAGEM;
        }

        if (categoriaOrigem == CategoriaArquivo.PDF) {
            return categoriaDestino == CategoriaArquivo.TEXTO;
        }

        if (categoriaDestino == CategoriaArquivo.PDF) {
            return true;
        }

        return categoriaOrigem == categoriaDestino;
    }
}