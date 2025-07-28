# CarteiraDigitalMVP - Backend Seguro

Backend Android para aplicativo de carteira digital que captura e armazena com segurança documentos como CNH, RG, plano de saúde e título de eleitor.

## 🔒 Características de Segurança

- **Criptografia AES-256-GCM**: Todos os documentos são criptografados usando Jetpack Security
- **Armazenamento Interno**: Arquivos salvos no sandbox da aplicação
- **FileProvider**: Compartilhamento seguro sem exposição de caminhos internos
- **Scoped Storage**: Compatível com Android 10+ e políticas da Play Store
- **Sem Permissões Desnecessárias**: Apenas câmera e armazenamento quando necessário

## 📁 Estrutura do Projeto

```
app/src/main/java/com/fabiorosestolato/carteiraandroid/
├── domain/
│   └── DocumentType.kt              # Enum dos tipos de documentos
├── data/
│   ├── local/
│   │   └── EncryptedFileManager.kt  # Gerenciamento de arquivos criptografados
│   ├── share/
│   │   └── FileShareManager.kt      # Compartilhamento seguro via FileProvider
│   └── repository/
│       └── DocumentRepository.kt    # Repositório principal
├── camera/
│   └── CameraManager.kt             # Integração câmera + armazenamento
└── res/xml/
    └── file_paths.xml               # Configuração FileProvider
```

## 🚀 Como Usar

### 1. Inicialização

```kotlin
val documentRepository = DocumentRepository(context)
val cameraManager = CameraManager(context)
```

### 2. Capturar e Salvar Documento

```kotlin
cameraManager.captureAndSaveDocument(
    documentType = DocumentType.CNH,
    callback = object : CameraManager.CaptureCallback {
        override fun onCaptureSuccess(fileName: String, documentType: DocumentType) {
            // Documento salvo com sucesso
            Log.d("CarteiraApp", "Documento salvo: $fileName")
        }
        
        override fun onCaptureError(error: String) {
            // Erro na captura
            Log.e("CarteiraApp", "Erro: $error")
        }
    }
)
```

### 3. Listar Documentos Salvos

```kotlin
val cnhDocuments = documentRepository.getDocumentsByType(DocumentType.CNH)
val rgDocuments = documentRepository.getDocumentsByType(DocumentType.RG)
```

### 4. Compartilhar Documento

```kotlin
val shareIntent = documentRepository.shareDocument(
    fileName = "cnh_1234567890.enc",
    shareTitle = "Compartilhar CNH"
)
shareIntent?.let { startActivity(it) }
```

### 5. Recuperar Dados do Documento

```kotlin
val imageData = documentRepository.getDocument("cnh_1234567890.enc")
imageData?.let { data ->
    val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
    // Usar bitmap conforme necessário
}
```

## 🛡️ Implementação de Segurança

### Criptografia
- **Algoritmo**: AES-256-GCM com HKDF
- **Chave Mestra**: Gerenciada pelo Android Keystore
- **Jetpack Security**: Biblioteca oficial do Android para criptografia

### Armazenamento
- **Local**: Diretório interno da aplicação (`context.filesDir`)
- **Sandbox**: Isolado de outras aplicações
- **Sem Backup**: Arquivos não incluídos em backups automáticos

### Compartilhamento
- **FileProvider**: URIs temporárias e seguras
- **Permissões Granulares**: Acesso apenas quando necessário
- **Limpeza Automática**: Arquivos temporários removidos automaticamente

## 📱 Compatibilidade

- **Android 6.0+** (API 23+)
- **Scoped Storage** compatível
- **Play Store** policy compliant
- **Jetpack Security** 1.1.0-alpha06

## 🔧 Dependências Principais

```kotlin
// Jetpack Security para criptografia
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// Camera X para captura
implementation("androidx.camera:camera-core:1.3.1")
implementation("androidx.camera:camera-camera2:1.3.1")
implementation("androidx.camera:camera-lifecycle:1.3.1")
implementation("androidx.camera:camera-view:1.3.1")

// Core Android
implementation("androidx.core:core-ktx:1.13.0")
implementation("androidx.documentfile:documentfile:1.0.1")
```

## 🔐 Permissões

```xml
<!-- Câmera para captura de documentos -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Armazenamento (apenas para Android 9 e inferior) -->
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
    android:maxSdkVersion="28" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

## 📊 Tipos de Documentos Suportados

- **CNH**: Carteira Nacional de Habilitação
- **RG**: Registro Geral
- **SAUDE**: Plano de Saúde
- **TITULO**: Título de Eleitor

## 🧹 Manutenção

### Limpeza de Arquivos Temporários
```kotlin
documentRepository.cleanupTempFiles()
```

### Estatísticas de Documentos
```kotlin
val stats = documentRepository.getDocumentStatistics()
stats.forEach { (type, count) ->
    Log.d("Stats", "${type.displayName}: $count documentos")
}
```

## ⚠️ Considerações Importantes

1. **Backup**: Documentos criptografados não são incluídos em backups
2. **Desinstalação**: Todos os dados são perdidos ao desinstalar o app
3. **Root**: Dispositivos com root podem comprometer a segurança
4. **Debugging**: Desabilitar logs em produção

## 🔄 Próximos Passos

1. Implementar interface de usuário
2. Adicionar autenticação biométrica
3. Implementar backup seguro na nuvem
4. Adicionar OCR para extração de dados
5. Implementar validação de documentos

## 📄 Licença

Este projeto segue as melhores práticas de segurança para aplicações Android e está em conformidade com as políticas da Google Play Store.