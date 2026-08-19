package com.conversor.controller;

import com.conversor.model.FormatoArquivo;
import com.conversor.model.ResultadoConversao;
import com.conversor.service.ConversorService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class ConversorController {

    @FXML private Button btnSelecionarArquivo;
    @FXML private Label lblArquivoSelecionado;
    @FXML private ComboBox<FormatoArquivo> comboFormatoDestino;
    @FXML private Button btnConverter;
    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label lblStatus;

    private final ConversorService conversorService = new ConversorService();

    private File arquivoSelecionado;

    private final File pastaDestino = new File(System.getProperty("user.home"), "ConversorCNAGA");

    @FXML
    public void initialize() {
        comboFormatoDestino.setOnAction(e -> atualizarBotaoConverter());
    }

    @FXML
    private void onSelecionarArquivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar arquivo para converter");

        File arquivo = fileChooser.showOpenDialog(obterStage());

        if (arquivo == null) {
            return; // usuário cancelou a seleção
        }

        arquivoSelecionado = arquivo;
        lblArquivoSelecionado.setText(arquivo.getName());
        lblStatus.setText("");

        popularFormatosDestino();
    }

    private void popularFormatosDestino() {
        try {
            FormatoArquivo[] formatosDisponiveis = conversorService.formatosDestinoDisponiveis(arquivoSelecionado);

            comboFormatoDestino.getItems().setAll(formatosDisponiveis);
            comboFormatoDestino.setDisable(false);
            comboFormatoDestino.getSelectionModel().clearSelection();

        } catch (Exception e) {
            lblStatus.setStyle("-fx-text-fill: red;");
            lblStatus.setText("Não foi possível identificar o tipo do arquivo: " + e.getMessage());
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

        setCarregando(true);

        Task<ResultadoConversao> tarefaConversao = new Task<>() {
            @Override
            protected ResultadoConversao call() {
                return conversorService.converter(arquivoSelecionado, formatoDestino, pastaDestino);
            }
        };

        tarefaConversao.setOnSucceeded(e -> {
            setCarregando(false);
            exibirResultado(tarefaConversao.getValue());
        });

        tarefaConversao.setOnFailed(e -> {
            setCarregando(false);
            lblStatus.setStyle("-fx-text-fill: red;");
            lblStatus.setText("Erro inesperado: " + tarefaConversao.getException().getMessage());
        });

        Thread thread = new Thread(tarefaConversao);
        thread.setDaemon(true);
        thread.start();
    }

    private void exibirResultado(ResultadoConversao resultado) {
        if (resultado.isSucesso()) {
            lblStatus.setStyle("-fx-text-fill: green;");
            lblStatus.setText("Convertido com sucesso! Salvo em: " + resultado.getArquivoConvertido().getAbsolutePath());
        } else {
            lblStatus.setStyle("-fx-text-fill: red;");
            lblStatus.setText("Erro: " + resultado.getMensagemErro());
        }
    }

    private void setCarregando(boolean carregando) {
        Platform.runLater(() -> {
            progressIndicator.setVisible(carregando);
            btnConverter.setDisable(carregando);
            btnSelecionarArquivo.setDisable(carregando);
        });
    }

    private Stage obterStage() {
        return (Stage) btnSelecionarArquivo.getScene().getWindow();
    }
}