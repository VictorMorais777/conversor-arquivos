package com.conversor.model;

/**
 * Representa os formatos de arquivo suportados pelo conversor.
 * Cada formato guarda sua extensão (usada para salvar o arquivo de saída)
 * e o mime type esperado (usado pelo FormatoDetector para validar o arquivo real).
 */
public enum FormatoArquivo {

    PDF("pdf", "application/pdf"),
    DOCX("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
    DOC("doc", "application/msword"),
    ODT("odt", "application/vnd.oasis.opendocument.text"),
    XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
    XLS("xls", "application/vnd.ms-excel"),
    CSV("csv", "text/csv"),
    PPTX("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
    ODS("ods", "application/vnd.oasis.opendocument.spreadsheet"),

    PNG("png", "image/png"),
    JPG("jpg", "image/jpeg"),
    WEBP("webp", "image/webp"),
    BMP("bmp", "image/bmp"),
    GIF("gif", "image/gif");

    private final String extensao;
    private final String mimeType;

    FormatoArquivo(String extensao, String mimeType) {
        this.extensao = extensao;
        this.mimeType = mimeType;
    }

    public String getExtensao() {
        return extensao;
    }

    public String getMimeType() {
        return mimeType;
    }

    /**
     * Indica se o formato pertence ao grupo de documentos (convertidos via LibreOffice).
     */
    public boolean isDocumento() {
        return this == PDF || this == DOCX || this == DOC || this == ODT
                || this == XLSX || this == XLS || this == CSV
                || this == PPTX || this == ODS;
    }

    /**
     * Indica se o formato pertence ao grupo de imagens (convertidas via ImageIO).
     */
    public boolean isImagem() {
        return this == PNG || this == JPG || this == WEBP || this == BMP || this == GIF;
    }

    /**
     * Busca o enum a partir da extensão do arquivo (ex: "pdf", "docx"), ignorando maiúsculas/minúsculas.
     * Retorna null se a extensão não for suportada.
     */
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