package com.conversor.service;


public class FormatoNaoSuportadoException extends RuntimeException {

    public FormatoNaoSuportadoException(String mensagem) {
        super(mensagem);
    }

    public FormatoNaoSuportadoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}