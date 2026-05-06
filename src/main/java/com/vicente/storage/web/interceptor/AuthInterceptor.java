package com.vicente.storage.web.interceptor;

import com.vicente.storage.dto.AuthRequestDTO;
import com.vicente.storage.exception.InvalidAccessKeyException;
import com.vicente.storage.exception.InvalidTimestampException;
import com.vicente.storage.exception.MissingAuthHeaderException;
import com.vicente.storage.service.AuthService;
import com.vicente.storage.web.resolver.AuthAccessKeyResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

/**
 * <h2>Authentication Interceptor</h2>
 *
 * <p>
 * Interceptor responsável por realizar a autenticação de requisições HTTP
 * antes que elas cheguem aos controllers.
 * </p>
 *
 * <h3>Responsabilidades</h3>
 * <ul>
 *     <li>Extrair headers de autenticação da requisição</li>
 *     <li>Validar presença e formato dos dados</li>
 *     <li>Delegar autenticação para o {@code AuthService}</li>
 *     <li>Armazenar o {@code accessKeyId} no contexto da requisição</li>
 * </ul>
 *
 * <h3>Headers esperados</h3>
 * <ul>
 *     <li><b>X-Access-Key</b> → identificador da chave de acesso</li>
 *     <li><b>X-Signature</b> → assinatura HMAC da requisição</li>
 *     <li><b>X-Timestamp</b> → timestamp da requisição (ISO-8601)</li>
 * </ul>
 *
 * <h3>Fluxo de execução</h3>
 * <ol>
 *     <li>Valida presença dos headers obrigatórios</li>
 *     <li>Valida tamanho da access key</li>
 *     <li>Converte e valida o timestamp</li>
 *     <li>Cria o {@code AuthRequestDTO}</li>
 *     <li>Executa autenticação via {@code AuthService}</li>
 *     <li>Salva o {@code accessKeyId} no request</li>
 * </ol>
 *
 * <h3>Integração com o fluxo</h3>
 * <p>
 * O {@code accessKeyId} armazenado no request será posteriormente utilizado pelo
 * {@code AuthAccessKeyResolver} para injeção automática nos controllers.
 * </p>
 *
 * <h3>Comportamento em caso de erro</h3>
 * <ul>
 *     <li>Lança exceptions customizadas (extends {@code ApiException})</li>
 *     <li>As exceptions são tratadas pelo {@code GlobalExceptionHandler}</li>
 * </ul>
 *
 * @see AuthService
 * @see AuthAccessKeyResolver
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    /**
     * Executado antes do controller para validar e autenticar a requisição.
     *
     * <p>
     * Caso a autenticação seja bem-sucedida, o {@code accessKeyId} é armazenado
     * no {@code HttpServletRequest} para uso posterior.
     * </p>
     *
     * <p>
     * Caso ocorra qualquer erro de validação ou autenticação, uma exception é lançada,
     * interrompendo o fluxo da requisição.
     * </p>
     *
     * @param request current HTTP request
     * @param response current HTTP response
     * @param handler handler that will process the request
     * @return true to continue the request processing chain
     *
     * @throws MissingAuthHeaderException if required authentication headers are missing or blank
     * @throws InvalidAccessKeyException if the access key length is invalid
     * @throws InvalidTimestampException if the timestamp format is invalid
     */
    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) {
        String accessKey = request.getHeader("X-Access-Key");
        String signature = request.getHeader("X-Signature");
        String timestampHeader = request.getHeader("X-Timestamp");

        if ((accessKey == null || accessKey.isBlank()) || (signature == null || signature.isBlank())
                || (timestampHeader == null || timestampHeader.isBlank())) {
            throw new MissingAuthHeaderException("Missing authentication headers");
        }
        if (accessKey.length() < 3 || accessKey.length() > 64) {
            throw new InvalidAccessKeyException("Invalid X-Access-Key size");
        }

        Instant timestamp;
        try {
            timestamp = Instant.parse(timestampHeader);
        } catch (Exception e) {
            throw new InvalidTimestampException("Invalid timestamp format");
        }

        AuthRequestDTO authRequestDTO = new AuthRequestDTO(accessKey, signature, timestamp, request.getMethod(),
                request.getRequestURI(), request.getQueryString(), request.getContentType(), request.getContentLengthLong());

        long accessKeyId = authService.authenticate(authRequestDTO);

        request.setAttribute("accessKeyId", accessKeyId);
        return true;
    }
}
