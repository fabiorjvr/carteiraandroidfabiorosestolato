package com.fabiorosestolato.carteiraandroid.security

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.io.File

/**
 * Validador de segurança que detecta dispositivos comprometidos,
 * emuladores e aplicações perigosas para proteger dados sensíveis.
 */
class SecurityValidator(private val context: Context) {
    
    companion object {
        // Apps conhecidos de root
        private val ROOT_APPS = arrayOf(
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.koushikdutta.superuser",
            "com.noshufou.android.su",
            "com.thirdparty.superuser",
            "com.yellowes.su",
            "com.kingroot.kinguser",
            "com.kingo.root",
            "com.smedialink.oneclickroot",
            "com.zhiqupk.root.global",
            "com.alephzain.framaroot"
        )
        
        // Binários de root comuns
        private val ROOT_BINARIES = arrayOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/tmp/su",
            "/system/app/Superuser.apk",
            "/system/app/SuperSU.apk"
        )
        
        // Propriedades de emulador
        private val EMULATOR_PROPS = arrayOf(
            "ro.hardware" to arrayOf("goldfish", "ranchu", "vbox86"),
            "ro.product.device" to arrayOf("generic", "generic_x86", "generic_x86_64"),
            "ro.product.model" to arrayOf("sdk", "google_sdk", "Android SDK built for x86"),
            "ro.product.name" to arrayOf("sdk", "google_sdk", "generic"),
            "ro.product.board" to arrayOf("unknown", "goldfish")
        )
        
