package com.fabiorosestolato.carteiraandroid.security

import android.content.Context
import android.util.Log
import com.fabiorosestolato.carteiraandroid.BuildConfig

/**
 * Logger seguro que sanitiza automaticamente dados sensíveis e controla
 * a verbosidade baseada no ambiente de desenvolvimento/produção.
 */
object SecureLogger {
    
    private const val DEFAULT_TAG = "CarteiraDigital"
    private const val MAX_LOG_LENGTH = 4000
    
    // Padrões regex para sanitização de dados sensíveis
    private val SENSITIVE_PATTERNS = mapOf(
        "cpf" to "\\b\\d{3}\\.?\\d{3}\\.?\\d{3}-?\\d{2}\\b".toRegex(),
        "rg" to "\\b\\d{1,2}\\.?\\d{3}\\.?\\d{3}-?[\\dxX]\\b".toRegex(),
        "phone" to "\\b\\(?\\d{2}\\)?\\s?9?\\d{4}-?\\d{4}\\b".toRegex(),
        "email" to "\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b".toRegex(),
        "password" to "(?i)(password|senha|pass|pwd)\\s*[:=]\\s*\\S+".toRegex(),
        "token" to "(?i)(token|key|secret)\\s*[:=]\\s*\\S+".toRegex()
    )
    
    private val REPLACEMENT_PATTERNS = mapOf(
        "cpf" to "***_CPF***",
        "rg" to "***_RG***",
        "phone" to "***_PHONE***",
        "email" to "***_EMAIL***",
        "password" to "***_PASSWORD***",
        "token" to "***_TOKEN***"
    )
    
    /**
     * Log de debug - apenas em modo desenvolvimento
     * @param tag Tag do log
     * @param message Mensagem a ser logada
     * @param throwable Exceção opcional
     */
    fun d(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (BuildConfig.DEBUG) {
            val sanitizedMessage = sanitizeMessage(message)
            logLongMessage(Log.DEBUG, tag, sanitizedMessage, throwable)
        }
    }
    
    /**
     * Log de informação
     * @param tag Tag do log
     * @param message Mensagem a ser logada
     * @param throwable Exceção opcional
     */
    fun i(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitizeMessage(message)
        logLongMessage(Log.INFO, tag, sanitizedMessage, throwable)
    }
    
