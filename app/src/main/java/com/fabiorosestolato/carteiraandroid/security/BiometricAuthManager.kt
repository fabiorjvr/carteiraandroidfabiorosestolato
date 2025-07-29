package com.fabiorosestolato.carteiraandroid.security

import android.content.Context
import android.content.SharedPreferences
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executor

/**
 * Gerenciador de autenticação biométrica que suporta impressão digital,
 * reconhecimento facial e PIN como fallback, com configurações personalizáveis.
 */
class BiometricAuthManager(private val context: Context) {
    
    companion object {
        private const val PREFS_NAME = "biometric_auth_prefs"
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled"
        private const val KEY_REQUIRE_AUTH_FOR_DOCUMENTS = "require_auth_for_documents"
        private const val KEY_REQUIRE_AUTH_FOR_SHARING = "require_auth_for_sharing"
        private const val AUTH_TIMEOUT_MS = 30000L // 30 segundos
    }
    
    private val sharedPreferences: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val executor: Executor = ContextCompat.getMainExecutor(context)
    
    /**
     * Enum representando o status da disponibilidade biométrica
     */
    enum class BiometricStatus {
        AVAILABLE,
        NOT_AVAILABLE,
        NOT_ENROLLED,
        HARDWARE_NOT_AVAILABLE,
        SECURITY_UPDATE_REQUIRED
    }
    
    /**
     * Interface para callbacks de autenticação
     */
    interface AuthenticationCallback {
        fun onSuccess()
        fun onError(errorMessage: String)
        fun onCancel()
    }
    
