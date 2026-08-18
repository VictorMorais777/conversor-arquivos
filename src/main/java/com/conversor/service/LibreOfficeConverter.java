package com.conversor.service;

import com.conversor.model.FormatoArquivo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Converte arquivos de documento (pdf, docx, odt, xlsx, csv, pptx, etc.)
 * chamando o LibreOffice em modo headless como processo externo.
 * Requer o LibreOffice instalado na máquina.
 */
public class LibreOfficeConverter {

    /**
     * Caminho do executável do LibreOffice. Ajuste conforme o sistema operacional
     * onde a aplicação vai rodar.
     * Windows costuma ser: C:\Program Files\LibreOffice\program\soffice.exe
     * Linux costuma ser: /usr/bin/soffice
     * Mac costuma ser: /Applications/LibreOffice.app/Contents/MacOS/soffice
     */
    private static final String CAMINHO_SOFFICE = "C:\\Program Files\\LibreOffice\\program\\soffice.exe";

    private static final long TIMEOUT_SEGUNDOS = 120;

    /**
     * Converte o arquivo de origem para o formato de destino usando LibreOffice headless.
     *
     * @param arquivoOrigem  arquivo a ser convertido
     * @param formatoDestino formato desejado de saída
     * @param pastaDestino   pasta onde o arquivo convertido será salvo
     * @return o arquivo convertido
     */
    public File converter(File arquivoOrigem, FormatoArquivo formatoDestino, File pastaDestino) throws ConversaoException {
        validarSoffice();

        if (!pastaDestino.exists()) {
            pastaDestino.mkdirs();
        }

        // Executa o soffice.exe a partir da própria pasta onde ele está instalado
        // (equivalente a dar "cd" até lá antes de rodar o comando). Isso resolve
        // problemas de localização de bibliotecas internas do LibreOffice que
        // podem ocorrer quando o processo é iniciado de um diretório diferente.
        File pastaLibreOffice = new File(CAMINHO_SOFFICE).getParentFile();

        ProcessBuilder builder = new ProcessBuilder(
                CAMINHO_SOFFICE,
                "--headless",
                "--convert-to", formatoDestino.getExtensao(),
                "--outdir", pastaDestino.getAbsolutePath(),
                arquivoOrigem.getAbsolutePath()
        );
        builder.directory(pastaLibreOffice);
        builder.redirectErrorStream(true);

        // DEBUG: mostra se essas variáveis estavam presentes antes de remover,
        // para confirmar se essa era mesmo a causa do problema.
        System.out.println("DEBUG - PYTHONHOME antes de remover: " + builder.environment().get("PYTHONHOME"));
        System.out.println("DEBUG - PYTHONPATH antes de remover: " + builder.environment().get("PYTHONPATH"));

        // Remove variáveis de ambiente Python que podem ter sido herdadas do processo
        // pai (IntelliJ/Maven) e que conflitam com o Python interno do LibreOffice,
        // causando o erro "Could not find platform independent libraries <prefix>".
        builder.environment().remove("PYTHONHOME");
        builder.environment().remove("PYTHONPATH");

        // LOG DE DEBUG: imprime o comando exato sendo executado, para comparar
        // com o comando testado manualmente no terminal. Remover depois que
        // o problema de conversão for resolvido.
        System.out.println("DEBUG - Comando: " + String.join(" ", builder.command()));
        System.out.println("DEBUG - Diretório de trabalho: " + builder.directory());

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

    /**
     * O LibreOffice salva o arquivo com o mesmo nome base do original, trocando só a extensão.
     * Esse método monta o caminho esperado e confirma que o arquivo realmente foi criado.
     */
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

    /**
     * Confirma que o executável do LibreOffice existe antes de tentar rodar,
     * pra dar uma mensagem de erro clara em vez de falha genérica de processo.
     */
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