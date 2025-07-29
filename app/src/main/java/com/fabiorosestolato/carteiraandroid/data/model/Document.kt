package com.fabiorosestolato.carteiraandroid.data.model

import android.graphics.Bitmap
import com.fabiorosestolato.carteiraandroid.domain.DocumentType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Modelo de dados para representar um documento salvo
 */
data class Document(
    val id: String,
    val documentType: DocumentType,
    val fileName: String,
    val fileSizeKB: Long,
    val createdAt: String,
    val imageBitmap: Bitmap? = null,
    val isEncrypted: Boolean = true,
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        private val dateFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        /**
         * Cria um novo documento com timestamp atual
         */
        fun create(
            documentType: DocumentType,
            fileName: String,
            fileSizeKB: Long,
            imageBitmap: Bitmap? = null,
            metadata: Map<String, String> = emptyMap()
        ): Document {
            val timestamp = System.currentTimeMillis()
            val id = "${documentType.filePrefix}_$timestamp"
            val createdAt = dateFormatter.format(Date(timestamp))
            
            return Document(
                id = id,
                documentType = documentType,
                fileName = fileName,
                fileSizeKB = fileSizeKB,
                createdAt = createdAt,
                imageBitmap = imageBitmap,
                isEncrypted = true,
                metadata = metadata
            )
        }
        
        /**
         * Cria um documento a partir de dados existentes
         */
        fun fromExisting(
            id: String,
            documentType: DocumentType,
            fileName: String,
            fileSizeKB: Long,
            createdTimestamp: Long,
            imageBitmap: Bitmap? = null,
            metadata: Map<String, String> = emptyMap()
        ): Document {
            val createdAt = dateFormatter.format(Date(createdTimestamp))
            
            return Document(
                id = id,
                documentType = documentType,
                fileName = fileName,
                fileSizeKB = fileSizeKB,
                createdAt = createdAt,
                imageBitmap = imageBitmap,
                isEncrypted = true,
                metadata = metadata
            )
        }
    }
    
    /**
     * Retorna o tamanho formatado do arquivo
     */
    fun getFormattedSize(): String {
        return when {
            fileSizeKB < 1024 -> "${fileSizeKB} KB"
            else -> {
                val sizeMB = fileSizeKB / 1024.0
                String.format("%.1f MB", sizeMB)
            }
        }
    }
    
    /**
     * Verifica se o documento tem uma imagem válida
     */
    fun hasValidImage(): Boolean {
        return imageBitmap != null && !imageBitmap.isRecycled
    }
    
    /**
     * Retorna informações de debug (sem dados sensíveis)
     */
    fun getDebugInfo(): String {
        return "Document(id=$id, type=${documentType.displayName}, size=${getFormattedSize()}, created=$createdAt)"
    }
}

/**
 * Extensões para facilitar o trabalho com listas de documentos
 */
fun List<Document>.filterByType(documentType: DocumentType): List<Document> {
    return filter { it.documentType == documentType }
}

fun List<Document>.getTotalSize(): Long {
    return sumOf { it.fileSizeKB }
}

fun List<Document>.getLatest(): Document? {
    return maxByOrNull { it.id.substringAfterLast("_").toLongOrNull() ?: 0L }
}

fun List<Document>.groupByType(): Map<DocumentType, List<Document>> {
    return groupBy { it.documentType }
}