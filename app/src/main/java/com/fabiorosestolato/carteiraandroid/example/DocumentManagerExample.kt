package com.fabiorosestolato.carteiraandroid.example

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.lifecycle.LifecycleOwner
import com.fabiorosestolato.carteiraandroid.camera.CameraManager
import com.fabiorosestolato.carteiraandroid.data.repository.DocumentRepository
import com.fabiorosestolato.carteiraandroid.data.repository.DocumentResult
import com.fabiorosestolato.carteiraandroid.domain.DocumentType

/**
 * Exemplo prático de como usar o sistema de gerenciamento de documentos.
 * Esta classe demonstra a integração completa entre captura, armazenamento e compartilhamento.
 */
class DocumentManagerExample(private val context: Context) {
    
    private val documentRepository = DocumentRepository(context)
    private val cameraManager = CameraManager(context)
    
    /**
     * Interface para callbacks das operações
     */
    interface DocumentOperationCallback {
        fun onSuccess(message: String)
        fun onError(error: String)
        fun onDocumentSaved(fileName: String, documentType: DocumentType)
        fun onDocumentsList(documents: Map<DocumentType, List<String>>)
    }
    
    /**
     * Inicia o processo de captura de um documento
     * @param documentType Tipo do documento a ser capturado
     * @param lifecycleOwner Owner do ciclo de vida
     * @param previewView View para preview da câmera
     * @param callback Callback para resultados
     */
    fun startDocumentCapture(
        documentType: DocumentType,
        lifecycleOwner: LifecycleOwner,
        previewView: androidx.camera.view.PreviewView,
        callback: DocumentOperationCallback
    ) {
        // Inicializa a câmera
        cameraManager.startCamera(lifecycleOwner, previewView)
        
        // Configura callback para captura
        val captureCallback = object : CameraManager.CaptureCallback {
            override fun onCaptureSuccess(fileName: String, documentType: DocumentType) {
                callback.onDocumentSaved(fileName, documentType)
                callback.onSuccess("${documentType.displayName} salvo com sucesso!")
            }
            
            override fun onCaptureError(error: String) {
                callback.onError("Erro ao capturar ${documentType.displayName}: $error")
            }
        }
        
        // Captura o documento
        cameraManager.captureAndSaveDocument(documentType, captureCallback)
    }
    
    /**
     * Lista todos os documentos salvos organizados por tipo
     * @param callback Callback com a lista de documentos
     */
    fun listAllDocuments(callback: DocumentOperationCallback) {
        try {
            val documentsMap = mutableMapOf<DocumentType, List<String>>()
            
            DocumentType.values().forEach { type ->
                val documents = documentRepository.getDocumentsByType(type)
                documentsMap[type] = documents
            }
            
            callback.onDocumentsList(documentsMap)
        } catch (e: Exception) {
            callback.onError("Erro ao listar documentos: ${e.message}")
        }
    }
    
    /**
     * Compartilha um documento específico
     * @param fileName Nome do arquivo a ser compartilhado
     * @param documentType Tipo do documento
     * @param callback Callback para resultado
     * @return Intent para compartilhamento ou null
     */
    fun shareDocument(
        fileName: String,
        documentType: DocumentType,
        callback: DocumentOperationCallback
    ): Intent? {
        return try {
            if (!documentRepository.canShareDocument(fileName)) {
                callback.onError("Documento não pode ser compartilhado")
                return null
            }
            
            val shareIntent = documentRepository.shareDocument(
                fileName,
                "Compartilhar ${documentType.displayName}"
            )
            
            if (shareIntent != null) {
                callback.onSuccess("${documentType.displayName} pronto para compartilhamento")
            } else {
                callback.onError("Erro ao preparar compartilhamento")
            }
            
            shareIntent
        } catch (e: Exception) {
            callback.onError("Erro no compartilhamento: ${e.message}")
            null
        }
    }
    
    /**
     * Remove um documento específico
     * @param fileName Nome do arquivo a ser removido
     * @param documentType Tipo do documento
     * @param callback Callback para resultado
     */
    fun deleteDocument(
        fileName: String,
        documentType: DocumentType,
        callback: DocumentOperationCallback
    ) {
        try {
            val success = documentRepository.deleteDocument(fileName)
            if (success) {
                callback.onSuccess("${documentType.displayName} removido com sucesso")
            } else {
                callback.onError("Falha ao remover ${documentType.displayName}")
            }
        } catch (e: Exception) {
            callback.onError("Erro ao remover documento: ${e.message}")
        }
    }
    
    /**
     * Obtém uma imagem de documento como Bitmap
     * @param fileName Nome do arquivo
     * @return Bitmap da imagem ou null se não encontrada
     */
    fun getDocumentImage(fileName: String): Bitmap? {
        return try {
            val imageData = documentRepository.getDocument(fileName)
            imageData?.let { data ->
                BitmapFactory.decodeByteArray(data, 0, data.size)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Verifica se todos os tipos de documentos obrigatórios foram salvos
     * @param callback Callback com resultado da verificação
     */
    fun checkDocumentCompleteness(callback: DocumentOperationCallback) {
        try {
            val stats = documentRepository.getDocumentStatistics()
            val missingDocuments = mutableListOf<DocumentType>()
            
            DocumentType.values().forEach { type ->
                if (stats[type] == 0) {
                    missingDocuments.add(type)
                }
            }
            
            if (missingDocuments.isEmpty()) {
                callback.onSuccess("Todos os documentos foram salvos!")
            } else {
                val missingNames = missingDocuments.joinToString(", ") { it.displayName }
                callback.onError("Documentos faltantes: $missingNames")
            }
        } catch (e: Exception) {
            callback.onError("Erro ao verificar documentos: ${e.message}")
        }
    }
    
    /**
     * Obtém estatísticas detalhadas dos documentos
     * @return Mapa com estatísticas por tipo
     */
    fun getDetailedStatistics(): Map<String, Any> {
        return try {
            val stats = documentRepository.getDocumentStatistics()
            val totalDocuments = stats.values.sum()
            
            mapOf(
                "total_documents" to totalDocuments,
                "documents_by_type" to stats.mapKeys { it.key.displayName },
                "completion_percentage" to if (DocumentType.values().isNotEmpty()) {
                    (stats.values.count { it > 0 } * 100) / DocumentType.values().size
                } else 0,
                "has_all_types" to documentRepository.hasAllDocumentTypes()
            )
        } catch (e: Exception) {
            mapOf("error" to e.message.orEmpty())
        }
    }
    
    /**
     * Realiza limpeza de arquivos temporários
     * @param callback Callback para resultado
     */
    fun performMaintenance(callback: DocumentOperationCallback) {
        try {
            documentRepository.cleanupTempFiles()
            callback.onSuccess("Limpeza realizada com sucesso")
        } catch (e: Exception) {
            callback.onError("Erro na limpeza: ${e.message}")
        }
    }
    
    /**
     * Para a câmera e libera recursos
     */
    fun cleanup() {
        cameraManager.stopCamera()
        documentRepository.cleanupTempFiles()
    }
}