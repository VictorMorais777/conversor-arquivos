package com.conversor.service;

import com.conversor.model.FormatoArquivo;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class ImagemConverter {

    public File converter(File arquivoOrigem, FormatoArquivo formatoDestino, File pastaDestino) throws ConversaoException {
        if (!pastaDestino.exists()) {
            pastaDestino.mkdirs();
        }

        try {
            BufferedImage imagemOriginal = ImageIO.read(arquivoOrigem);

            if (imagemOriginal == null) {
                throw new ConversaoException("Não foi possível ler a imagem de origem. Arquivo pode estar corrompido: " + arquivoOrigem.getName());
            }

            BufferedImage imagemParaSalvar = imagemOriginal;
            if (formatoDestino == FormatoArquivo.JPG && imagemOriginal.getColorModel().hasAlpha()) {
                imagemParaSalvar = removerTransparencia(imagemOriginal);
            }

            String nomeBase = obterNomeSemExtensao(arquivoOrigem.getName());
            File arquivoSaida = new File(pastaDestino, nomeBase + "." + formatoDestino.getExtensao());

            boolean sucesso = salvarComQualidade(imagemParaSalvar, formatoDestino, arquivoSaida);

            if (!sucesso) {
                throw new ConversaoException("Formato de imagem de destino não suportado pelo ImageIO: " + formatoDestino);
            }

            return arquivoSaida;

        } catch (IOException e) {
            throw new ConversaoException("Erro ao converter imagem: " + e.getMessage(), e);
        }
    }

    private BufferedImage removerTransparencia(BufferedImage original) {
        BufferedImage semAlpha = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        semAlpha.createGraphics().drawImage(original, 0, 0, java.awt.Color.WHITE, null);
        return semAlpha;
    }

    private boolean salvarComQualidade(BufferedImage imagem, FormatoArquivo formato, File arquivoSaida) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(formato.getExtensao());

        if (!writers.hasNext()) {
            return false;
        }

        ImageWriter writer = writers.next();

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(arquivoSaida)) {
            writer.setOutput(ios);

            ImageWriteParam parametros = writer.getDefaultWriteParam();

            if (parametros.canWriteCompressed() && (formato == FormatoArquivo.JPG)) {
                parametros.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                parametros.setCompressionQuality(0.9f); // 90% de qualidade, bom equilíbrio entre nitidez e tamanho
            }

            writer.write(null, new IIOImage(imagem, null, null), parametros);
        } finally {
            writer.dispose();
        }

        return true;
    }

    private String obterNomeSemExtensao(String nomeArquivo) {
        int ponto = nomeArquivo.lastIndexOf('.');
        return ponto == -1 ? nomeArquivo : nomeArquivo.substring(0, ponto);
    }
}