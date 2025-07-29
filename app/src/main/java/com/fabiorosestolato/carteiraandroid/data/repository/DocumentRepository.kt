package com.fabiorosestolato.carteiraandroid.data.repository

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.fragment.app.FragmentActivity
import com.fabiorosestolato.carteiraandroid.data.local.EncryptedFileManager
import com.fabiorosestolato.carteiraandroid.data.model.Document
import com.fabiorosestolato.carteiraandroid.data.share.FileShareManager
import com.fabiorosestolato.carteiraandroid.domain.DocumentType
import com.fabiorosestolato.carteiraandroid.security.BiometricAuthManager
import java.io.File

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
    
    /**
     * Acessa um documento com autenticação biométrica
     * @param activity Activity que hospeda a autenticação
     * @param fileName Nome do arquivo
     * @param onSuccess Callback executado com os dados do documento após autenticação
     */
    fun accessDocumentWithAuth(activity: FragmentActivity, fileName: String, onSuccess: (ByteArray) -> Unit) {
        BiometricAuthManager(activity).authenticateBeforeAction(
            activity, "acessar documento"
        ) { 
            val documentData = getDocument(fileName)
            if (documentData != null) {
                onSuccess(documentData)
            }
        }
    }
    
    /**
     * Retorna uma lista de objetos Document para um tipo específico
     * @param documentType Tipo do documento
     * @return Lista de documentos
     */
    fun getDocumentsByType(documentType: DocumentType): List<Document> {
        val files = encryptedFileManager.listFiles(documentType.filePrefix)
        return files.mapNotNull { fileName ->
            try {
                val file = File(encryptedFileManager.getFilePath(fileName))
                if (file.exists()) {
                    val fileSizeKB = file.length() / 1024
                    val timestamp = extractTimestampFromFileName(fileName)
                    val imageBitmap = loadDocumentImage(fileName)
                    
                    Document.fromExisting(
                        id = fileName.substringBeforeLast("."),
                        documentType = documentType,
                        fileName = fileName,
                        fileSizeKB = fileSizeKB,
                        createdTimestamp = timestamp,
                        imageBitmap = imageBitmap
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Retorna um documento específico por ID
     * @param documentId ID do documento
     * @return Documento ou null se não encontrado
     */
    fun getDocumentById(documentId: String): Document? {
        return DocumentType.values().firstNotNullOfOrNull { type ->
            getDocumentsByType(type).find { it.id == documentId }
        }
    }
    
    /**
     * Carrega a imagem de um documento como Bitmap
     * @param fileName Nome do arquivo
     * @return Bitmap da imagem ou null se não conseguir carregar
     */
    private fun loadDocumentImage(fileName: String): Bitmap? {
        return try {
            val imageData = getDocument(fileName)
            imageData?.let { data ->
                BitmapFactory.decodeByteArray(data, 0, data.size)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Extrai o timestamp do nome do arquivo
     * @param fileName Nome do arquivo
     * @return Timestamp ou timestamp atual se não conseguir extrair
     */
    private fun extractTimestampFromFileName(fileName: String): Long {
        return try {
            val timestampStr = fileName.substringAfterLast("_").substringBeforeLast(".")
            timestampStr.toLongOrNull() ?: System.currentTimeMillis()
        } catch (e: Exception) {
            System.currentTimeMillis()
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