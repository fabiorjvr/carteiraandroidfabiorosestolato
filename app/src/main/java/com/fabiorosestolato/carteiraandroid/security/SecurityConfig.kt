package com.fabiorosestolato.carteiraandroid.security

import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey

/**
 * Configurações centralizadas de segurança para o aplicativo CarteiraDigitalMVP.
 * Centraliza todas as configurações relacionadas à criptografia e segurança.
 */
object SecurityConfig {
    
    /**
     * Configurações de criptografia
     */
    object Encryption {
        // Esquema de criptografia para arquivos
        val FILE_ENCRYPTION_SCHEME = EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        
        // Esquema da chave mestra
        val MASTER_KEY_SCHEME = MasterKey.KeyScheme.AES256_GCM
        
        // Qualidade de compressão JPEG (0-100)
        const val JPEG_COMPRESSION_QUALITY = 85
        
        // Tamanho máximo do arquivo em bytes (5MB)
        const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024
    }
    
    /**
     * Configurações de arquivos
     */
    object Files {
        // Extensão para arquivos criptografados
        const val ENCRYPTED_FILE_EXTENSION = ".enc"
        
        // Extensão para arquivos de imagem
        const val IMAGE_FILE_EXTENSION = ".jpg"
        
        // Prefixo para arquivos temporários
        const val TEMP_FILE_PREFIX = "temp_"
        
        // Tempo de vida dos arquivos temporários em milissegundos (1 hora)
        const val TEMP_FILE_LIFETIME_MS = 60 * 60 * 1000L
    }
    
    /**
     * Configurações do FileProvider
     */
    object FileProvider {
        // Autoridade do FileProvider
        const val AUTHORITY = "com.fabiorosestolato.carteiraandroid.fileprovider"
        
        // Paths configurados no XML
        const val CACHE_PATH_NAME = "temp_images"
        const val FILES_PATH_NAME = "encrypted_docs"
        const val EXTERNAL_CACHE_PATH_NAME = "external_temp"
        const val EXTERNAL_FILES_PATH_NAME = "external_files"
    }
    
    /**
     * Configurações de câmera
     */
    object Camera {
        // Modo de captura para máxima qualidade
        const val CAPTURE_MODE = androidx.camera.core.ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
        
        // Formato de saída
        const val OUTPUT_FORMAT = android.graphics.Bitmap.CompressFormat.JPEG
        
        // Resolução máxima recomendada (para evitar arquivos muito grandes)
        const val MAX_RESOLUTION_WIDTH = 1920
        const val MAX_RESOLUTION_HEIGHT = 1080
    }
    
    /**
     * Configurações de validação
     */
    object Validation {
        // Tipos MIME aceitos
        val ACCEPTED_MIME_TYPES = listOf(
            "image/jpeg",
            "image/jpg"
        )
        
        // Tamanho mínimo da imagem em pixels
        const val MIN_IMAGE_WIDTH = 300
        const val MIN_IMAGE_HEIGHT = 300
        
        // Tamanho máximo da imagem em pixels
        const val MAX_IMAGE_WIDTH = 4096
        const val MAX_IMAGE_HEIGHT = 4096
    }
    
    /**
     * Configurações de logging (para debug)
     */
    object Logging {
        // Tag padrão para logs
        const val DEFAULT_TAG = "CarteiraDigital"
        
        // Habilitar logs detalhados (deve ser false em produção)
        const val ENABLE_VERBOSE_LOGGING = false
        
        // Habilitar logs de operações de arquivo
        const val ENABLE_FILE_OPERATION_LOGS = false
    }
    
    /**
     * Configurações de performance
     */
    object Performance {
        // Número máximo de operações simultâneas de arquivo
        const val MAX_CONCURRENT_FILE_OPERATIONS = 3
        
        // Timeout para operações de arquivo em milissegundos
        const val FILE_OPERATION_TIMEOUT_MS = 30000L
        
        // Tamanho do buffer para operações de I/O
        const val IO_BUFFER_SIZE = 8192
    }
    
    /**
     * Configurações de backup e recuperação
     */
    object Backup {
        // Habilitar backup automático (false por segurança)
        const val ENABLE_AUTO_BACKUP = false
        
        // Incluir arquivos criptografados no backup do sistema
        const val INCLUDE_ENCRYPTED_FILES_IN_BACKUP = false
        
        // Versão do esquema de dados
        const val DATA_SCHEMA_VERSION = 1
    }
    
    /**
     * Verifica se as configurações de segurança estão adequadas para produção
     */
    fun validateProductionSecurity(): List<String> {
        val issues = mutableListOf<String>()
        
        if (Logging.ENABLE_VERBOSE_LOGGING) {
            issues.add("Logging verboso habilitado em produção")
        }
        
        if (Logging.ENABLE_FILE_OPERATION_LOGS) {
            issues.add("Logs de operações de arquivo habilitados em produção")
        }
        
        if (Backup.ENABLE_AUTO_BACKUP) {
            issues.add("Backup automático habilitado (risco de segurança)")
        }
        
        if (Backup.INCLUDE_ENCRYPTED_FILES_IN_BACKUP) {
            issues.add("Arquivos criptografados incluídos no backup")
        }
        
        return issues
    }
    
    /**
     * Obtém configurações resumidas para debug
     */
    fun getConfigSummary(): Map<String, Any> {
        return mapOf(
            "encryption_scheme" to FILE_ENCRYPTION_SCHEME.name,
            "master_key_scheme" to MASTER_KEY_SCHEME.name,
            "max_file_size_mb" to Encryption.MAX_FILE_SIZE_BYTES / (1024 * 1024),
            "jpeg_quality" to Encryption.JPEG_COMPRESSION_QUALITY,
            "temp_file_lifetime_hours" to Files.TEMP_FILE_LIFETIME_MS / (60 * 60 * 1000),
            "verbose_logging" to Logging.ENABLE_VERBOSE_LOGGING,
            "auto_backup" to Backup.ENABLE_AUTO_BACKUP,
            "data_schema_version" to Backup.DATA_SCHEMA_VERSION
        )
    }
}