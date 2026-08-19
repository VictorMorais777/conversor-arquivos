package com.conversor.service;

public class ConversaoException extends RuntimeException {

    public ConversaoException(String mensagem) {
        super(mensagem);
    }

    public ConversaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}