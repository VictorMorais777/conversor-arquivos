# Conversor de Arquivos

Aplicativo desktop em JavaFX para conversão de arquivos e junção de PDFs, feito para uso interno da empresa — sem necessidade de conta, sem dependência de internet, rodando 100% localmente.

![Java](https://img.shields.io/badge/Java-21-orange)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.2-blue)
![Licença](https://img.shields.io/badge/uso-interno-lightgrey)

## Funcionalidades

- **Conversão de documentos**: PDF, DOCX, DOC, ODT, XLSX, XLS, CSV, ODS, PPTX
- **Conversão de imagens**: PNG, JPG, WEBP, BMP, GIF
- **Junção de PDFs**: combina múltiplos arquivos PDF em um único documento, na ordem selecionada
- **Detecção automática de formato**: identifica o tipo real do arquivo pelo conteúdo (não só pela extensão)
- **Regras de conversão inteligentes**: o app só sugere conversões que fazem sentido (ex.: um PDF não pode virar planilha, mas pode virar DOCX/DOC/ODT)
- **Arrastar e soltar**: selecione arquivos arrastando direto para a janela
- **Escolha de nome e local**: você decide onde salvar o resultado, com o nome que quiser

## Como usar (versão instalada)

1. Baixe e instale o LibreOffice, caso ainda não tenha: [libreoffice.org/download](https://www.libreoffice.org/download/download-libreoffice/)
2. Execute o instalador `ConversorArquivos-1.0.exe`
3. Abra o app pelo atalho criado na Área de Trabalho ou no Menu Iniciar

> **Nota sobre o aviso do Windows:** como este é um aplicativo interno, sem assinatura digital paga, o Windows Defender SmartScreen pode exibir um aviso ao instalar. Clique em **"Mais informações" → "Executar assim mesmo"** para prosseguir — é um comportamento padrão para aplicativos não assinados, não uma detecção de ameaça real.

### Pré-requisito obrigatório: LibreOffice

O app usa o LibreOffice (em modo headless) como motor de conversão para documentos. Ele **não vem embutido no instalador** — precisa estar instalado separadamente na máquina. O caminho de instalação é detectado automaticamente (funciona com instalação em `Program Files`, `Program Files (x86)`, ou qualquer local registrado no PATH do sistema).

## Stack técnica

| Camada | Tecnologia |
|---|---|
| Interface | JavaFX 21 + FXML + CSS |
| Detecção de formato | Apache Tika |
| Conversão de documentos | LibreOffice (headless, via `ProcessBuilder`) |
| Conversão de imagens | Java ImageIO (nativo) |
| Junção de PDFs | Apache PDFBox |
| Build/empacotamento | Maven + `jpackage` |

## Estrutura do projeto

```
src/main/java/com/conversor/
├── App.java                 # Ponto de entrada JavaFX
├── Launcher.java            # Entrada alternativa (necessária para rodar via jar empacotado)
├── controller/
│   └── ConversorController.java
├── model/
│   ├── FormatoArquivo.java      # Enum dos formatos suportados
│   ├── CategoriaArquivo.java    # Categoria de cada formato (texto, planilha, apresentação, PDF, imagem)
│   └── ResultadoConversao.java
└── service/
    ├── ConversorService.java     # Orquestrador: decide qual motor usar e valida a conversão
    ├── FormatoDetector.java      # Detecção via Apache Tika
    ├── LibreOfficeConverter.java # Motor de documentos
    ├── ImagemConverter.java      # Motor de imagens
    └── PdfMergeService.java      # Junção de PDFs

src/main/resources/com/conversor/
├── icone.ico / icone.png    # Ícone do aplicativo
└── view/
    ├── conversor-view.fxml
    └── styles.css
```

## Regras de conversão

O app organiza os formatos em categorias e só permite conversões que fazem sentido:

| Categoria | Formatos | Pode virar |
|---|---|---|
| Texto | DOCX, DOC, ODT | outro formato de texto, ou PDF |
| Planilha | XLSX, XLS, CSV, ODS | outro formato de planilha, ou PDF |
| Apresentação | PPTX | PDF |
| PDF | PDF | apenas DOCX, DOC ou ODT |
| Imagem | PNG, JPG, WEBP, BMP, GIF | outra imagem |

> PDF como origem só converte para formatos de texto porque o LibreOffice precisa saber como interpretar o conteúdo do PDF ao importar (o app sempre trata PDF como texto editável, via filtro `writer_pdf_import`).

## Rodando em modo desenvolvimento

Pré-requisitos: JDK 21, Maven, LibreOffice instalado.

```bash
mvn javafx:run
```

## Gerando o instalador do zero

```bash
# 1. Empacota o código + dependências em um único .jar
mvn clean package

# 2. Gera o instalador .exe (requer WiX Toolset v3 instalado e no PATH)
jpackage --input target --name ConversorArquivos --main-jar conversor-arquivos-1.0-SNAPSHOT.jar --main-class com.conversor.Launcher --type exe --win-shortcut --win-dir-chooser --icon src\main\resources\com\conversor\icone.ico --dest target\instalador
```

O instalador `.exe` gerado embute o runtime Java — quem for instalar não precisa ter Java na máquina, apenas o LibreOffice.

### Sobre o WiX Toolset

O `jpackage` do Java 21 depende do **WiX Toolset v3** (não v4+) para gerar instaladores `.exe`/`.msi` no Windows. Baixe em: [github.com/wixtoolset/wix3/releases](https://github.com/wixtoolset/wix3/releases)

## Limitações conhecidas

- Requer LibreOffice instalado separadamente em cada máquina
- Testado apenas no Windows (a busca de caminho do LibreOffice também cobre Linux/Mac, mas sem testes completos nesses sistemas)
- PDFs escaneados (imagem, sem texto real) não geram bons resultados na conversão para DOCX, já que o conteúdo não é texto editável
- Instalador sem assinatura digital — o Windows exibe aviso de segurança padrão na primeira execução
