package com.fabiorosestolato.carteiraandroid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fabiorosestolato.carteiraandroid.camera.CameraManager
import com.fabiorosestolato.carteiraandroid.data.repository.DocumentRepository
import com.fabiorosestolato.carteiraandroid.domain.DocumentType
import com.fabiorosestolato.carteiraandroid.security.BiometricAuthManager
import com.fabiorosestolato.carteiraandroid.security.SecurityValidator
import com.fabiorosestolato.carteiraandroid.ui.components.LoadingState
import com.fabiorosestolato.carteiraandroid.ui.components.SecurityErrorState
import kotlinx.coroutines.delay

/**
 * Estados da tela de captura
 */
sealed class CaptureState {
    object Initial : CaptureState()
    object ValidatingSecurity : CaptureState()
    object SecurityFailed : CaptureState()
    object RequestingBiometric : CaptureState()
    object BiometricFailed : CaptureState()
    object OpeningCamera : CaptureState()
    object Capturing : CaptureState()
    object Processing : CaptureState()
    object Success : CaptureState()
    data class Error(val message: String) : CaptureState()
}

/**
 * Tela de captura de documentos com validação de segurança e autenticação biométrica
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureScreen(
    documentType: DocumentType,
    onNavigateBack: () -> Unit,
    onCaptureSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    
    val documentRepository = remember { DocumentRepository(context) }
    val securityValidator = remember { SecurityValidator(context) }
    val biometricAuthManager = remember { BiometricAuthManager(context) }
    val cameraManager = remember { CameraManager(context) }
    
    var captureState by remember { mutableStateOf<CaptureState>(CaptureState.Initial) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Função para iniciar o processo de captura
    val startCaptureProcess = {
        captureState = CaptureState.ValidatingSecurity
    }
    
    // Efeito para gerenciar o fluxo de captura
    LaunchedEffect(captureState) {
        when (captureState) {
            CaptureState.ValidatingSecurity -> {
                try {
                    val securityReport = securityValidator.validateDeviceSecurity()
                    if (securityReport.isSecure) {
                        captureState = CaptureState.RequestingBiometric
                    } else {
                        captureState = CaptureState.SecurityFailed
                        errorMessage = "Dispositivo não seguro: ${securityReport.threats.joinToString(", ")}"
                    }
                } catch (e: Exception) {
                    captureState = CaptureState.Error("Erro na validação de segurança: ${e.message}")
                }
            }
            
            CaptureState.RequestingBiometric -> {
                try {
                    if (biometricAuthManager.isBiometricAvailable()) {
                        biometricAuthManager.authenticate(
                            title = "Autenticação Necessária",
                            subtitle = "Confirme sua identidade para capturar ${documentType.displayName}",
                            onSuccess = {
                                captureState = CaptureState.OpeningCamera
                            },
                            onError = { error ->
                                captureState = CaptureState.BiometricFailed
                                errorMessage = "Falha na autenticação: $error"
                            }
                        )
                    } else {
                        // Se biometria não estiver disponível, prossegue direto
                        captureState = CaptureState.OpeningCamera
                    }
                } catch (e: Exception) {
                    captureState = CaptureState.Error("Erro na autenticação: ${e.message}")
                }
            }
            
            CaptureState.OpeningCamera -> {
                try {
                    captureState = CaptureState.Capturing
                    
                    // Simula abertura da câmera e captura
                    delay(1000) // Simula tempo de abertura da câmera
                    
                    cameraManager.captureAndSaveDocument(
                        documentType = documentType,
                        onSuccess = { savedDocument ->
                            captureState = CaptureState.Processing
                        },
                        onError = { error ->
                            captureState = CaptureState.Error("Erro na captura: $error")
                        }
                    )
                } catch (e: Exception) {
                    captureState = CaptureState.Error("Erro ao abrir câmera: ${e.message}")
                }
            }
            
            CaptureState.Processing -> {
                // Simula processamento da imagem
                delay(2000)
                captureState = CaptureState.Success
            }
            
            CaptureState.Success -> {
                delay(1500) // Mostra sucesso por um tempo
                onCaptureSuccess()
            }
            
            else -> { /* Outros estados não precisam de ação automática */ }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Capturar ${documentType.displayName}",
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
            when (captureState) {
                CaptureState.Initial -> {
                    CaptureInitialContent(
                        documentType = documentType,
                        onStartCapture = startCaptureProcess
                    )
                }
                
                CaptureState.ValidatingSecurity -> {
                    LoadingState(
                        message = "Validando segurança do dispositivo...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                CaptureState.SecurityFailed -> {
                    SecurityErrorState(
                        errorMessage = errorMessage ?: "Falha na validação de segurança",
                        onRetry = { captureState = CaptureState.Initial },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                CaptureState.RequestingBiometric -> {
                    LoadingState(
                        message = "Aguardando autenticação biométrica...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                CaptureState.BiometricFailed -> {
                    CaptureErrorContent(
                        title = "Falha na Autenticação",
                        message = errorMessage ?: "Não foi possível autenticar",
                        onRetry = { captureState = CaptureState.Initial },
                        onCancel = onNavigateBack
                    )
                }
                
                CaptureState.OpeningCamera -> {
                    LoadingState(
                        message = "Abrindo câmera...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                CaptureState.Capturing -> {
                    CameraCaptureContent(
                        documentType = documentType
                    )
                }
                
                CaptureState.Processing -> {
                    LoadingState(
                        message = "Processando imagem...",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                
                CaptureState.Success -> {
                    CaptureSuccessContent(
                        documentType = documentType
                    )
                }
                
                is CaptureState.Error -> {
                    CaptureErrorContent(
                        title = "Erro na Captura",
                        message = captureState.message,
                        onRetry = { captureState = CaptureState.Initial },
                        onCancel = onNavigateBack
                    )
                }
            }
        }
    }
}

/**
 * Conteúdo inicial da tela de captura
 */
@Composable
fun CaptureInitialContent(
    documentType: DocumentType,
    onStartCapture: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícone do documento
        Icon(
            imageVector = documentType.icon,
            contentDescription = documentType.displayName,
            modifier = Modifier.size(120.dp),
            tint = documentType.color
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Capturar ${documentType.displayName}",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Posicione o documento dentro da área de captura e mantenha-o bem iluminado.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Área de captura simulada
        Box(
            modifier = Modifier
                .size(width = 280.dp, height = 180.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(12.dp)
                )
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Área de Captura\n(3:4)",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onStartCapture,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Iniciar Captura",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

/**
 * Conteúdo da captura da câmera
 */
@Composable
fun CameraCaptureContent(
    documentType: DocumentType,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Simula a visualização da câmera
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            // Máscara de captura
            Box(
                modifier = Modifier
                    .size(width = 300.dp, height = 400.dp)
                    .border(
                        width = 3.dp,
                        color = Color.White,
                        shape = RoundedCornerShape(16.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Posicione o ${documentType.displayName}\ndentro desta área",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Instruções na parte inferior
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Capturando automaticamente...",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                modifier = Modifier.width(200.dp),
                color = Color.White
            )
        }
    }
}

/**
 * Conteúdo de sucesso na captura
 */
@Composable
fun CaptureSuccessContent(
    documentType: DocumentType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Sucesso",
            modifier = Modifier.size(120.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Documento Capturado!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "${documentType.displayName} foi salvo com sucesso na sua carteira digital.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Conteúdo de erro na captura
 */
@Composable
fun CaptureErrorContent(
    title: String,
    message: String,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
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
            text = title,
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
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            ) {
                Text("Cancelar")
            }
            
            Button(
                onClick = onRetry,
                modifier = Modifier.weight(1f)
            ) {
                Text("Tentar Novamente")
            }
        }
    }
}