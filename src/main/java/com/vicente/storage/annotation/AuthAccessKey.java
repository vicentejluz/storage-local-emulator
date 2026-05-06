package com.vicente.storage.annotation;

import com.vicente.storage.web.resolver.AuthAccessKeyResolver;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <h2>Annotation de Chave de Acesso Autenticada</h2>
 *
 * <p>
 * Annotation customizada utilizada para injetar automaticamente o <b>accessKeyId</b>
 * autenticado diretamente nos parâmetros de métodos dos controllers.
 * </p>
 *
 * <h3>Propósito</h3>
 * <p>
 * Essa annotation funciona como um marcador para o
 * {@code AuthAccessKeyResolver}, permitindo que o Spring resolva
 * e injete o identificador da access key a partir do contexto da requisição.
 * </p>
 *
 * <h3>Como funciona</h3>
 * <ol>
 *     <li>A requisição é interceptada por uma camada de autenticação (interceptor ou filter)</li>
 *     <li>O {@code AuthService} valida a requisição e extrai o accessKeyId</li>
 *     <li>O accessKeyId é armazenado na requisição (via {@code request.setAttribute})</li>
 *     <li>O {@code AuthAccessKeyResolver} detecta essa annotation</li>
 *     <li>O resolver injeta o accessKeyId no parâmetro do método</li>
 * </ol>
 *
 * <h3>Exemplo de uso</h3>
 * <pre>{@code
 * @PutMapping("/{bucket}")
 * public ResponseEntity<Void> create(
 *     @PathVariable String bucketName,
 *     @AuthAccessKey Long accessKeyId
 * ) {
 *     bucketService.createBucket(bucketName, accessKeyId);
 *     return ResponseEntity.noContent().build();
 * }
 * }</pre>
 *
 * <h3>Benefícios</h3>
 * <ul>
 *     <li>Remove a necessidade de extrair dados de autenticação manualmente no controller</li>
 *     <li>Mantém o controller limpo e focado na regra de negócio</li>
 *     <li>Centraliza a lógica de autenticação</li>
 * </ul>
 *
 * <h3>Escopo</h3>
 * <ul>
 *     <li>Pode ser utilizada apenas em parâmetros de métodos</li>
 *     <li>Depende de um {@code HandlerMethodArgumentResolver} para funcionar</li>
 * </ul>
 *
 * @see AuthAccessKeyResolver
 */

/*
 * Define onde a annotation pode ser usada.
 * PARAMETER = apenas em parâmetros de método.
 *
 * Outros possíveis:
 * - TYPE → classes
 * - METHOD → métodos
 * - FIELD → atributos
 * - CONSTRUCTOR → construtores
 * - ANNOTATION_TYPE → outras annotations
 * - PACKAGE → pacotes
 * - TYPE_USE → uso em tipos (avançado)
 */
@Target(ElementType.PARAMETER)
/*
 * Define até quando a annotation existe.
 * RUNTIME = disponível em tempo de execução (necessário para o Spring ler via reflection).
 *
 * Outros possíveis:
 * - CLASS → fica no .class, mas não disponível em runtime
 * - SOURCE → descartada na compilação
*/
@Retention(RetentionPolicy.RUNTIME)
/*
 * Declara uma annotation customizada em Java.
 *
 * @interface é usado para definir annotations (anotações),
 * que são tipos especiais de metadados no código.
 *
 * Essas annotations NÃO executam lógica por si só.
 * Elas apenas "marcam" elementos do código.
 *
 * Quem interpreta essa marcação geralmente é:
 * - O Spring Framework
 * - Reflection (Java)
 * - Bibliotecas/frameworks
 *
 * Neste caso específico:
 * - @AuthAccessKey marca um parâmetro do controller
 * - O AuthAccessKeyResolver detecta essa annotation
 * - O resolver injeta automaticamente o accessKeyId
 *
 * Exemplo de uso:
 * @AuthAccessKey Long accessKeyId
 *
 * Importante:
 * - Annotations são como "interfaces especiais"
 * - Não possuem implementação direta
 * - Servem como contrato/metadado
 *
 * Resumo:
 * @interface = criação de uma annotation customizada
 */
public @interface AuthAccessKey {
}
