package com.fabiorosestolato.carteiraandroid.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.fabiorosestolato.carteiraandroid.data.repository.DocumentRepository
import com.fabiorosestolato.carteiraandroid.data.repository.DocumentResult
import com.fabiorosestolato.carteiraandroid.domain.DocumentType
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Gerenciador de câmera integrado com o sistema de armazenamento seguro.
 * Facilita a captura e salvamento automático de documentos.
 */
class CameraManager(private val context: Context) {
    
    private val documentRepository = DocumentRepository(context)
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    
    private var imageCapture: ImageCapture? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    /**
     * Interface para callbacks de captura
     */
    interface CaptureCallback {
        fun onCaptureSuccess(fileName: String, documentType: DocumentType)
        fun onCaptureError(error: String)
    }
    
    /**
     * Inicializa a câmera
     * @param lifecycleOwner Owner do ciclo de vida
     * @param previewView View para preview da câmera
     */
    fun startCamera(lifecycleOwner: LifecycleOwner, previewView: androidx.camera.view.PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            
            // Preview
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            
            // ImageCapture
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
            
            // Seletor de câmera (traseira por padrão)
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                // Desvincula casos de uso antes de religar
                cameraProvider?.unbindAll()
                
                // Vincula casos de uso à câmera
                cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture
                )
            } catch (exc: Exception) {
                // Log do erro
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    /**
     * Captura uma foto e salva como documento criptografado
     * @param documentType Tipo do documento sendo capturado
     * @param callback Callback para resultado da operação
     */
    fun captureAndSaveDocument(documentType: DocumentType, callback: CaptureCallback) {
        val imageCapture = imageCapture ?: run {
            callback.onCaptureError("Câmera não inicializada")
            return
        }
        
        // Configurações de saída
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
            createTempFile()
        ).build()
        
        // Captura a imagem
        imageCapture.takePicture(
            outputFileOptions,
            ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Processa e salva a imagem capturada
                    processAndSaveImage(output, documentType, callback)
                }
                
                override fun onError(exception: ImageCaptureException) {
                    callback.onCaptureError("Erro na captura: ${exception.message}")
                }
            }
        )
    }
    
    /**
     * Processa a imagem capturada e salva de forma criptografada
     */
    private fun processAndSaveImage(
        output: ImageCapture.OutputFileResults,
        documentType: DocumentType,
        callback: CaptureCallback
    ) {
        try {
            val savedUri = output.savedUri ?: run {
                callback.onCaptureError("Erro ao salvar imagem temporária")
                return
            }
            
            // Lê a imagem e converte para ByteArray
            val inputStream = context.contentResolver.openInputStream(savedUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            // Comprime a imagem para reduzir tamanho
            val compressedImageData = compressBitmap(bitmap)
            
            // Salva de forma criptografada
            when (val result = documentRepository.saveDocument(documentType, compressedImageData)) {
                is DocumentResult.Success -> {
                    callback.onCaptureSuccess(result.fileName, documentType)
                }
                is DocumentResult.Error -> {
                    callback.onCaptureError(result.message)
                }
            }
            
            // Remove arquivo temporário
            context.contentResolver.delete(savedUri, null, null)
            
        } catch (e: Exception) {
            callback.onCaptureError("Erro ao processar imagem: ${e.message}")
        }
    }
    
    /**
     * Comprime bitmap para reduzir tamanho do arquivo
     */
    private fun compressBitmap(bitmap: Bitmap, quality: Int = 85): ByteArray {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
    
    /**
     * Cria arquivo temporário para captura
     */
    private fun createTempFile(): java.io.File {
        return java.io.File.createTempFile(
            "temp_capture_${System.currentTimeMillis()}",
            ".jpg",
            context.cacheDir
        )
    }
    
    /**
     * Para a câmera e libera recursos
     */
    fun stopCamera() {
        cameraProvider?.unbindAll()
        cameraExecutor.shutdown()
    }
    
    /**
     * Alterna entre câmera frontal e traseira
     */
    fun switchCamera(lifecycleOwner: LifecycleOwner, previewView: androidx.camera.view.PreviewView) {
        // Implementação para alternar câmeras
        // Por simplicidade, mantemos apenas a câmera traseira neste exemplo
    }
}