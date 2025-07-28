package com.fabiorosestolato.carteiraandroid.data.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.fabiorosestolato.carteiraandroid.data.local.EncryptedFileManager
import java.io.File

/**
 * Classe responsável por gerenciar o compartilhamento seguro de arquivos
 * usando FileProvider para manter a compatibilidade com as políticas do Android.
 */
class FileShareManager(private val context: Context) {
    
    private val encryptedFileManager = EncryptedFileManager(context)
    
    companion object {
        private const val FILE_PROVIDER_AUTHORITY = "com.fabiorosestolato.carteiraandroid.fileprovider"
    }
    
    /**
     * Compartilha um arquivo criptografado de forma segura
     * @param encryptedFileName Nome do arquivo criptografado
     * @param shareTitle Título para o diálogo de compartilhamento
     * @return Intent para compartilhamento ou null em caso de erro
     */
    fun shareEncryptedFile(encryptedFileName: String, shareTitle: String = "Compartilhar Documento"): Intent? {
        return try {
            // Limpa arquivos temporários antigos
            encryptedFileManager.cleanTempFiles()
            
            // Cria arquivo temporário descriptografado
            val tempFile = encryptedFileManager.createTempDecryptedFile(encryptedFileName)
                ?: return null
            
            // Obtém URI usando FileProvider
            val fileUri = getFileUri(tempFile)
            
            // Cria intent de compartilhamento
            createShareIntent(fileUri, shareTitle)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Obtém URI segura para um arquivo usando FileProvider
     * @param file Arquivo para obter URI
     * @return URI do arquivo
     */
    private fun getFileUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            file
        )
    }
    
    /**
     * Cria intent de compartilhamento com as permissões adequadas
     * @param fileUri URI do arquivo
     * @param shareTitle Título do compartilhamento
     * @return Intent configurado para compartilhamento
     */
    private fun createShareIntent(fileUri: Uri, shareTitle: String): Intent {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        return Intent.createChooser(shareIntent, shareTitle)
    }
    
    /**
     * Exporta um arquivo para um diretório específico (Downloads, por exemplo)
     * @param encryptedFileName Nome do arquivo criptografado
     * @param exportFileName Nome desejado para o arquivo exportado
     * @return true se exportado com sucesso, false caso contrário
     */
    fun exportToDownloads(encryptedFileName: String, exportFileName: String): Boolean {
        return try {
            val tempFile = encryptedFileManager.createTempDecryptedFile(encryptedFileName)
                ?: return false
            
            // Cria intent para salvar arquivo
            val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/jpeg"
                putExtra(Intent.EXTRA_TITLE, exportFileName)
            }
            
            // Nota: Este intent precisa ser iniciado por uma Activity
            // O resultado deve ser tratado em onActivityResult
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Verifica se um arquivo pode ser compartilhado
     * @param encryptedFileName Nome do arquivo criptografado
     * @return true se o arquivo existe e pode ser compartilhado
     */
    fun canShareFile(encryptedFileName: String): Boolean {
        return try {
            val file = File(context.filesDir, encryptedFileName)
            file.exists() && file.canRead()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Obtém o tamanho de um arquivo criptografado
     * @param encryptedFileName Nome do arquivo
     * @return Tamanho em bytes ou -1 em caso de erro
     */
    fun getFileSize(encryptedFileName: String): Long {
        return try {
            val file = File(context.filesDir, encryptedFileName)
            if (file.exists()) file.length() else -1L
        } catch (e: Exception) {
            -1L
        }
    }
    
    /**
     * Limpa todos os arquivos temporários do cache
     */
    fun cleanupTempFiles() {
        encryptedFileManager.cleanTempFiles()
    }
}