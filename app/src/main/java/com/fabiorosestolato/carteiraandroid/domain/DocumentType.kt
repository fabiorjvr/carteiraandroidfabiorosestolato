package com.fabiorosestolato.carteiraandroid.domain

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.fabiorosestolato.carteiraandroid.ui.theme.*

/**
 * Enum que define os tipos de documentos suportados pelo aplicativo
 * CarteiraDigitalMVP para armazenamento seguro.
 */
enum class DocumentType(val displayName: String, val filePrefix: String) {
    CNH("Carteira Nacional de Habilitação", "cnh"),
    RG("Registro Geral", "rg"),
    SAUDE("Plano de Saúde", "saude"),
    TITULO("Título de Eleitor", "titulo");
    
    /**
     * Retorna o nome do arquivo com timestamp para evitar conflitos
     */
    fun generateFileName(): String {
        val timestamp = System.currentTimeMillis()
        return "${filePrefix}_${timestamp}.jpg"
    }
    
    /**
     * Retorna o nome do arquivo criptografado
     */
    fun generateEncryptedFileName(): String {
        val timestamp = System.currentTimeMillis()
        return "${filePrefix}_${timestamp}.enc"
    }
}

/**
 * Extensões para UI do DocumentType
 */
val DocumentType.icon: ImageVector
    get() = when (this) {
        DocumentType.CNH -> Icons.Default.DriveEta
        DocumentType.RG -> Icons.Default.Badge
        DocumentType.SAUDE -> Icons.Default.LocalHospital
        DocumentType.TITULO -> Icons.Default.HowToVote
    }

val DocumentType.color: Color
    @Composable
    get() = when (this) {
        DocumentType.CNH -> CNHColor
        DocumentType.RG -> RGColor
        DocumentType.SAUDE -> PlanoSaudeColor
        DocumentType.TITULO -> TituloEleitorColor
    }

val DocumentType.description: String
    get() = when (this) {
        DocumentType.CNH -> "Documento de habilitação para conduzir veículos"
        DocumentType.RG -> "Documento de identificação civil"
        DocumentType.SAUDE -> "Cartão do plano de saúde"
        DocumentType.TITULO -> "Documento eleitoral para votação"
    }