        // Apps perigosos conhecidos
        private val DANGEROUS_APPS = arrayOf(
            "com.chelpus.lackypatch",
            "com.dimonvideo.luckypatcher",
            "com.forpda.lp",
            "com.android.vending.billing.InAppBillingService.COIN",
            "uret.jasi2169.patcher",
            "zone.jasi2169.uretpatcher",
            "p.jasi2169.al",
            "com.blackmartalpha",
            "org.blackmart.market",
            "com.allinone.free",
            "com.repodroid.app",
            "org.creeplays.hack",
            "com.baseappfull.fwd",
            "com.zmapp",
            "com.dv.adm",
            "org.sbtools.gamehack",
            "com.zune.gamekiller",
            "cn.maocai.gamekiller",
            "com.gmd.speedtime",
            "org.droidplanner.patcher",
            "com.xmodgames"
        )
    }
    
    /**
     * Enum representando diferentes tipos de ameaças de segurança
     */
    enum class SecurityThreat {
        ROOT_DETECTED,
        DEBUG_ENABLED,
        EMULATOR_DETECTED,
        DANGEROUS_APP_INSTALLED,
        DEVELOPER_OPTIONS_ENABLED,
        ADB_ENABLED,
        UNKNOWN_SOURCES_ENABLED
    }
    
    /**
     * Data class representando o relatório de segurança
     */
    data class SecurityReport(
        val isSecure: Boolean,
        val threats: List<SecurityThreat>,
        val details: Map<SecurityThreat, String>
    )
    
    /**
     * Valida a segurança geral do dispositivo
     * @return Relatório completo de segurança
     */
    fun validateDeviceSecurity(): SecurityReport {
        SecureLogger.logSecurity("device_security_check_started")
        
        val threats = mutableListOf<SecurityThreat>()
        val details = mutableMapOf<SecurityThreat, String>()
        
        // Verifica root
        if (isDeviceRooted()) {
            threats.add(SecurityThreat.ROOT_DETECTED)
            details[SecurityThreat.ROOT_DETECTED] = "Device appears to be rooted"
            SecureLogger.logSecurity("root_detected")
        }
        
        // Verifica debug
        if (isDebuggingEnabled()) {
            threats.add(SecurityThreat.DEBUG_ENABLED)
            details[SecurityThreat.DEBUG_ENABLED] = "Debug mode is enabled"
            SecureLogger.logSecurity("debug_enabled")
        }
        
        // Verifica emulador
        if (isEmulator()) {
            threats.add(SecurityThreat.EMULATOR_DETECTED)
            details[SecurityThreat.EMULATOR_DETECTED] = "Running on emulator"
            SecureLogger.logSecurity("emulator_detected")
        }
        
        // Verifica apps perigosos
        val dangerousApps = getDangerousAppsInstalled()
        if (dangerousApps.isNotEmpty()) {
            threats.add(SecurityThreat.DANGEROUS_APP_INSTALLED)
            details[SecurityThreat.DANGEROUS_APP_INSTALLED] = "Dangerous apps: ${dangerousApps.joinToString(", ")}"
            SecureLogger.logSecurity("dangerous_apps_detected", mapOf("count" to dangerousApps.size))
        }
        
        // Verifica opções de desenvolvedor
        if (isDeveloperOptionsEnabled()) {
            threats.add(SecurityThreat.DEVELOPER_OPTIONS_ENABLED)
            details[SecurityThreat.DEVELOPER_OPTIONS_ENABLED] = "Developer options are enabled"
            SecureLogger.logSecurity("developer_options_enabled")
        }
        
        // Verifica ADB
        if (isAdbEnabled()) {
            threats.add(SecurityThreat.ADB_ENABLED)
            details[SecurityThreat.ADB_ENABLED] = "ADB debugging is enabled"
            SecureLogger.logSecurity("adb_enabled")
        }
        
        val isSecure = threats.isEmpty()
        val report = SecurityReport(isSecure, threats, details)
        
        SecureLogger.logSecurity(
            "device_security_check_completed",
            mapOf(
                "is_secure" to isSecure,
                "threats_count" to threats.size,
                "threats" to threats.map { it.name }
            )
        )
        
        return report
    }
    
    /**
     * Valida segurança antes de operações sensíveis
     * @param operationName Nome da operação sendo validada
     * @return true se a operação pode prosseguir com segurança
     */
    fun validateSensitiveOperation(operationName: String): Boolean {
        SecureLogger.logSecurity("sensitive_operation_validation", mapOf("operation" to operationName))
        
        val report = validateDeviceSecurity()
        
        // Operações sensíveis não devem prosseguir em dispositivos comprometidos
        val criticalThreats = listOf(
            SecurityThreat.ROOT_DETECTED,
            SecurityThreat.DANGEROUS_APP_INSTALLED
        )
        
        val hasCriticalThreats = report.threats.any { it in criticalThreats }
        
        if (hasCriticalThreats) {
            SecureLogger.logSecurity(
                "sensitive_operation_blocked",
                mapOf(
                    "operation" to operationName,
                    "reason" to "critical_security_threats",
                    "threats" to report.threats.filter { it in criticalThreats }.map { it.name }
                )
            )
            return false
        }
        
        SecureLogger.logSecurity(
            "sensitive_operation_approved",
            mapOf("operation" to operationName)
        )
        
        return true
    }
    
    /**
     * Verifica se o dispositivo está com root
     */
    private fun isDeviceRooted(): Boolean {
        return checkRootApps() || checkRootBinaries() || checkBuildTags()
    }
    
    /**
     * Verifica apps de root instalados
     */
    private fun checkRootApps(): Boolean {
        val packageManager = context.packageManager
        return ROOT_APPS.any { packageName ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
    
    /**
     * Verifica binários de root
     */
    private fun checkRootBinaries(): Boolean {
        return ROOT_BINARIES.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }
    
    /**
     * Verifica build tags suspeitas
     */
    private fun checkBuildTags(): Boolean {
        return Build.TAGS?.contains("test-keys") == true
    }
    
    /**
     * Verifica se debugging está habilitado
     */
    private fun isDebuggingEnabled(): Boolean {
        return (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }
    
    /**
     * Verifica se está rodando em emulador
     */
    private fun isEmulator(): Boolean {
        return EMULATOR_PROPS.any { (prop, values) ->
            val propValue = getSystemProperty(prop)
            values.any { value -> propValue.contains(value, ignoreCase = true) }
        }
    }
    
    /**
     * Obtém propriedade do sistema
     */
    private fun getSystemProperty(property: String): String {
        return try {
            val process = Runtime.getRuntime().exec("getprop $property")
            process.inputStream.bufferedReader().readText().trim()
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Verifica apps perigosos instalados
     */
    private fun getDangerousAppsInstalled(): List<String> {
        val packageManager = context.packageManager
        return DANGEROUS_APPS.filter { packageName ->
            try {
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }
    
    /**
     * Verifica se opções de desenvolvedor estão habilitadas
     */
    private fun isDeveloperOptionsEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Verifica se ADB está habilitado
     */
    private fun isAdbEnabled(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.ADB_ENABLED,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
}