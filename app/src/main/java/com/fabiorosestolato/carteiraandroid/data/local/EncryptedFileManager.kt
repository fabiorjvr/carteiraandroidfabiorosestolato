package com.fabiorosestolato.carteiraandroid.data.local

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.fabiorosestolato.carteiraandroid.domain.DocumentType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Classe responsável por gerenciar o armazenamento seguro de arquivos
 * usando criptografia AES-256-GCM através do Jetpack Security.
 */
class EncryptedFileManager(private val context: Context) {
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    /**
     * Salva um arquivo de forma criptografada no armazenamento interno
     * @param documentType Tipo do documento
     * @param imageData Array de bytes da imagem
     * @return Nome do arquivo salvo ou null em caso de erro
     */
    fun saveEncryptedFile(documentType: DocumentType, imageData: ByteArray): String? {
        return try {
            val fileName = documentType.generateEncryptedFileName()
            val file = File(context.filesDir, fileName)
            
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            
            encryptedFile.openFileOutput().use { outputStream ->
                outputStream.write(imageData)
            }
            
            fileName
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Lê um arquivo criptografado do armazenamento interno
     * @param fileName Nome do arquivo a ser lido
     * @return Array de bytes do arquivo ou null em caso de erro
     */
    fun readEncryptedFile(fileName: String): ByteArray? {
        return try {
            val file = File(context.filesDir, fileName)
            if (!file.exists()) return null
            
            val encryptedFile = EncryptedFile.Builder(
                context,
                file,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()
            
            encryptedFile.openFileInput().use { inputStream ->
                inputStream.readBytes()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Lista todos os arquivos criptografados de um tipo específico
     * @param documentType Tipo do documento
     * @return Lista de nomes de arquivos
     */
    fun listEncryptedFiles(documentType: DocumentType): List<String> {
        return try {
            context.filesDir.listFiles()?.filter { file ->
                file.name.startsWith(documentType.filePrefix) && file.name.endsWith(".enc")
            }?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Remove um arquivo criptografado
     * @param fileName Nome do arquivo a ser removido
     * @return true se removido com sucesso, false caso contrário
     */
    fun deleteEncryptedFile(fileName: String): Boolean {
        return try {
            val file = File(context.filesDir, fileName)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Cria um arquivo temporário descriptografado para compartilhamento
     * @param fileName Nome do arquivo criptografado
     * @return File temporário ou null em caso de erro
     */
    fun createTempDecryptedFile(fileName: String): File? {
        return try {
            val encryptedData = readEncryptedFile(fileName) ?: return null
            val tempFileName = "temp_${System.currentTimeMillis()}.jpg"
            val tempFile = File(context.cacheDir, tempFileName)
            
            FileOutputStream(tempFile).use { outputStream ->
                outputStream.write(encryptedData)
            }
            
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Limpa arquivos temporários antigos do cache
     */
    fun cleanTempFiles() {
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.name.startsWith("temp_") && file.name.endsWith(".jpg")) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Lista todos os arquivos com um prefixo específico
     * @param prefix Prefixo do arquivo
     * @return Lista de nomes de arquivos
     */
    fun listFiles(prefix: String): List<String> {
        return try {
            context.filesDir.listFiles()?.filter { file ->
                file.name.startsWith(prefix) && file.name.endsWith(".enc")
            }?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Retorna o caminho completo de um arquivo
     * @param fileName Nome do arquivo
     * @return Caminho completo do arquivo
     */
    fun getFilePath(fileName: String): String {
        return File(context.filesDir, fileName).absolutePath
    }
}