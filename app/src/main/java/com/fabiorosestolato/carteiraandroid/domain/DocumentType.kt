package com.fabiorosestolato.carteiraandroid.domain

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