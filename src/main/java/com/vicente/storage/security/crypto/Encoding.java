package com.vicente.storage.security.crypto;

import java.util.Base64;
import java.util.HexFormat;

/**
 * Encoding representa estratégias de codificação e decodificação de bytes.
 *
 * <h2>Design Pattern</h2>
 * <p>
 * Este enum implementa o padrão <b>Strategy Pattern</b>, onde cada constante
 * representa uma estratégia diferente de encoding/decoding.
 * </p>
 *
 * <p>
 * Neste caso específico, o Strategy Pattern está sendo aplicado via <b>Enum Strategy</b>,
 * onde cada constante do enum encapsula seu próprio comportamento, eliminando
 * a necessidade de condicionais (if/switch) e promovendo polimorfismo.
 * </p>
 *
 * <h2>Tipo de Strategy usado</h2>
 * <p>
 * Este é um <b>Strategy Pattern baseado em Enum Polymorphism</b> (também conhecido como
 * "Enum-based Strategy").
 * </p>
 *
 * <h2>Benefícios dessa abordagem</h2>
 * <ul>
 *     <li>Remove lógica condicional (if/switch)</li>
 *     <li>Facilita extensão (nova estratégia = nova constante)</li>
 *     <li>Melhora legibilidade e coesão</li>
 *     <li>Cada encoding encapsula sua própria implementação</li>
 * </ul>
 *
 * <h2>Responsabilidade</h2>
 * <p>
 * Cada implementação deve garantir:
 * </p>
 * <ul>
 *     <li>encode(byte[]) → String</li>
 *     <li>decode(String) → byte[]</li>
 * </ul>
 *
 */
public enum Encoding {
    /**
     * Codificação hexadecimal (base 16)
     * <p>
     * Exemplo: "4a6f686e"
     */
    HEX{
        @Override
        public String encode(byte[] raw) {
            return HexFormat.of().formatHex(raw);
        }

        @Override
        public byte[] decode(String value) {
            return HexFormat.of().parseHex(value);
        }
    },

    /**
     * Codificação Base64 padrão (RFC 4648)
     * <p>
     * Inclui padding "="
     * <p>
     * Exemplo: "Sm9obg=="
     */
    BASE64{
        @Override
        public String encode(byte[] raw) {
            return Base64.getEncoder().encodeToString(raw);
        }

        @Override
        public byte[] decode(String value) {
            return Base64.getDecoder().decode(value);
        }
    },

    /**
     * Codificação Base64 URL-safe
     * <p>
     * - Substitui '+' por '-'
     * <p>
     * - Substitui '/' por '_'
     * <p>
     * - Remove padding "="
     * <p>
     * Ideal para URLs (ex: pre-signed URLs)
     */
    BASE64_URL {
        @Override
        public String encode(byte[] raw) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        }

        @Override
        public byte[] decode(String value) {
            return Base64.getUrlDecoder().decode(value);
        }
    };

    /**
     * Codifica um array de bytes em String
     *
     * @param raw binary input data
     * @return encoded string representation
     */
    public abstract String encode(byte[] raw);

    /**
     * Decodifica uma String para array de bytes
     *
     * @param value encoded string value
     * @return decoded byte array
     */
    public abstract byte[] decode(String value);
}
