package com.fabiorosestolato.carteiraandroid.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fabiorosestolato.carteiraandroid.domain.DocumentType
import com.fabiorosestolato.carteiraandroid.ui.theme.*

/**
 * Card de documento inspirado no Google Wallet
 * @param documentType Tipo do documento
 * @param isDocumentSaved Se o documento já foi salvo
 * @param onClick Ação ao clicar no card
 */
@Composable
fun DocumentCard(
    documentType: DocumentType,
    isDocumentSaved: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    
    val elevation by animateDpAsState(
        targetValue = if (isPressed) 2.dp else 4.dp,
        label = "card_elevation"
    )
    
    val cardInfo = getDocumentCardInfo(documentType)
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (MaterialTheme.colorScheme.background == DarkBackground) CardShadowDark else CardShadow
            )
            .clickable {
                isPressed = !isPressed
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (MaterialTheme.colorScheme.background == DarkBackground) CardBackgroundDark else CardBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícone do documento
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = cardInfo.color.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = cardInfo.icon,
                    contentDescription = cardInfo.title,
                    tint = cardInfo.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Conteúdo do card
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = cardInfo.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (isDocumentSaved) "Documento salvo" else "Toque para adicionar",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isDocumentSaved) {
                        cardInfo.color
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            
            // Ícone de seta ou check
            Icon(
                imageVector = if (isDocumentSaved) Icons.Default.CheckCircle else Icons.Default.ArrowForward,
                contentDescription = if (isDocumentSaved) "Documento salvo" else "Adicionar documento",
                tint = if (isDocumentSaved) cardInfo.color else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Informações visuais para cada tipo de documento
 */
data class DocumentCardInfo(
    val title: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Obtém as informações visuais para cada tipo de documento
 */
fun getDocumentCardInfo(documentType: DocumentType): DocumentCardInfo {
    return when (documentType) {
        DocumentType.CNH -> DocumentCardInfo(
            title = "CNH",
            icon = Icons.Default.DirectionsCar,
            color = CNHColor
        )
        DocumentType.RG -> DocumentCardInfo(
            title = "RG",
            icon = Icons.Default.Person,
            color = RGColor
        )
        DocumentType.TITULO_ELEITOR -> DocumentCardInfo(
            title = "Título de Eleitor",
            icon = Icons.Default.HowToVote,
            color = TituloEleitorColor
        )
        DocumentType.PLANO_SAUDE -> DocumentCardInfo(
            title = "Plano de Saúde",
            icon = Icons.Default.LocalHospital,
            color = PlanoSaudeColor
        )
    }
}

/**
 * Preview do DocumentCard
 */
@Composable
fun DocumentCardPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DocumentCard(
                documentType = DocumentType.CNH,
                isDocumentSaved = false,
                onClick = { }
            )
            
            DocumentCard(
                documentType = DocumentType.RG,
                isDocumentSaved = true,
                onClick = { }
            )
            
            DocumentCard(
                documentType = DocumentType.TITULO_ELEITOR,
                isDocumentSaved = false,
                onClick = { }
            )
            
            DocumentCard(
                documentType = DocumentType.PLANO_SAUDE,
                isDocumentSaved = true,
                onClick = { }
            )
        }
    }
}