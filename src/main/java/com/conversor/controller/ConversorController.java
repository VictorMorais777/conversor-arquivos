package com.conversor.controller;

import com.conversor.model.FormatoArquivo;
import com.conversor.model.ResultadoConversao;
import com.conversor.service.ConversaoException;
import com.conversor.service.ConversorService;
import com.conversor.service.PdfMergeService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.List;

public class ConversorController {

    @FXML private VBox dropZone;
    @FXML private Label lblArquivoSelecionado;
    @FXML private ComboBox<FormatoArquivo> comboFormatoDestino;
    @FXML private Button btnConverter;
    @FXML private Button btnJuntarPdfs;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblStatus;

    private final ConversorService conversorService = new ConversorService();
    private final PdfMergeService pdfMergeService = new PdfMergeService();

    private File arquivoSelecionado;

    @FXML
    public void initialize() {
        comboFormatoDestino.setOnAction(e -> atualizarBotaoConverter());
        configurarDragAndDrop();
    }

    private void configurarDragAndDrop() {
        dropZone.setOnDragOver(this::onDragOver);
        dropZone.setOnDragEntered(e -> dropZone.getStyleClass().add("drop-zone-ativa"));
        dropZone.setOnDragExited(e -> dropZone.getStyleClass().remove("drop-zone-ativa"));
        dropZone.setOnDragDropped(this::onDragDropped);
    }

    private void onDragOver(DragEvent evento) {
        if (evento.getDragboard().hasFiles()) {
            evento.acceptTransferModes(TransferMode.COPY);
        }
        evento.consume();
    }

    private void onDragDropped(DragEvent evento) {
        Dragboard dragboard = evento.getDragboard();
        boolean sucesso = false;

        if (dragboard.hasFiles()) {
            List<File> arquivos = dragboard.getFiles();
            if (!arquivos.isEmpty()) {
                selecionarArquivo(arquivos.get(0));
                sucesso = true;
            }
        }

        evento.setDropCompleted(sucesso);
        evento.consume();
    }

    @FXML
    private void onSelecionarArquivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar arquivo para converter");

        File arquivo = fileChooser.showOpenDialog(obterStage());

        if (arquivo == null) {
            return;
        }

