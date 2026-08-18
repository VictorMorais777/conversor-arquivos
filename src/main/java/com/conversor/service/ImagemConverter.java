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

/**
 * Converte arquivos de imagem (png, jpg, webp, bmp, gif) usando a biblioteca
 * ImageIO nativa do Java. Não depende de nenhuma ferramenta externa instalada.
 */
public class ImagemConverter {

    /**
     * Converte a imagem de origem para o formato de destino.
     *
     * @param arquivoOrigem  imagem a ser convertida
     * @param formatoDestino formato desejado de saída
     * @param pastaDestino   pasta onde a imagem convertida será salva
     * @return o arquivo convertido
     */
    public File converter(File arquivoOrigem, FormatoArquivo formatoDestino, File pastaDestino) throws ConversaoException {
        if (!pastaDestino.exists()) {
            pastaDestino.mkdirs();
        }

        try {
            BufferedImage imagemOriginal = ImageIO.read(arquivoOrigem);

            if (imagemOriginal == null) {
                throw new ConversaoException("Não foi possível ler a imagem de origem. Arquivo pode estar corrompido: " + arquivoOrigem.getName());
            }

            // JPG não suporta transparência (canal alpha). Se a imagem original tiver
            // (ex: convertendo de PNG), precisa "achatar" pra um fundo branco antes de salvar,
            // senão o resultado sai com cores erradas ou falha ao salvar.
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

    /**
     * Remove o canal alpha (transparência) preenchendo com fundo branco.
     * Necessário para formatos como JPG que não suportam transparência.
     */
    private BufferedImage removerTransparencia(BufferedImage original) {
        BufferedImage semAlpha = new BufferedImage(
                original.getWidth(),
                original.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        semAlpha.createGraphics().drawImage(original, 0, 0, java.awt.Color.WHITE, null);
        return semAlpha;
    }

    /**
     * Salva a imagem usando o writer nativo do ImageIO para o formato,
     * aplicando uma qualidade razoável para formatos com compressão (ex: JPG).
     */
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