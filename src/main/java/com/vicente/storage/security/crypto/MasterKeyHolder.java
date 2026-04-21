package com.vicente.storage.security.crypto;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

// Marca a classe como um bean gerenciado pelo Spring.
// Isso significa que o Spring cria uma única instância (singleton por padrão)
// e permite injeção com @Autowired / constructor injection em outros componentes.
//
// Neste caso, ele funciona como um armazenamento central em memória
// da Master Key atualmente ativa.
@Component
public class MasterKeyHolder {
    // volatile garante VISIBILIDADE entre threads.
    //
    // Sem volatile:
    // Thread A poderia atualizar a chave
    // Thread B poderia continuar enxergando valor antigo em cache da CPU.
    //
    // Com volatile:
    // toda escrita feita aqui fica imediatamente visível
    // para outras threads que fizerem leitura depois.
    //
    // É importante porque várias threads podem criptografar/decriptografar
    // ao mesmo tempo enquanto outra thread atualiza a chave.
    //
    // SecretKey representa a chave criptográfica em memória.
    private volatile SecretKey activeMasterKey;
    // volatile pelo mesmo motivo acima:
    // garante que a versão carregada da chave seja visível para todas threads.
    //
    // A versão pode ser usada para:
    // - logs
    // - auditoria
    // - identificar qual chave criptografou determinado dado
    // - rotação de chave
    private volatile Long activeVersion;

    // Método público para obter a Master Key atual.
    //
    // Normalmente será usado pelo serviço criptográfico:
    // SecretKey key = holder.getActiveMasterKey();
    public SecretKey getActiveMasterKey() {
        if(activeMasterKey == null) {
            // Se estiver null significa que a aplicação ainda não inicializou
            // a chave corretamente ou houve falha no startup.
            //
            // Lançar exception aqui é melhor do que retornar null,
            // pois falha rápido e evita NullPointerException depois.
            throw new IllegalStateException("Master key not loaded");
        }
        // Retorna a chave atualmente ativa em memória.
        return activeMasterKey;
    }

    // Retorna a versão da chave ativa.
    //
    // Como é volatile, qualquer thread verá valor atualizado.
    public Long getActiveVersion() {
        return activeVersion;
    }

    // synchronized permite exclusão mútua.
    //
    // Isso significa:
    // apenas UMA thread por vez pode executar esse método.
    //
    // Importante porque atualizar chave + versão precisa ser atômico:
    //
    // errado:
    // thread lê chave nova e versão antiga
    //
    // correto:
    // ambas atualizadas juntas como uma única operação lógica.
    //
    // Sem synchronized poderia haver condição de corrida.
    public synchronized void load(SecretKey key,  Long version) {
        // Atualiza a chave ativa em memória.
        //
        // Após essa escrita, graças ao volatile,
        // outras threads passam a enxergar o novo valor.
        this.activeMasterKey = key;
        // Atualiza a versão correspondente.
        //
        // Como o método é synchronized,
        // chave e versão são trocadas em sequência protegida.
        this.activeVersion = version;
    }
}
