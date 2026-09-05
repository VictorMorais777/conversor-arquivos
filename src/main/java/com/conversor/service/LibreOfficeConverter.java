package com.conversor.service;

import com.conversor.model.FormatoArquivo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

public class LibreOfficeConverter {

    private static final long TIMEOUT_SEGUNDOS = 180;

    private static final String[] CAMINHOS_PADRAO_WINDOWS = {
            "C:\\Program Files\\LibreOffice\\program\\soffice.exe",
            "C:\\Program Files (x86)\\LibreOffice\\program\\soffice.exe"
    };

    private static final String[] CAMINHOS_PADRAO_LINUX = {
            "/usr/bin/soffice",
            "/opt/libreoffice/program/soffice"
    };

    private static final String[] CAMINHOS_PADRAO_MAC = {
            "/Applications/LibreOffice.app/Contents/MacOS/soffice"
    };

    private static String caminhoSofficeEncontrado;
    private static File perfilIsoladoDaSessao;

    public File converter(File arquivoOrigem, FormatoArquivo formatoDestino, File pastaDestino) throws ConversaoException {
        String caminhoSoffice = localizarSoffice();

        if (!pastaDestino.exists()) {
            pastaDestino.mkdirs();
        }

        File pastaLibreOffice = new File(caminhoSoffice).getParentFile();
        File perfilIsolado = obterPerfilIsoladoDaSessao();

        removerLockAntigo(perfilIsolado);

        ProcessBuilder builder = montarComando(caminhoSoffice, arquivoOrigem, formatoDestino, pastaDestino, perfilIsolado);
        builder.directory(pastaLibreOffice);
        builder.redirectErrorStream(true);

        String pastaPythonCore = localizarPastaPythonCore(pastaLibreOffice);
        if (pastaPythonCore != null) {
            builder.environment().put("PYTHONHOME", pastaPythonCore);
        }

        try {
            Process processo = builder.start();

            String saida = new String(processo.getInputStream().readAllBytes());

            boolean finalizou = processo.waitFor(TIMEOUT_SEGUNDOS, TimeUnit.SECONDS);

            if (!finalizou) {
                matarProcessoETodosOsFilhos(processo);
                removerLockAntigo(perfilIsolado);
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

    private synchronized File obterPerfilIsoladoDaSessao() throws ConversaoException {
        if (perfilIsoladoDaSessao != null) {
            return perfilIsoladoDaSessao;
        }

        try {
            perfilIsoladoDaSessao = Files.createTempDirectory("conversor-lo-perfil-").toFile();
        } catch (IOException e) {
            throw new ConversaoException("Não foi possível criar perfil temporário do LibreOffice: " + e.getMessage(), e);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> apagarPastaTemporaria(perfilIsoladoDaSessao)));

        return perfilIsoladoDaSessao;
    }

    private void removerLockAntigo(File perfil) {
        File[] locks = {
                new File(perfil, ".lock"),
                new File(perfil, "lock"),
                new File(perfil, "user/.lock")
        };
        for (File lock : locks) {
            if (lock.exists()) {
                lock.delete();
            }
        }
    }

    private void apagarPastaTemporaria(File pasta) {
        if (pasta == null || !pasta.exists()) {
            return;
        }
        try {
            try (var arquivos = Files.walk(pasta.toPath())) {
                arquivos.sorted(java.util.Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            }
        } catch (IOException ignorado) {
        }
    }

    private void matarProcessoETodosOsFilhos(Process processo) {
        processo.descendants().forEach(ProcessHandle::destroyForcibly);
        processo.destroyForcibly();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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

    private ProcessBuilder montarComando(String caminhoSoffice, File arquivoOrigem, FormatoArquivo formatoDestino, File pastaDestino, File perfilIsolado) {
        boolean origemEhPdf = arquivoOrigem.getName().toLowerCase().endsWith(".pdf");

        String filtroExportacao = FILTROS_EXPORTACAO.get(formatoDestino);
        String argumentoConvertTo = filtroExportacao != null
                ? formatoDestino.getExtensao() + ":" + filtroExportacao
                : formatoDestino.getExtensao();

        String caminhoPerfil = perfilIsolado.getAbsolutePath().replace("\\", "/");

        java.util.List<String> argumentos = new java.util.ArrayList<>();
        argumentos.add(caminhoSoffice);
        argumentos.add("--headless");
        argumentos.add("--norestore");
        argumentos.add("--nolockcheck");
        argumentos.add("-env:UserInstallation=file:///" + caminhoPerfil);

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

    private String localizarSoffice() throws ConversaoException {
        if (caminhoSofficeEncontrado != null) {
            return caminhoSofficeEncontrado;
        }

        for (String candidato : caminhosPadraoDoSistemaOperacional()) {
            if (new File(candidato).exists()) {
                caminhoSofficeEncontrado = candidato;
                return caminhoSofficeEncontrado;
            }
        }

        String caminhoViaPath = buscarNoPath();
        if (caminhoViaPath != null) {
            caminhoSofficeEncontrado = caminhoViaPath;
            return caminhoSofficeEncontrado;
        }

        throw new ConversaoException(
                "LibreOffice não foi encontrado nesta máquina. "
                        + "Instale o LibreOffice (libreoffice.org/download) para converter documentos "
                        + "como PDF, Word, Excel e PowerPoint."
        );
    }

    private String[] caminhosPadraoDoSistemaOperacional() {
        String sistemaOperacional = System.getProperty("os.name", "").toLowerCase();

        if (sistemaOperacional.contains("win")) {
            return CAMINHOS_PADRAO_WINDOWS;
        }
        if (sistemaOperacional.contains("mac")) {
            return CAMINHOS_PADRAO_MAC;
        }
        return CAMINHOS_PADRAO_LINUX;
    }

    private String buscarNoPath() {
        String path = System.getenv("PATH");
        if (path == null) {
            return null;
        }

        boolean windows = System.getProperty("os.name", "").toLowerCase().contains("win");
        String nomeExecutavel = windows ? "soffice.exe" : "soffice";

        for (String pasta : path.split(File.pathSeparator)) {
            File candidato = new File(pasta, nomeExecutavel);
            if (candidato.exists()) {
                return candidato.getAbsolutePath();
            }
        }

        return null;
    }
}