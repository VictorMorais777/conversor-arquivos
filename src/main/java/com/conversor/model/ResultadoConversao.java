package com.conversor.model;

import java.io.File;

/**
 * Representa o resultado de uma tentativa de conversão: sucesso (com o arquivo gerado)
 * ou erro (com uma mensagem clara para exibir ao usuário na UI).
 */
public class ResultadoConversao {

    private final boolean sucesso;
    private final File arquivoConvertido;
    private final FormatoArquivo formatoOrigem;
    private final FormatoArquivo formatoDestino;
    private final String mensagemErro;

    private ResultadoConversao(boolean sucesso, File arquivoConvertido, FormatoArquivo formatoOrigem,
                               FormatoArquivo formatoDestino, String mensagemErro) {
        this.sucesso = sucesso;
        this.arquivoConvertido = arquivoConvertido;
        this.formatoOrigem = formatoOrigem;
        this.formatoDestino = formatoDestino;
        this.mensagemErro = mensagemErro;
    }

    public static ResultadoConversao sucesso(File arquivoConvertido, FormatoArquivo formatoOrigem, FormatoArquivo formatoDestino) {
        return new ResultadoConversao(true, arquivoConvertido, formatoOrigem, formatoDestino, null);
    }

    public static ResultadoConversao erro(String mensagemErro) {
        return new ResultadoConversao(false, null, null, null, mensagemErro);
    }

    public boolean isSucesso() {
        return sucesso;
    }

    public File getArquivoConvertido() {
        return arquivoConvertido;
    }

    public FormatoArquivo getFormatoOrigem() {
        return formatoOrigem;
    }

    public FormatoArquivo getFormatoDestino() {
        return formatoDestino;
    }

    public String getMensagemErro() {
        return mensagemErro;
    }
}