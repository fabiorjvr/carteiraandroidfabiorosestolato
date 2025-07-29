package com.fabiorosestolato.carteiraandroid.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabiorosestolato.carteiraandroid.data.repository.DocumentRepository
import com.fabiorosestolato.carteiraandroid.domain.DocumentType
import com.fabiorosestolato.carteiraandroid.security.SecurityValidator
import com.fabiorosestolato.carteiraandroid.ui.components.*

/**
 * Tela principal do aplicativo com os cards dos documentos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onDocumentClick: (DocumentType) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val documentRepository = remember { DocumentRepository(context) }
    val securityValidator = remember { SecurityValidator(context) }
    
    var documentStates by remember { mutableStateOf(mapOf<DocumentType, Boolean>()) }
    var securityReport by remember { mutableStateOf<SecurityValidator.SecurityReport?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Carrega o estado dos documentos e validação de segurança
    LaunchedEffect(Unit) {
        try {
            // Validação de segurança
            securityReport = securityValidator.validateDeviceSecurity()
            
            // Carrega estado dos documentos
            val states = DocumentType.values().associateWith { type ->
                documentRepository.getDocumentsByType(type).isNotEmpty()
            }
            documentStates = states
        } finally {
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Carteira Digital",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Medium
                    )
                },
                actions = {
                    // Indicador de segurança
                    securityReport?.let { report ->
                        IconButton(
                            onClick = { /* TODO: Mostrar detalhes de segurança */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = "Status de segurança",
                                tint = if (report.isSecure) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            DeveloperCredit()
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
                        message = "Carregando documentos...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                securityReport?.isSecure == false -> {
                    SecurityErrorState(
                        errorMessage = "Dispositivo não seguro detectado. Algumas funcionalidades podem estar limitadas.",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                documentStates.values.none { it } -> {
                    // Estado vazio - nenhum documento salvo
                    Column {
                        DocumentGrid(
                            documentStates = documentStates,
                            onDocumentClick = onDocumentClick,
                            modifier = Modifier.weight(1f)
                        )
                        
                        EmptyState(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
                
                else -> {
                    // Exibe os cards dos documentos
                    DocumentGrid(
                        documentStates = documentStates,
                        onDocumentClick = onDocumentClick,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}

/**
 * Grid com os cards dos documentos
 */
@Composable
fun DocumentGrid(
    documentStates: Map<DocumentType, Boolean>,
    onDocumentClick: (DocumentType) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(DocumentType.values().toList()) { documentType ->
            DocumentCard(
                documentType = documentType,
                isDocumentSaved = documentStates[documentType] ?: false,
                onClick = { onDocumentClick(documentType) }
            )
        }
    }
}

/**
 * Estatísticas dos documentos (componente adicional)
 */
@Composable
fun DocumentStats(
    documentStates: Map<DocumentType, Boolean>,
    modifier: Modifier = Modifier
) {
    val totalDocuments = DocumentType.values().size
    val savedDocuments = documentStates.values.count { it }
    val progress = if (totalDocuments > 0) savedDocuments.toFloat() / totalDocuments else 0f
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Progresso",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Text(
                    text = "$savedDocuments/$totalDocuments",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
        }
    }
}