    /**
     * Log de warning
     * @param tag Tag do log
     * @param message Mensagem a ser logada
     * @param throwable Exceção opcional
     */
    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitizeMessage(message)
        logLongMessage(Log.WARN, tag, sanitizedMessage, throwable)
    }
    
    /**
     * Log de erro
     * @param tag Tag do log
     * @param message Mensagem a ser logada
     * @param throwable Exceção opcional
     */
    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        val sanitizedMessage = sanitizeMessage(message)
        logLongMessage(Log.ERROR, tag, sanitizedMessage, throwable)
    }
    
    /**
     * Log específico para eventos de segurança
     * @param event Nome do evento de segurança
     * @param details Detalhes adicionais do evento
     */
    fun logSecurity(event: String, details: Map<String, Any> = emptyMap()) {
        val detailsString = details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        val message = "SECURITY_EVENT: $event${if (detailsString.isNotEmpty()) " | $detailsString" else ""}"
        i("Security", message)
        
        // TODO: Integração com Firebase Analytics (comentado para implementação futura)
        // FirebaseAnalytics.getInstance(context).logEvent("security_event", bundleOf(
        //     "event_name" to event,
        //     "details" to detailsString
        // ))
    }
    
    /**
     * Log para operações de documentos
     * @param operation Tipo da operação (save, read, delete, share)
     * @param documentType Tipo do documento
     * @param success Se a operação foi bem-sucedida
     * @param details Detalhes adicionais
     */
    fun logOperation(
        operation: String,
        documentType: String,
        success: Boolean,
        details: Map<String, Any> = emptyMap()
    ) {
        val status = if (success) "SUCCESS" else "FAILED"
        val detailsString = details.entries.joinToString(", ") { "${it.key}=${it.value}" }
        val message = "OPERATION: $operation | TYPE: $documentType | STATUS: $status${if (detailsString.isNotEmpty()) " | $detailsString" else ""}"
        i("Operations", message)
    }
    
    /**
     * Log para métricas de performance
     * @param operation Nome da operação
     * @param durationMs Duração em milissegundos
     * @param details Detalhes adicionais
     */
    fun logPerformance(
        operation: String,
        durationMs: Long,
        details: Map<String, Any> = emptyMap()
    ) {
        if (BuildConfig.DEBUG) {
            val detailsString = details.entries.joinToString(", ") { "${it.key}=${it.value}" }
            val message = "PERFORMANCE: $operation | DURATION: ${durationMs}ms${if (detailsString.isNotEmpty()) " | $detailsString" else ""}"
            d("Performance", message)
        }
    }
    
    /**
     * Sanitiza mensagem removendo dados sensíveis
     * @param message Mensagem original
     * @return Mensagem sanitizada
     */
    private fun sanitizeMessage(message: String): String {
        var sanitized = message
        
        SENSITIVE_PATTERNS.forEach { (type, pattern) ->
            val replacement = REPLACEMENT_PATTERNS[type] ?: "***_SENSITIVE***"
            sanitized = pattern.replace(sanitized, replacement)
        }
        
        return sanitized
    }
    
    /**
     * Loga mensagens longas dividindo em chunks
     * @param priority Prioridade do log
     * @param tag Tag do log
     * @param message Mensagem a ser logada
     * @param throwable Exceção opcional
     */
    private fun logLongMessage(priority: Int, tag: String, message: String, throwable: Throwable?) {
        if (message.length <= MAX_LOG_LENGTH) {
            when (priority) {
                Log.DEBUG -> if (throwable != null) Log.d(tag, message, throwable) else Log.d(tag, message)
                Log.INFO -> if (throwable != null) Log.i(tag, message, throwable) else Log.i(tag, message)
                Log.WARN -> if (throwable != null) Log.w(tag, message, throwable) else Log.w(tag, message)
                Log.ERROR -> if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
            }
        } else {
            // Divide mensagem em chunks
            var index = 0
            while (index < message.length) {
                val end = minOf(index + MAX_LOG_LENGTH, message.length)
                val chunk = message.substring(index, end)
                val chunkTag = "$tag[${index / MAX_LOG_LENGTH + 1}]"
                
                when (priority) {
                    Log.DEBUG -> Log.d(chunkTag, chunk)
                    Log.INFO -> Log.i(chunkTag, chunk)
                    Log.WARN -> Log.w(chunkTag, chunk)
                    Log.ERROR -> Log.e(chunkTag, chunk)
                }
                
                index = end
            }
            
            // Loga exceção apenas no último chunk
            if (throwable != null) {
                when (priority) {
                    Log.DEBUG -> Log.d(tag, "Exception details:", throwable)
                    Log.INFO -> Log.i(tag, "Exception details:", throwable)
                    Log.WARN -> Log.w(tag, "Exception details:", throwable)
                    Log.ERROR -> Log.e(tag, "Exception details:", throwable)
                }
            }
        }
    }
}

/**
 * Extension function para medir tempo de execução
 * @param operation Nome da operação
 * @param block Bloco de código a ser executado
 * @return Resultado do bloco
 */
inline fun <T> logTime(operation: String, block: () -> T): T {
    val startTime = System.currentTimeMillis()
    return try {
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        SecureLogger.logPerformance(operation, duration, mapOf("success" to true))
        result
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        SecureLogger.logPerformance(operation, duration, mapOf("success" to false, "error" to e.javaClass.simpleName))
        throw e
    }
}

/**
 * Extension function para inicializar o logger seguro no contexto
 * @param enableVerboseLogging Habilitar logs verbosos (apenas em debug)
 */
fun Context.initSecureLogger(enableVerboseLogging: Boolean = BuildConfig.DEBUG) {
    SecureLogger.i("SecureLogger", "Secure logging initialized | Debug: ${BuildConfig.DEBUG} | Verbose: $enableVerboseLogging")
    
    if (BuildConfig.DEBUG && enableVerboseLogging) {
        SecureLogger.d("SecureLogger", "Verbose logging enabled for development")
    }
}