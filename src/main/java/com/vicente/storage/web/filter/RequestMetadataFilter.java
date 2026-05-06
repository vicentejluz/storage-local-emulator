package com.vicente.storage.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class RequestMetadataFilter extends OncePerRequestFilter {

    /**
     * <h2>Request Metadata Filter</h2>
     *
     * <p>
     * Filter responsável por enriquecer todas as requisições HTTP com metadados essenciais
     * para rastreabilidade e observabilidade.
     * </p>
     *
     * <h3>Responsabilidades</h3>
     * <ul>
     *     <li>Gerar ou propagar o <b>requestId</b> (correlation ID)</li>
     *     <li>Disponibilizar o requestId no contexto da requisição</li>
     *     <li>Adicionar headers padrão na resposta (X-Request-Id, Date)</li>
     * </ul>
     *
     * <h3>Fluxo de funcionamento</h3>
     * <ol>
     *     <li>Verifica se o client enviou o header <code>X-Request-Id</code></li>
     *     <li>Se não existir, gera um UUID</li>
     *     <li>Armazena no request via <code>setAttribute</code></li>
     *     <li>Propaga o valor no response header</li>
     *     <li>Continua a execução da filter chain</li>
     * </ol>
     *
     * <h3>Por que isso é importante?</h3>
     * <p>
     * Em ambientes distribuídos, o requestId permite:
     * </p>
     * <ul>
     *     <li>Correlacionar logs entre diferentes camadas</li>
     *     <li>Rastrear erros em produção</li>
     *     <li>Facilitar debugging e suporte</li>
     *     <li>Integrar com sistemas de tracing (ex: OpenTelemetry)</li>
     * </ul>
     *
     * <h3>Observações</h3>
     * <ul>
     *     <li>Executa apenas uma vez por requisição (OncePerRequestFilter)</li>
     *     <li>Deve ser um dos primeiros filtros na chain</li>
     *     <li>O requestId é retornado ao client via header</li>
     * </ul>
     *
     * @param request the current HTTP request
     * @param response the current HTTP response
     * @param filterChain the filter chain
     *
     * @throws ServletException if a servlet-related error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        /*
         * Obtém o requestId enviado pelo client.
         *
         * IMPORTANTE:
         * - Esse valor é opcional
         * - Pode ser usado por sistemas externos para correlacionar chamadas
         */
        String requestId = request.getHeader("X-Request-Id");

        /*
         * Caso o client não envie, geramos um novo UUID.
         *
         * UUID é suficiente para garantir unicidade na prática.
         */
        if(requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }

        /*
         * Armazena o requestId no contexto da requisição.
         *
         * Isso permite acessar em:
         * - Interceptors
         * - Controllers
         * - Services (via RequestContextHolder, se necessário)
         * - ExceptionHandler
         *
         * Exemplo:
         * request.getAttribute("requestId")
         */
        request.setAttribute("requestId", requestId);

        /*
         * Define o header de resposta com o requestId.
         *
         * Isso é CRÍTICO para:
         * - Debug do lado do client
         * - Suporte (cliente manda o requestId do erro)
         */
        response.setHeader("X-Request-Id", requestId);

        /*
         * Define o header HTTP Date no padrão RFC 1123.
         *
         * Exemplo:
         * Tue, 05 May 2026 22:10:00 GMT
         *
         * Isso melhora:
         * - Compatibilidade HTTP
         * - Debug de tempo de resposta
         */
        response.setHeader(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME
                .format(ZonedDateTime.now(ZoneOffset.UTC)));

        /*
         * Continua a execução da chain (Controller, Interceptors, etc).
         *
         * IMPORTANTE:
         * - Se não chamar isso, a requisição "morre" aqui.
         */
        filterChain.doFilter(request, response);

        // Garante que ninguém sobrescreveu
        response.setHeader("X-Request-Id", requestId);
        response.setHeader(HttpHeaders.DATE, DateTimeFormatter.RFC_1123_DATE_TIME
                .format(ZonedDateTime.now(ZoneOffset.UTC)));
    }
}
