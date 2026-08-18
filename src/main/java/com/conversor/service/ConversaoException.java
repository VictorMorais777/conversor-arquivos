package com.conversor.service;

/**
 * Lançada quando ocorre qualquer erro durante o processo de conversão de arquivo,
 * seja por falha do LibreOffice, timeout, ou arquivo de saída não encontrado.
 */
public class ConversaoException extends RuntimeException {

    public ConversaoException(String mensagem) {
        super(mensagem);
    }

    public ConversaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}