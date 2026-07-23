# Curl Runner

Aplicação desktop para executar comandos cURL de forma repetida e controlada, similar ao Collection Runner do Postman e Bruno.

## Para que serve?

O Curl Runner permite testar endpoints HTTP executando o mesmo comando cURL múltiplas vezes, com controle sobre:

- **Número de iterações** — quantas vezes a requisição será executada (1 a 10.000)
- **Delay entre requisições** — intervalo em milissegundos entre cada execução (0 a 60.000ms)
- **Modo paralelo ou sequencial** — execute uma por vez ou todas simultaneamente

Ao final, a aplicação exibe um resumo completo com métricas de performance: tempo médio de resposta, percentis (P50, P95, P99), taxa de requisições por segundo, distribuição de status codes e contagem de sucessos/falhas.

## Como usar

1. **Cole seu comando cURL** no campo de texto da aplicação
2. **Configure a execução**:
   - Defina o número de iterações desejado
   - Defina o delay entre requisições (0 para sem pausa)
   - Marque "Paralelo" se quiser execução simultânea
3. **Clique em "Executar"**
4. **Acompanhe o progresso** pela barra de progresso
5. **Analise os resultados** no painel de resumo e na tabela de resultados individuais

Para cancelar uma execução em andamento, clique em "Cancelar".

## Exemplo de comando cURL

```
curl --request POST --url https://api.exemplo.com/dados --header 'Content-Type: application/json' --data '{"chave": "valor"}'
```

Comandos copiados de navegadores ou ferramentas com `\` de continuação de linha também são aceitos.

## Formatos suportados

- `-X` / `--request` — método HTTP (GET, POST, PUT, DELETE, PATCH)
- `--url` — URL alvo
- `-H` / `--header` — headers da requisição
- `-d` / `--data` / `--data-raw` — corpo da requisição
- `-u` / `--user` — autenticação básica (usuario:senha)
- URLs sem flag (passadas diretamente após `curl`)

## Executando a aplicação

Requisitos: Java 17 ou superior.

```bash
mvn spring-boot:run
```

Ou gere o JAR e execute:

```bash
mvn clean package -DskipTests
java -jar target/curl-runner-0.0.1-SNAPSHOT.jar
```

## Observações

- Os dados de execução ficam apenas em memória durante a sessão. Ao fechar a aplicação, o histórico é perdido.
- Comandos com caracteres de injeção de shell (pipes, redirecionamentos, backticks) são rejeitados por segurança.
- Em modo paralelo, o número de threads é limitado ao número de processadores disponíveis na máquina.
