package com.fabiorosestolato.carteiraandroid.data.repository

import android.content.Context
import android.content.Intent
import com.fabiorosestolato.carteiraandroid.data.local.EncryptedFileManager
import com.fabiorosestolato.carteiraandroid.data.share.FileShareManager
import com.fabiorosestolato.carteiraandroid.domain.DocumentType

/**
 * Repositório principal para gerenciar documentos da carteira digital.
 * Centraliza as operações de armazenamento, recuperação e compartilhamento.
 */
class DocumentRepository(context: Context) {
    
    private val encryptedFileManager = EncryptedFileManager(context)
    private val fileShareManager = FileShareManager(context)
    
    /**
     * Salva um documento de forma segura
     * @param documentType Tipo do documento
     * @param imageData Dados da imagem em bytes
     * @return Resultado da operação
     */
    fun saveDocument(documentType: DocumentType, imageData: ByteArray): DocumentResult {
        return try {
            val fileName = encryptedFileManager.saveEncryptedFile(documentType, imageData)
            if (fileName != null) {
                DocumentResult.Success(fileName)
            } else {
                DocumentResult.Error("Falha ao salvar o documento")
            }
        } catch (e: Exception) {
            DocumentResult.Error("Erro inesperado: ${e.message}")
        }
    }
    
    /**
     * Recupera um documento salvo
     * @param fileName Nome do arquivo
     * @return Dados do documento ou null se não encontrado
     */
    fun getDocument(fileName: String): ByteArray? {
        return encryptedFileManager.readEncryptedFile(fileName)
    }
    
    /**
     * Lista todos os documentos de um tipo específico
     * @param documentType Tipo do documento
     * @return Lista de nomes de arquivos
     */
    fun getDocumentsByType(documentType: DocumentType): List<String> {
        return encryptedFileManager.listEncryptedFiles(documentType)
    }
    
    /**
     * Remove um documento
     * @param fileName Nome do arquivo
     * @return true se removido com sucesso
     */
    fun deleteDocument(fileName: String): Boolean {
        return encryptedFileManager.deleteEncryptedFile(fileName)
    }
    
    /**
     * Compartilha um documento de forma segura
     * @param fileName Nome do arquivo
     * @param shareTitle Título para o compartilhamento
     * @return Intent para compartilhamento ou null
     */
    fun shareDocument(fileName: String, shareTitle: String = "Compartilhar Documento"): Intent? {
        return fileShareManager.shareEncryptedFile(fileName, shareTitle)
    }
    
    /**
     * Verifica se um documento pode ser compartilhado
     * @param fileName Nome do arquivo
     * @return true se pode ser compartilhado
     */
    fun canShareDocument(fileName: String): Boolean {
        return fileShareManager.canShareFile(fileName)
    }
    
    /**
     * Obtém o tamanho de um documento
     * @param fileName Nome do arquivo
     * @return Tamanho em bytes
     */
    fun getDocumentSize(fileName: String): Long {
        return fileShareManager.getFileSize(fileName)
    }
    
    /**
     * Limpa arquivos temporários
     */
    fun cleanupTempFiles() {
        fileShareManager.cleanupTempFiles()
    }
    
    /**
     * Obtém estatísticas dos documentos salvos
     * @return Mapa com contagem por tipo de documento
     */
    fun getDocumentStatistics(): Map<DocumentType, Int> {
        return DocumentType.values().associateWith { type ->
            getDocumentsByType(type).size
        }
    }
    
    /**
     * Verifica se existe pelo menos um documento de cada tipo
     * @return true se todos os tipos têm pelo menos um documento
     */
    fun hasAllDocumentTypes(): Boolean {
        return DocumentType.values().all { type ->
            getDocumentsByType(type).isNotEmpty()
        }
    }
}

/**
 * Classe selada para representar o resultado das operações
 */
sealed class DocumentResult {
    data class Success(val fileName: String) : DocumentResult()
    data class Error(val message: String) : DocumentResult()
}