    /**
     * Verifica se a autenticação biométrica está disponível
     * @return Status da disponibilidade biométrica
     */
    fun isBiometricAvailable(): BiometricStatus {
        val biometricManager = BiometricManager.from(context)
        
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                SecureLogger.logSecurity("biometric_available")
                BiometricStatus.AVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                SecureLogger.logSecurity("biometric_no_hardware")
                BiometricStatus.HARDWARE_NOT_AVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                SecureLogger.logSecurity("biometric_hw_unavailable")
                BiometricStatus.NOT_AVAILABLE
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                SecureLogger.logSecurity("biometric_none_enrolled")
                BiometricStatus.NOT_ENROLLED
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                SecureLogger.logSecurity("biometric_security_update_required")
                BiometricStatus.SECURITY_UPDATE_REQUIRED
            }
            else -> {
                SecureLogger.logSecurity("biometric_unknown_status")
                BiometricStatus.NOT_AVAILABLE
            }
        }
    }
    
    /**
     * Realiza autenticação biométrica
     * @param activity Activity que hospeda a autenticação
     * @param title Título do prompt de autenticação
     * @param subtitle Subtítulo do prompt
     * @param description Descrição da autenticação
     * @param callback Callback para resultado da autenticação
     */
    fun authenticate(
        activity: FragmentActivity,
        title: String = "Autenticação Necessária",
        subtitle: String = "Use sua biometria para continuar",
        description: String = "Confirme sua identidade para acessar seus documentos",
        callback: AuthenticationCallback
    ) {
        if (!isBiometricEnabled()) {
            SecureLogger.logSecurity("biometric_auth_disabled")
            callback.onSuccess() // Pula autenticação se desabilitada
            return
        }
        
        val biometricStatus = isBiometricAvailable()
        if (biometricStatus != BiometricStatus.AVAILABLE) {
            val errorMessage = when (biometricStatus) {
                BiometricStatus.NOT_ENROLLED -> "Nenhuma biometria cadastrada no dispositivo"
                BiometricStatus.HARDWARE_NOT_AVAILABLE -> "Hardware biométrico não disponível"
                BiometricStatus.SECURITY_UPDATE_REQUIRED -> "Atualização de segurança necessária"
                else -> "Autenticação biométrica não disponível"
            }
            SecureLogger.logSecurity("biometric_auth_unavailable", mapOf("reason" to biometricStatus.name))
            callback.onError(errorMessage)
            return
        }
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                SecureLogger.logSecurity("biometric_auth_success")
                callback.onSuccess()
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> {
                        SecureLogger.logSecurity("biometric_auth_cancelled", mapOf("error_code" to errorCode))
                        callback.onCancel()
                    }
                    else -> {
                        SecureLogger.logSecurity("biometric_auth_error", mapOf("error_code" to errorCode, "error_message" to errString.toString()))
                        callback.onError("Erro na autenticação: $errString")
                    }
                }
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                SecureLogger.logSecurity("biometric_auth_failed")
                // Não chama callback aqui, permite nova tentativa
            }
        })
        
        // Implementa timeout
        activity.lifecycleScope.launch {
            delay(AUTH_TIMEOUT_MS)
            SecureLogger.logSecurity("biometric_auth_timeout")
        }
        
        try {
            biometricPrompt.authenticate(promptInfo)
            SecureLogger.logSecurity("biometric_auth_started")
        } catch (e: Exception) {
            SecureLogger.logSecurity("biometric_auth_exception", mapOf("error" to e.message.orEmpty()))
            callback.onError("Erro ao iniciar autenticação: ${e.message}")
        }
    }
    
    /**
     * Autentica antes de executar uma ação específica
     * @param activity Activity que hospeda a autenticação
     * @param actionDescription Descrição da ação que será executada
     * @param onSuccess Callback executado após autenticação bem-sucedida
     */
    fun authenticateBeforeAction(
        activity: FragmentActivity,
        actionDescription: String,
        onSuccess: () -> Unit
    ) {
        authenticate(
            activity = activity,
            title = "Confirmar Identidade",
            subtitle = "Autenticação necessária",
            description = "Confirme sua identidade para $actionDescription",
            callback = object : AuthenticationCallback {
                override fun onSuccess() {
                    SecureLogger.logSecurity("authenticated_action_approved", mapOf("action" to actionDescription))
                    onSuccess()
                }
                
                override fun onError(errorMessage: String) {
                    SecureLogger.logSecurity("authenticated_action_error", mapOf("action" to actionDescription, "error" to errorMessage))
                }
                
                override fun onCancel() {
                    SecureLogger.logSecurity("authenticated_action_cancelled", mapOf("action" to actionDescription))
                }
            }
        )
    }
    
    /**
     * Verifica se a autenticação biométrica está habilitada
     */
    fun isBiometricEnabled(): Boolean {
        return sharedPreferences.getBoolean(KEY_BIOMETRIC_ENABLED, true)
    }
    
    /**
     * Habilita ou desabilita a autenticação biométrica
     * @param enabled true para habilitar, false para desabilitar
     */
    fun setBiometricEnabled(enabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_BIOMETRIC_ENABLED, enabled)
            .apply()
        
        SecureLogger.logSecurity("biometric_setting_changed", mapOf("enabled" to enabled))
    }
    
    /**
     * Verifica se autenticação é obrigatória para acessar documentos
     */
    fun isAuthRequiredForDocuments(): Boolean {
        return sharedPreferences.getBoolean(KEY_REQUIRE_AUTH_FOR_DOCUMENTS, true)
    }
    
    /**
     * Define se autenticação é obrigatória para acessar documentos
     * @param required true se obrigatório, false caso contrário
     */
    fun setAuthRequiredForDocuments(required: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_REQUIRE_AUTH_FOR_DOCUMENTS, required)
            .apply()
        
        SecureLogger.logSecurity("auth_requirement_documents_changed", mapOf("required" to required))
    }
    
    /**
     * Verifica se autenticação é obrigatória para compartilhar documentos
     */
    fun isAuthRequiredForSharing(): Boolean {
        return sharedPreferences.getBoolean(KEY_REQUIRE_AUTH_FOR_SHARING, true)
    }
    
    /**
     * Define se autenticação é obrigatória para compartilhar documentos
     * @param required true se obrigatório, false caso contrário
     */
    fun setAuthRequiredForSharing(required: Boolean) {
        sharedPreferences.edit()
            .putBoolean(KEY_REQUIRE_AUTH_FOR_SHARING, required)
            .apply()
        
        SecureLogger.logSecurity("auth_requirement_sharing_changed", mapOf("required" to required))
    }
    
    /**
     * Obtém configurações atuais de autenticação
     * @return Mapa com as configurações
     */
    fun getAuthSettings(): Map<String, Boolean> {
        return mapOf(
            "biometric_enabled" to isBiometricEnabled(),
            "auth_required_documents" to isAuthRequiredForDocuments(),
            "auth_required_sharing" to isAuthRequiredForSharing(),
            "biometric_available" to (isBiometricAvailable() == BiometricStatus.AVAILABLE)
        )
    }
}

/**
 * Extension function para FragmentActivity que facilita a autenticação biométrica
 * @param title Título do prompt
 * @param description Descrição da autenticação
 * @param onSuccess Callback de sucesso
 * @param onError Callback de erro
 * @param onCancel Callback de cancelamento
 */
fun FragmentActivity.requireBiometricAuth(
    title: String = "Autenticação Necessária",
    description: String = "Confirme sua identidade para continuar",
    onSuccess: () -> Unit,
    onError: (String) -> Unit = {},
    onCancel: () -> Unit = {}
) {
    val biometricManager = BiometricAuthManager(this)
    
    biometricManager.authenticate(
        activity = this,
        title = title,
        description = description,
        callback = object : BiometricAuthManager.AuthenticationCallback {
            override fun onSuccess() = onSuccess()
            override fun onError(errorMessage: String) = onError(errorMessage)
            override fun onCancel() = onCancel()
        }
    )
}