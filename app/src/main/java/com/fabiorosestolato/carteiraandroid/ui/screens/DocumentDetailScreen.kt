package com.fabiorosestolato.carteiraandroid.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fabiorosestolato.carteiraandroid.data.model.Document
import com.fabiorosestolato.carteiraandroid.data.repository.DocumentRepository
import com.fabiorosestolato.carteiraandroid.domain.DocumentType
import com.fabiorosestolato.carteiraandroid.security.BiometricAuthManager
import com.fabiorosestolato.carteiraandroid.ui.components.LoadingState
import kotlinx.coroutines.launch

/**
 * Tela de detalhes do documento
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    documentType: DocumentType,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    val documentRepository = remember { DocumentRepository(context) }
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    
    var document by remember { mutableStateOf<Document?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDocumentNumber by remember { mutableStateOf(false) }
    var documentNumber by remember { mutableStateOf("") }
    var showNumberDialog by remember { mutableStateOf(false) }
    
    // Carrega o documento
    LaunchedEffect(documentType) {
        try {
            val documents = documentRepository.getDocumentsByType(documentType)
            document = documents.firstOrNull()
            if (document == null) {
                errorMessage = "Documento não encontrado"
            }
        } catch (e: Exception) {
            errorMessage = "Erro ao carregar documento: ${e.message}"
        } finally {
            isLoading = false
        }
    }
    
    // Função para compartilhar documento
    val shareDocument = {
        document?.let { doc ->
            scope.launch {
                try {
                    if (biometricAuthManager.isBiometricAvailable()) {
                        biometricAuthManager.authenticate(
                            title = "Confirmar Compartilhamento",
                            subtitle = "Confirme sua identidade para compartilhar o documento",
                            onSuccess = {
                                scope.launch {
                                    try {
                                        documentRepository.shareDocument(doc.id)
                                        // TODO: Implementar compartilhamento via FileProvider
                                    } catch (e: Exception) {
                                        errorMessage = "Erro ao compartilhar: ${e.message}"
                                    }
                                }
                            },
                            onError = { error ->
                                errorMessage = "Falha na autenticação: $error"
                            }
                        )
                    } else {
                        // Compartilha sem autenticação se biometria não estiver disponível
                        documentRepository.shareDocument(doc.id)
                    }
                } catch (e: Exception) {
                    errorMessage = "Erro ao compartilhar documento: ${e.message}"
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = documentType.displayName,
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    // Menu de opções
                    var showMenu by remember { mutableStateOf(false) }
                    
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Mais opções"
                        )
                    }
                    
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Anotar número") },
                            onClick = {
                                showMenu = false
                                showNumberDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null
                                )
                            }
                        )
                        
                        DropdownMenuItem(
                            text = { Text("Compartilhar") },
                            onClick = {
                                showMenu = false
                                shareDocument()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Share,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    LoadingState(
                        message = "Carregando documento...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                errorMessage != null -> {
                    DocumentErrorContent(
                        message = errorMessage!!,
                        onRetry = {
                            isLoading = true
                            errorMessage = null
                            scope.launch {
                                try {
                                    val documents = documentRepository.getDocumentsByType(documentType)
                                    document = documents.firstOrNull()
                                    if (document == null) {
                                        errorMessage = "Documento não encontrado"
                                    }
                                } catch (e: Exception) {
                                    errorMessage = "Erro ao carregar documento: ${e.message}"
                                } finally {
                                    isLoading = false
                                }
                            }
                        },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                document != null -> {
                    DocumentDetailContent(
                        document = document!!,
                        documentType = documentType,
                        showDocumentNumber = showDocumentNumber,
                        documentNumber = documentNumber,
                        onToggleNumberVisibility = { showDocumentNumber = !showDocumentNumber },
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                    )
                }
            }
        }
    }
    
    // Dialog para anotar número do documento
    if (showNumberDialog) {
        DocumentNumberDialog(
            currentNumber = documentNumber,
            onNumberChanged = { documentNumber = it },
            onConfirm = {
                showNumberDialog = false
                showDocumentNumber = true
            },
            onDismiss = { showNumberDialog = false }
        )
    }
}

/**
 * Conteúdo principal dos detalhes do documento
 */
@Composable
fun DocumentDetailContent(
    document: Document,
    documentType: DocumentType,
    showDocumentNumber: Boolean,
    documentNumber: String,
    onToggleNumberVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Card com a imagem do documento
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = documentType.icon,
                            contentDescription = null,
                            tint = documentType.color,
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = documentType.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            
                            Text(
                                text = "Capturado em ${document.createdAt}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Icon(
                        imageVector = Icons.Default.Verified,
                        contentDescription = "Verificado",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Imagem do documento
                document.imageBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Imagem do ${documentType.displayName}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } ?: run {
                    // Placeholder se não houver imagem
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 4f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.outline
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "Imagem não disponível",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
        
        // Card com informações do documento
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Informações",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Tamanho do arquivo
                DocumentInfoRow(
                    label = "Tamanho",
                    value = "${document.fileSizeKB} KB"
                )
                
                // Formato
                DocumentInfoRow(
                    label = "Formato",
                    value = "JPEG"
                )
                
                // Data de criação
                DocumentInfoRow(
                    label = "Data de captura",
                    value = document.createdAt
                )
                
                // Número do documento (se anotado)
                if (documentNumber.isNotEmpty()) {
                    DocumentInfoRow(
                        label = "Número",
                        value = if (showDocumentNumber) documentNumber else "••••••••",
                        isClickable = true,
                        onClick = onToggleNumberVisibility,
                        trailingIcon = if (showDocumentNumber) Icons.Default.VisibilityOff else Icons.Default.Visibility
                    )
                }
            }
        }
        
        // Card com ações rápidas
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Ações Rápidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Botão para anotar número
                    OutlinedButton(
                        onClick = { /* Implementar */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Anotar")
                    }
                    
                    // Botão para compartilhar
                    Button(
                        onClick = { /* Implementar */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Compartilhar")
                    }
                }
            }
        }
    }
}

/**
 * Linha de informação do documento
 */
@Composable
fun DocumentInfoRow(
    label: String,
    value: String,
    isClickable: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            
            if (isClickable && trailingIcon != null && onClick != null) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

/**
 * Dialog para anotar número do documento
 */
@Composable
fun DocumentNumberDialog(
    currentNumber: String,
    onNumberChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Anotar Número do Documento")
        },
        text = {
            Column {
                Text(
                    text = "Digite o número do documento para facilitar a identificação:",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = currentNumber,
                    onValueChange = onNumberChanged,
                    label = { Text("Número do documento") },
                    placeholder = { Text("Ex: 123456789") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = currentNumber.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

/**
 * Conteúdo de erro no carregamento do documento
 */
@Composable
fun DocumentErrorContent(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Error,
            contentDescription = "Erro",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Erro ao Carregar",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Tentar Novamente")
        }
    }
}