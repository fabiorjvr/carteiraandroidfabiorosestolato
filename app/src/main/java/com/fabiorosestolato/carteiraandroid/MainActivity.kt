package com.fabiorosestolato.carteiraandroid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController
import com.fabiorosestolato.carteiraandroid.navigation.AppNavigation
import com.fabiorosestolato.carteiraandroid.security.SecureLogger
import com.fabiorosestolato.carteiraandroid.security.SecurityValidator
import com.fabiorosestolato.carteiraandroid.security.initSecureLogger
import com.fabiorosestolato.carteiraandroid.ui.theme.CarteiraAndroidTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Inicializa o sistema de logging seguro
        initSecureLogger()
        
        // Valida a segurança do dispositivo
        val validator = SecurityValidator(this)
        val report = validator.validateDeviceSecurity()
        if (!report.isSecure) {
            SecureLogger.logSecurity("insecure_device", mapOf("threats" to report.threats.size))
        }
        
        enableEdgeToEdge()
        setContent {
            CarteiraAndroidTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CarteiraDigitalApp()
                }
            }
        }
    }
}

/**
 * Componente principal do aplicativo Carteira Digital
 */
@Composable
fun CarteiraDigitalApp() {
    val navController = rememberNavController()
    
    AppNavigation(
        navController = navController,
        modifier = Modifier.fillMaxSize()
    )
}

@Preview(showBackground = true)
@Composable
fun CarteiraDigitalAppPreview() {
    CarteiraAndroidTheme {
        CarteiraDigitalApp()
    }
}