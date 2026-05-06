package com.vicente.storage.config;

import com.vicente.storage.web.interceptor.AuthInterceptor;
import com.vicente.storage.web.resolver.AuthAccessKeyResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * <h2>Web MVC Configuration</h2>
 *
 * <p>
 * Classe de configuração responsável por customizar o comportamento do Spring MVC.
 * </p>
 *
 * <p>
 * Aqui registramos componentes que participam do ciclo de vida da requisição HTTP,
 * como:
 * </p>
 *
 * <ul>
 *     <li>Interceptors (pré/pós processamento de requisições)</li>
 *     <li>Argument Resolvers (injeção customizada de parâmetros)</li>
 * </ul>
 *
 * <h3>Responsabilidades</h3>
 * <ul>
 *     <li>Registrar o {@code AuthInterceptor} para autenticação de requisições</li>
 *     <li>Registrar o {@code AuthAccessKeyResolver} para injeção automática de parâmetros</li>
 * </ul>
 *
 * <h3>Fluxo da requisição</h3>
 * <ol>
 *     <li>Request chega na aplicação</li>
 *     <li>O {@code AuthInterceptor} é executado (preHandle)</li>
 *     <li>Autenticação é realizada e o accessKeyId é salvo no request</li>
 *     <li>Spring resolve os parâmetros do controller</li>
 *     <li>O {@code AuthAccessKeyResolver} injeta o accessKeyId</li>
 *     <li>Controller executa normalmente</li>
 * </ol>
 *
 * <h3>Por que usar WebMvcConfigurer?</h3>
 * <p>
 * A interface {@code WebMvcConfigurer} permite customizar o comportamento padrão
 * do Spring MVC sem sobrescrever a configuração automática do framework.
 * </p>
 *
 * <h3>Benefícios</h3>
 * <ul>
 *     <li>Centraliza configuração de infraestrutura web</li>
 *     <li>Separa responsabilidades (auth, resolução de parâmetros, etc)</li>
 *     <li>Mantém controllers limpos e desacoplados</li>
 * </ul>
 *
 * @see AuthInterceptor
 * @see AuthAccessKeyResolver
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    /*
     * Interceptor responsável por autenticar as requisições
     * antes de chegarem ao controller.
     */
    private final AuthInterceptor interceptor;

    /*
     * Resolver responsável por injetar automaticamente o accessKeyId
     * nos parâmetros anotados com @AuthAccessKey.
     */
    private final AuthAccessKeyResolver resolver;

    public WebConfig(AuthInterceptor interceptor, AuthAccessKeyResolver resolver) {
        this.interceptor = interceptor;
        this.resolver = resolver;
    }

    /**
     * Registra interceptors no ciclo de vida da requisição.
     *
     * <p>
     * O interceptor será executado antes do controller (preHandle),
     * podendo interromper a requisição ou enriquecê-la com dados.
     * </p>
     *
     * <p>
     * Neste caso:
     * </p>
     * <ul>
     *     <li>Aplica o {@code AuthInterceptor}</li>
     *     <li>Apenas para rotas que começam com <code>/api/v1/**</code></li>
     * </ul>
     *
     * <p>
     * Isso evita aplicar autenticação em endpoints públicos
     * (ex: health check, actuator, etc).
     * </p>
     *
     * @param registry Spring MVC interceptor registry
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor) // registra o interceptor
                .addPathPatterns("/api/v1/**"); // aplica apenas nessas rotas
    }

    /**
     * Registra resolvers de argumentos customizados.
     *
     * <p>
     * Argument resolvers permitem que o Spring injete automaticamente
     * valores em parâmetros de métodos de controllers.
     * </p>
     *
     * <p>
     * Neste caso:
     * </p>
     * <ul>
     *     <li>Detecta parâmetros anotados com {@code @AuthAccessKey}</li>
     *     <li>Resolve o valor a partir do request</li>
     *     <li>Injeta automaticamente no controller</li>
     * </ul>
     *
     * <p>
     * Esse mecanismo substitui a necessidade de acessar manualmente
     * o {@code HttpServletRequest} dentro dos controllers.
     * </p>
     *
     * @param resolvers list of available argument resolvers in Spring MVC
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(resolver);
    }
}
