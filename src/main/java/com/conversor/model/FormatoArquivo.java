package com.conversor.model;

public enum FormatoArquivo {

    PDF("pdf", "application/pdf", CategoriaArquivo.PDF),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", CategoriaArquivo.TEXTO),
    DOC("doc", "application/msword", CategoriaArquivo.TEXTO),
    ODT("odt", "application/vnd.oasis.opendocument.text", CategoriaArquivo.TEXTO),

    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", CategoriaArquivo.PLANILHA),
    XLS("xls", "application/vnd.ms-excel", CategoriaArquivo.PLANILHA),
    CSV("csv", "text/csv", CategoriaArquivo.PLANILHA),
    ODS("ods", "application/vnd.oasis.opendocument.spreadsheet", CategoriaArquivo.PLANILHA),

    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", CategoriaArquivo.APRESENTACAO),

    PNG("png", "image/png", CategoriaArquivo.IMAGEM),
    JPG("jpg", "image/jpeg", CategoriaArquivo.IMAGEM),
    WEBP("webp", "image/webp", CategoriaArquivo.IMAGEM),
    BMP("bmp", "image/bmp", CategoriaArquivo.IMAGEM),
    GIF("gif", "image/gif", CategoriaArquivo.IMAGEM);

    private final String extensao;
    private final String mimeType;
    private final CategoriaArquivo categoria;

    FormatoArquivo(String extensao, String mimeType, CategoriaArquivo categoria) {
        this.extensao = extensao;
        this.mimeType = mimeType;
        this.categoria = categoria;
    }

    public String getExtensao() {
        return extensao;
    }

    public String getMimeType() {
        return mimeType;
    }

    public CategoriaArquivo getCategoria() {
        return categoria;
    }

    public boolean isDocumento() {
        return categoria != CategoriaArquivo.IMAGEM;
    }

    public boolean isImagem() {
        return categoria == CategoriaArquivo.IMAGEM;
    }

    public static FormatoArquivo porExtensao(String extensao) {
        if (extensao == null) {
            return null;
        }
        String limpa = extensao.trim().toLowerCase().replace(".", "");
        for (FormatoArquivo formato : values()) {
            if (formato.extensao.equals(limpa)) {
                return formato;
            }
        }
        return null;
    }
}