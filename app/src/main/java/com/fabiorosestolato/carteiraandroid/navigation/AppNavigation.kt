package com.fabiorosestolato.carteiraandroid.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.fabiorosestolato.carteiraandroid.domain.DocumentType
import com.fabiorosestolato.carteiraandroid.ui.screens.CaptureScreen
import com.fabiorosestolato.carteiraandroid.ui.screens.DocumentDetailScreen
import com.fabiorosestolato.carteiraandroid.ui.screens.HomeScreen

/**
 * Rotas de navegação do aplicativo
 */
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Capture : Screen("capture/{documentType}") {
        fun createRoute(documentType: DocumentType): String {
            return "capture/${documentType.name}"
        }
    }
    object DocumentDetail : Screen("document_detail/{documentType}") {
        fun createRoute(documentType: DocumentType): String {
            return "document_detail/${documentType.name}"
        }
    }
}

/**
 * Componente principal de navegação do aplicativo
 */
@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        // Tela principal
        composable(Screen.Home.route) {
            HomeScreen(
                onDocumentClick = { documentType ->
                    navController.navigate(Screen.Capture.createRoute(documentType))
                }
            )
        }
        
        // Tela de captura
        composable(Screen.Capture.route) { backStackEntry ->
            val documentTypeString = backStackEntry.arguments?.getString("documentType")
            val documentType = documentTypeString?.let { 
                try {
                    DocumentType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    DocumentType.CNH // Fallback para CNH se o tipo for inválido
                }
            } ?: DocumentType.CNH
            
            CaptureScreen(
                documentType = documentType,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onCaptureSuccess = {
                    // Volta para a tela principal após captura bem-sucedida
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }
        
        // Tela de detalhes do documento
        composable(Screen.DocumentDetail.route) { backStackEntry ->
            val documentTypeString = backStackEntry.arguments?.getString("documentType")
            val documentType = documentTypeString?.let { 
                try {
                    DocumentType.valueOf(it)
                } catch (e: IllegalArgumentException) {
                    DocumentType.CNH // Fallback para CNH se o tipo for inválido
                }
            } ?: DocumentType.CNH
            
            DocumentDetailScreen(
                documentType = documentType,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}

/**
 * Extensões para facilitar a navegação
 */
fun NavHostController.navigateToCapture(documentType: DocumentType) {
    navigate(Screen.Capture.createRoute(documentType))
}

fun NavHostController.navigateToDocumentDetail(documentType: DocumentType) {
    navigate(Screen.DocumentDetail.createRoute(documentType))
}

fun NavHostController.navigateToHome() {
    navigate(Screen.Home.route) {
        popUpTo(Screen.Home.route) {
            inclusive = true
        }
    }
}