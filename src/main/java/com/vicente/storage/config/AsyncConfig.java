package com.vicente.storage.config;


import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableAsync
public class AsyncConfig {
    // Registra esse método como um Bean do Spring IoC container.
    // O nome "asyncCleanupExecutor" permite você referenciar ele no @Async:
    // @Async("asyncCleanupExecutor")
    // Ou injetar manualmente como Executor.
    @Bean(name = "asyncCleanupExecutor")
    public Executor asyncCleanupExecutor() {
        // Cria um ThreadFactory baseado em Virtual Threads (Project Loom - Java 21+)
        // Virtual threads são threads leves gerenciadas pela JVM, não pelo SO.
        // Ideal para tarefas I/O blocking como:
        // - delete de arquivos
        // - rename/move
        // - chamadas de rede
        // - operações de storage
        ThreadFactory threadFactory = Thread.ofVirtual()
                // Define o prefixo do nome das threads geradas.
                // Isso serve EXCLUSIVAMENTE para:
                // - logs
                // - debug
                // - observabilidade

                // Exemplo de threads geradas:
                // file-cleanup-1
                // file-cleanup-2
                // file-cleanup-3

                // O "1" indica o índice inicial da sequência.
                // (não muda comportamento funcional, só estética)
                .name("file-cleanup-", 1)
                // Constrói o ThreadFactory final que será usado pelo executor.
                // Ele encapsula a criação das virtual threads com o naming configurado.
                .factory();

        // Cria um Executor baseado no modelo "1 task = 1 thread".
        //
        // IMPORTANTE:
        // - Cada tarefa submetida recebe uma virtual thread nova
        // - Não existe pool fixo como ThreadPoolTaskExecutor
        // - Não há fila tradicional de espera
        //
        // Vantagens:
        // - altíssima escalabilidade
        // - ideal para workloads de I/O
        // - elimina tuning de corePoolSize/maxPoolSize
        return Executors.newThreadPerTaskExecutor(threadFactory);
    }
}
