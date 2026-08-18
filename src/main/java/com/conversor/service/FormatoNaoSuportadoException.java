package com.conversor.service;

/**
 * Lançada quando o arquivo enviado não corresponde a nenhum formato suportado pelo conversor.
 */
public class FormatoNaoSuportadoException extends RuntimeException {

    public FormatoNaoSuportadoException(String mensagem) {
        super(mensagem);
    }

    public FormatoNaoSuportadoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}