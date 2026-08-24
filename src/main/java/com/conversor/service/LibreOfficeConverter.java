package com.conversor.service;

import com.conversor.model.FormatoArquivo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class LibreOfficeConverter {

    private static final String CAMINHO_SOFFICE = "C:\\Program Files\\LibreOffice\\program\\soffice.exe";

    private static final long TIMEOUT_SEGUNDOS = 120;

    public File converter(File arquivoOrigem, FormatoArquivo formatoDestino, File pastaDestino) throws ConversaoException {
        validarSoffice();

        if (!pastaDestino.exists()) {
            pastaDestino.mkdirs();
        }

        File pastaLibreOffice = new File(CAMINHO_SOFFICE).getParentFile();

        ProcessBuilder builder = montarComando(arquivoOrigem, formatoDestino, pastaDestino);
        builder.directory(pastaLibreOffice);
        builder.redirectErrorStream(true);

        String pastaPythonCore = localizarPastaPythonCore(pastaLibreOffice);
        if (pastaPythonCore != null) {
            builder.environment().put("PYTHONHOME", pastaPythonCore);
        }

        try {
            Process processo = builder.start();

            // Lê a saída do processo para não travar o buffer, mesmo sem precisar do conteúdo
            String saida = new String(processo.getInputStream().readAllBytes());

            boolean finalizou = processo.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);

            if (!finalizou) {
                processo.destroyForcibly();
                throw new ConversaoException("A conversão excedeu o tempo limite de " + TIMEOUT_SEGUNDOS + " segundos.");
            }

            if (processo.exitValue() != 0) {
                throw new ConversaoException("LibreOffice retornou erro na conversão. Saída: " + saida);
            }

            File arquivoConvertido = localizarArquivoGerado(arquivoOrigem, formatoDestino, pastaDestino);

            if (arquivoConvertido == null) {
                throw new ConversaoException("A conversão terminou, mas o arquivo de saída não foi encontrado. Saída do processo: " + saida);
            }

            return arquivoConvertido;

        } catch (IOException e) {
            throw new ConversaoException("Erro ao executar o LibreOffice: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ConversaoException("Conversão interrompida.", e);
        }
    }

    private static final java.util.Map<FormatoArquivo, String> FILTROS_EXPORTACAO = java.util.Map.of(
            FormatoArquivo.DOCX, "MS Word 2007 XML",
            FormatoArquivo.DOC, "MS Word 97",
            FormatoArquivo.ODT, "writer8",
            FormatoArquivo.XLSX, "Calc MS Excel 2007 XML",
            FormatoArquivo.XLS, "MS Excel 97",
            FormatoArquivo.ODS, "calc8",
            FormatoArquivo.PPTX, "Impress MS PowerPoint 2007 XML",
            FormatoArquivo.CSV, "Text - txt - csv (StarCalc)"
    );

    private ProcessBuilder montarComando(File arquivoOrigem, FormatoArquivo formatoDestino, File pastaDestino) {
        boolean origemEhPdf = arquivoOrigem.getName().toLowerCase().endsWith(".pdf");

        String filtroExportacao = FILTROS_EXPORTACAO.get(formatoDestino);
        String argumentoConvertTo = filtroExportacao != null
                ? formatoDestino.getExtensao() + ":" + filtroExportacao
                : formatoDestino.getExtensao();

        java.util.List<String> argumentos = new java.util.ArrayList<>();
        argumentos.add(CAMINHO_SOFFICE);
        argumentos.add("--headless");

        if (origemEhPdf) {
            argumentos.add("--infilter=writer_pdf_import");
        }

        argumentos.add("--convert-to");
        argumentos.add(argumentoConvertTo);
        argumentos.add("--outdir");
        argumentos.add(pastaDestino.getAbsolutePath());
        argumentos.add(arquivoOrigem.getAbsolutePath());

        return new ProcessBuilder(argumentos);
    }

    private String localizarPastaPythonCore(File pastaLibreOffice) {
        File[] candidatos = pastaLibreOffice.listFiles(
                (dir, nome) -> nome.startsWith("python-core-")
        );

        if (candidatos == null || candidatos.length == 0) {
            return null;
        }

        return candidatos[0].getAbsolutePath();
    }

    private File localizarArquivoGerado(File arquivoOrigem, FormatoArquivo formatoDestino, File pastaDestino) {
        String nomeBase = arquivoOrigem.getName();
        int ponto = nomeBase.lastIndexOf('.');
        if (ponto != -1) {
            nomeBase = nomeBase.substring(0, ponto);
        }

        Path caminhoEsperado = pastaDestino.toPath().resolve(nomeBase + "." + formatoDestino.getExtensao());
        File arquivo = caminhoEsperado.toFile();

        return arquivo.exists() ? arquivo : null;
    }

    private void validarSoffice() throws ConversaoException {
        File soffice = new File(CAMINHO_SOFFICE);
        if (!soffice.exists()) {
            throw new ConversaoException(
                    "LibreOffice não encontrado em: " + CAMINHO_SOFFICE
                            + ". Verifique se está instalado ou ajuste o caminho em LibreOfficeConverter.CAMINHO_SOFFICE."
            );
        }
    }
}