        selecionarArquivo(arquivo);
    }

    private void selecionarArquivo(File arquivo) {
        arquivoSelecionado = arquivo;
        lblArquivoSelecionado.setText(arquivo.getName());
        limparStatus();

        popularFormatosDestino();
    }

    private void popularFormatosDestino() {
        try {
            FormatoArquivo[] formatosDisponiveis = conversorService.formatosDestinoDisponiveis(arquivoSelecionado);

            comboFormatoDestino.getItems().setAll(formatosDisponiveis);
            comboFormatoDestino.setDisable(false);
            comboFormatoDestino.getSelectionModel().clearSelection();

        } catch (Exception e) {
            exibirErro("Não foi possível identificar o tipo do arquivo: " + e.getMessage());
            comboFormatoDestino.setDisable(true);
        }

        atualizarBotaoConverter();
    }

    private void atualizarBotaoConverter() {
        boolean pronto = arquivoSelecionado != null && comboFormatoDestino.getValue() != null;
        btnConverter.setDisable(!pronto);
    }

    @FXML
    private void onConverter() {
        FormatoArquivo formatoDestino = comboFormatoDestino.getValue();

        if (arquivoSelecionado == null || formatoDestino == null) {
            return;
        }

        File arquivoDestino = escolherLocalParaSalvar(
                nomeBaseSemExtensao(arquivoSelecionado.getName()) + "." + formatoDestino.getExtensao(),
                formatoDestino.getExtensao()
        );

        if (arquivoDestino == null) {
            return;
        }

        File pastaDestino = arquivoDestino.getParentFile();

        setCarregando(true);

        Task<ResultadoConversao> tarefaConversao = new Task<>() {
            @Override
            protected ResultadoConversao call() {
                return conversorService.converter(arquivoSelecionado, formatoDestino, pastaDestino);
            }
        };

        tarefaConversao.setOnSucceeded(e -> {
            setCarregando(false);
            exibirResultado(tarefaConversao.getValue(), arquivoDestino, formatoDestino);
        });

        tarefaConversao.setOnFailed(e -> {
            setCarregando(false);
            exibirErro("Erro inesperado: " + tarefaConversao.getException().getMessage());
        });

        Thread thread = new Thread(tarefaConversao);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void onJuntarPdfs() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar PDFs para juntar (na ordem desejada)");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Arquivos PDF", "*.pdf")
        );

        List<File> arquivosPdf = fileChooser.showOpenMultipleDialog(obterStage());

        if (arquivosPdf == null || arquivosPdf.size() < 2) {
            if (arquivosPdf != null && arquivosPdf.size() == 1) {
                exibirErro("Selecione pelo menos 2 arquivos PDF para juntar.");
            }
            return;
        }

        File arquivoSaida = escolherLocalParaSalvar("pdfs-unidos.pdf", "pdf");

        if (arquivoSaida == null) {
            return;
        }

        limparStatus();
        setCarregando(true);

        Task<File> tarefaJuntar = new Task<>() {
            @Override
            protected File call() {
                return pdfMergeService.juntar(arquivosPdf, arquivoSaida);
            }
        };

        tarefaJuntar.setOnSucceeded(e -> {
            setCarregando(false);
            lblStatus.getStyleClass().removeAll("status-erro");
            lblStatus.getStyleClass().add("status-sucesso");
            lblStatus.setText(arquivosPdf.size() + " PDFs unidos com sucesso! Salvo em: " + tarefaJuntar.getValue().getAbsolutePath());
        });

        tarefaJuntar.setOnFailed(e -> {
            setCarregando(false);
            Throwable causa = tarefaJuntar.getException();
            String mensagem = (causa instanceof ConversaoException) ? causa.getMessage() : "Erro inesperado ao juntar PDFs: " + causa.getMessage();
            exibirErro(mensagem);
        });

        Thread thread = new Thread(tarefaJuntar);
        thread.setDaemon(true);
        thread.start();
    }

    private File escolherLocalParaSalvar(String nomeSugerido, String extensao) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar como");
        fileChooser.setInitialFileName(nomeSugerido);
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(extensao.toUpperCase() + " (*." + extensao + ")", "*." + extensao)
        );

        return fileChooser.showSaveDialog(obterStage());
    }

    private String nomeBaseSemExtensao(String nomeArquivo) {
        int ponto = nomeArquivo.lastIndexOf('.');
        return ponto == -1 ? nomeArquivo : nomeArquivo.substring(0, ponto);
    }

    private void exibirResultado(ResultadoConversao resultado, File arquivoDestino, FormatoArquivo formatoDestino) {
        if (resultado.isSucesso()) {
            File arquivoGerado = resultado.getArquivoConvertido();

            if (!arquivoGerado.getName().equals(arquivoDestino.getName())) {
                boolean renomeado = arquivoGerado.renameTo(arquivoDestino);
                if (renomeado) {
                    arquivoGerado = arquivoDestino;
                }
            }

            lblStatus.getStyleClass().removeAll("status-erro");
            lblStatus.getStyleClass().add("status-sucesso");
            lblStatus.setText("Convertido com sucesso! Salvo em: " + arquivoGerado.getAbsolutePath());
        } else {
            exibirErro("Erro: " + resultado.getMensagemErro());
        }
    }

    private void exibirErro(String mensagem) {
        lblStatus.getStyleClass().removeAll("status-sucesso");
        lblStatus.getStyleClass().add("status-erro");
        lblStatus.setText(mensagem);
    }

    private void limparStatus() {
        lblStatus.setText("");
        lblStatus.getStyleClass().removeAll("status-sucesso", "status-erro");
    }

    private void setCarregando(boolean carregando) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(carregando);
            btnConverter.setDisable(carregando);
            btnJuntarPdfs.setDisable(carregando);
            dropZone.setDisable(carregando);
        });
    }

    private Stage obterStage() {
        return (Stage) lblArquivoSelecionado.getScene().getWindow();
    }
}