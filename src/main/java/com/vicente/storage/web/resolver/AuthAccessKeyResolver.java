package com.vicente.storage.web.resolver;

import com.vicente.storage.annotation.AuthAccessKey;
import com.vicente.storage.exception.MissingAccessKeyContextException;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * <h2>AuthAccessKey Argument Resolver</h2>
 *
 * <p>
 * Implementação customizada de {@link HandlerMethodArgumentResolver}
 * responsável por resolver e injetar automaticamente o <b>accessKeyId</b>
 * em parâmetros de métodos de controllers anotados com {@code @AuthAccessKey}.
 * </p>
 *
 * <h3>Responsabilidade</h3>
 * <ul>
 *     <li>Detectar parâmetros anotados com {@code @AuthAccessKey}</li>
 *     <li>Obter o accessKeyId do contexto da requisição</li>
 *     <li>Injetar o valor automaticamente no controller</li>
 * </ul>
 *
 * <h3>Como funciona</h3>
 * <ol>
 *     <li>O {@code AuthInterceptor} autentica a requisição</li>
 *     <li>O accessKeyId é salvo via {@code request.setAttribute}</li>
 *     <li>Este resolver intercepta a chamada do controller</li>
 *     <li>Recupera o accessKeyId e injeta no parâmetro</li>
 * </ol>
 *
 * <h3>Exemplo de uso</h3>
 * <pre>{@code
 * public ResponseEntity<Void> create(
 *     @AuthAccessKey Long accessKeyId
 * ) {
 *     // accessKeyId já resolvido automaticamente
 * }
 * }</pre>
 *
 * <h3>Benefícios</h3>
 * <ul>
 *     <li>Remove dependência direta de HttpServletRequest nos controllers</li>
 *     <li>Mantém código limpo e desacoplado</li>
 *     <li>Centraliza lógica de resolução de autenticação</li>
 * </ul>
 *
 * @see AuthAccessKey
 */
@Component
public class AuthAccessKeyResolver implements HandlerMethodArgumentResolver {

    /**
     * Verifica se o parâmetro do método deve ser resolvido por este resolver.
     *
     * <p>
     * A condição é:
     * </p>
     * <ul>
     *     <li>O parâmetro deve estar anotado com {@code @AuthAccessKey}</li>
     *     <li>O tipo do parâmetro deve ser {@code Long}</li>
     * </ul>
     *
     * @param parameter method parameter metadata
     * @return true if this resolver should handle the parameter
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterAnnotation(AuthAccessKey.class) != null
                && parameter.getParameterType().equals(Long.class);
    }

    /**
     * Resolve o valor do argumento e injeta no parâmetro do controller.
     *
     * <p>
     * O valor é obtido a partir do atributo {@code accessKeyId}
     * previamente definido no {@code HttpServletRequest}.
     * </p>
     *
     * @param parameter method parameter metadata
     * @param mavContainer ModelAndView container (not used here)
     * @param webRequest web request wrapper
     * @param binderFactory data binder factory (not used here)
     * @return resolved accessKeyId value
     *
     * @throws MissingAccessKeyContextException if accessKeyId is not present in the request context
     */
    @Override
    public @Nullable Object resolveArgument(
            @NonNull MethodParameter parameter,
            @Nullable ModelAndViewContainer mavContainer,
            @NonNull NativeWebRequest webRequest,
            @Nullable WebDataBinderFactory binderFactory
    ) {
        // Obtém o HttpServletRequest real a partir do wrapper NativeWebRequest
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        Object accessKeyId = request != null ? request.getAttribute("accessKeyId") : null;

        if(accessKeyId == null){
            throw new MissingAccessKeyContextException("AccessKeyId not found in request context");
        }
        
        return accessKeyId;
    }
}
