# 🕷️ Crawler SBGames & SVR 2026 - Web Scraper Java

![Java](https://img.shields.io/badge/Java-17-orange.svg)
![Maven](https://img.shields.io/badge/Maven-3.x-blue.svg)
![Jsoup](https://img.shields.io/badge/Jsoup-1.17.2-green.svg)
![Gson](https://img.shields.io/badge/Gson-2.10.1-red.svg)
![License](https://img.shields.io/badge/License-MIT-brightgreen.svg)

Web crawler em **Java 17** desenvolvido para realizar web scraping e coleta automatizada da programação oficial do **SBGames / SVR 2026**, integrando dados carregados via **Whova API** com a extração profunda de artigos e autores no repositório **SOL SBC** (`sol.sbc.org.br`).

---

## 🎯 Objetivo do Projeto

Coletar, filtrar e estruturar automaticamente todas as sessões e artigos acadêmicos das trilhas do **SBGames 2026** (Educação, Computação, Artes & Design, Cultura, Indústria e Saúde) para os dias **01/10/2026** e **02/10/2026**, exportando os dados em arquivos **JSON** padronizados para alimentar portfólios, bancos de dados ou sistemas de análise.

---

## ✨ Diferenciais Técnicos & Funcionalidades

- **Reengenharia de API (Whova REST):** Em vez de utilizar automações pesadas de navegador (Selenium / Playwright), o crawler intercepta e consome diretamente os endpoints REST públicos da infraestrutura Whova, reduzindo o tempo de execução para poucos segundos.
- **Processamento Concorrente Multithread:** Utilização de `ExecutorService` e `CompletableFuture` para realizar requisições HTTP paralelas de alta performance.
- **Web Scraping com Jsoup & SSL Bypass:** Extração dos dados dos autores diretamente das páginas individuais de artigos no **SOL SBC**, com tratamento customizado para certificar a conexão HTTPS resiliente.
- **Fidelidade e Integridade dos Dados:** Preservação estrita do formato original dos autores, instituições, acentuação, pontuação e ordem de autoria, sem normalizações destrutivas.
- **Exportação Multi-dias:** Geração automatizada dos arquivos `resultad01.json` (01/10/2026) e `resultado.json` (02/10/2026).

---

## 🛠️ Tecnologias Utilizadas

- **Linguagem:** Java 17
- **Gerenciador de Dependências:** Apache Maven
- **HTML Parsing & Web Scraping:** [Jsoup 1.17.2](https://jsoup.org/)
- **Manipulação de JSON:** [Google Gson 2.10.1](https://github.com/google/gson)
- **Concorrência:** Java `CompletableFuture` & `ExecutorService`

---

## 📁 Estrutura do Projeto

```text
robo/
├── pom.xml                     # Configurações de compilação e dependências Maven
├── resultad01.json             # Dataset gerado para o dia 01/10/2026
├── resultado.json              # Dataset gerado para o dia 02/10/2026
└── src/
    └── main/
        └── java/
            └── com/
                └── robosvrsbc/
                    └── Main.java   # Código-fonte principal do Crawler
```

---

## 📊 Estrutura dos Dados Exportados (JSON)

Cada arquivo JSON produzido possui o seguinte formato conceitual:

```json
[
  {
    "data": "02/10/2026",
    "trilha": "Educação",
    "codigo": "ST09",
    "titulo": "Ambientes gamificados",
    "horarioInicio": "10:30",
    "horarioFim": "12:00",
    "sala": "Sala A",
    "evento": "SBGAMES",
    "chair": "Maurilio Martins Campano Junior (UEM)",
    "quantidadePapers": 6,
    "quantidadeSubsessions": 6,
    "papers": [
      {
        "titulo": "Serious Games contra a desinformação: aplicando o método prebunking de inoculação no contexto brasileiro",
        "link": "https://sol.sbc.org.br/index.php/sbgames/article/view/45476",
        "horarioInicio": "10:30",
        "horarioFim": "12:00",
        "autores": "Karoline Maria Fernandes da Costa e Silva (UFPE); Nadi Helena Presser (UFPE); Luciana Monteiro-Krebs (UFRGS); Rodrigo F. R. Carmo (UFRPE)"
      }
    ]
  }
]
```

---

## 🚀 Como Executar o Projeto

### Pré-requisitos
- **Java JDK 17** ou superior instalado e configurado nas variáveis de ambiente.
- **Apache Maven 3.x** instalado.

### Passos

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/seu-usuario/seu-repositorio.git
   cd robo
   ```

2. **Compile e execute o programa:**
   ```bash
   mvn clean compile exec:java "-Dexec.mainClass=com.robosvrsbc.Main"
   ```

3. **Verifique os resultados:**
   Após a execução, os dois arquivos JSON estarão disponíveis na raiz do projeto:
   - `resultad01.json` (programação do dia 01/10/2026)
   - `resultado.json` (programação do dia 02/10/2026)

---

## 📜 Licença

Este projeto é disponibilizado sob a licença [MIT](LICENSE).
