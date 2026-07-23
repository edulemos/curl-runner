package com.curlrunner.parser;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementation of CurlParserService that parses cURL command strings
 * into structured ParsedRequest objects.
 */
@Service
public class CurlParserServiceImpl implements CurlParserService {

    /**
     * Validates the raw cURL command string for shell injection characters.
     * Rejects commands containing pipes, redirections, backticks, command substitution,
     * semicolons, double ampersands, or double pipes.
     *
     * @param curlCommand the raw cURL command string to validate
     * @throws CurlParseException if shell injection characters are detected
     */
    private void validateSecurity(String curlCommand) throws CurlParseException {
        if (curlCommand.contains("||")) {
            throw new CurlParseException(
                "Comando cURL rejeitado: contém operador de pipe duplo '||' que representa risco de injeção de shell");
        }
        if (curlCommand.contains("&&")) {
            throw new CurlParseException(
                "Comando cURL rejeitado: contém operador '&&' que representa risco de injeção de shell");
        }
        if (curlCommand.contains("$(")) {
            throw new CurlParseException(
                "Comando cURL rejeitado: contém substituição de comando '$(' que representa risco de injeção de shell");
        }
        if (curlCommand.contains("|")) {
            throw new CurlParseException(
                "Comando cURL rejeitado: contém caractere de pipe '|' que representa risco de injeção de shell");
        }
        if (curlCommand.contains(">")) {
            throw new CurlParseException(
                "Comando cURL rejeitado: contém redirecionamento de saída '>' que representa risco de injeção de shell");
        }
        if (curlCommand.contains("<")) {
            throw new CurlParseException(
                "Comando cURL rejeitado: contém redirecionamento de entrada '<' que representa risco de injeção de shell");
        }
        if (curlCommand.contains("`")) {
            throw new CurlParseException(
                "Comando cURL rejeitado: contém backtick '`' que representa risco de injeção de shell");
        }
        if (curlCommand.contains(";")) {
            throw new CurlParseException(
                "Comando cURL rejeitado: contém ponto e vírgula ';' que representa risco de injeção de shell");
        }
    }

    @Override
    public ParsedRequest parse(String curlCommand) throws CurlParseException {
        if (curlCommand == null || curlCommand.isBlank()) {
            throw new CurlParseException("Comando cURL não pode ser vazio");
        }

        validateSecurity(curlCommand);

        List<String> tokens = CurlTokenizer.tokenize(curlCommand);

        if (tokens.isEmpty() || !tokens.get(0).equalsIgnoreCase("curl")) {
            throw new CurlParseException("Comando deve começar com 'curl'");
        }

        ParsedRequest request = new ParsedRequest();
        boolean explicitMethod = false;

        int i = 1; // skip "curl"
        while (i < tokens.size()) {
            String token = tokens.get(i);
            switch (token) {
                case "-X":
                case "--request":
                    i++;
                    if (i >= tokens.size()) {
                        throw new CurlParseException("Flag " + token + " requer um valor");
                    }
                    request.setMethod(tokens.get(i).toUpperCase());
                    explicitMethod = true;
                    break;

                case "-H":
                case "--header":
                    i++;
                    if (i >= tokens.size()) {
                        throw new CurlParseException("Flag " + token + " requer um valor");
                    }
                    String headerStr = tokens.get(i);
                    String[] parts = headerStr.split(":", 2);
                    if (parts.length == 2) {
                        request.addHeader(parts[0].trim(), parts[1].trim());
                    }
                    break;

                case "-d":
                case "--data":
                case "--data-raw":
                    i++;
                    if (i >= tokens.size()) {
                        throw new CurlParseException("Flag " + token + " requer um valor");
                    }
                    request.setBody(tokens.get(i));
                    if (!explicitMethod) {
                        request.setMethod("POST");
                    }
                    break;

                case "-u":
                case "--user":
                    i++;
                    if (i >= tokens.size()) {
                        throw new CurlParseException("Flag " + token + " requer um valor");
                    }
                    request.setBasicAuth(tokens.get(i));
                    break;

                case "--url":
                    i++;
                    if (i >= tokens.size()) {
                        throw new CurlParseException("Flag " + token + " requer um valor");
                    }
                    request.setUrl(tokens.get(i));
                    break;

                default:
                    if (!token.startsWith("-")) {
                        request.setUrl(token);
                    }
                    break;
            }
            i++;
        }

        if (request.getUrl() == null) {
            throw new CurlParseException("URL não encontrada no comando cURL");
        }

        return request;
    }
}
