package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import com.example.util.DeviceIdHelper
import com.example.util.NetworkMonitor
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

sealed class LoginResult {
    data class Success(val username: String) : LoginResult()
    data class Error(val message: String) : LoginResult()
    data class SessionBlocked(val message: String) : LoginResult()
}

sealed class LogoutResult {
    data object Success : LogoutResult()
    data class NoInternet(val message: String) : LogoutResult()
    data class Error(val message: String) : LogoutResult()
}

data class UserSessionInfo(
    val username: String = "",
    val deviceId: String = "",
    val isLoggedIn: Boolean = false,
    val isSessionActiveOnServer: Boolean = false
)

class AuthRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("auth_security_prefs", Context.MODE_PRIVATE)

    private val networkMonitor = NetworkMonitor(context)

    private fun getDatabaseInstance(): FirebaseDatabase? {
        return try {
            val customUrl = prefs.getString("custom_rtdb_url", null)
            if (!customUrl.isNullOrBlank()) {
                FirebaseDatabase.getInstance(customUrl)
            } else {
                if (FirebaseApp.getApps(context).isEmpty()) {
                    FirebaseApp.initializeApp(context)
                }
                FirebaseDatabase.getInstance()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getUsersRef(): DatabaseReference? {
        return getDatabaseInstance()?.getReference("usuarios")
    }

    fun setCustomDatabaseUrl(url: String?) {
        prefs.edit().putString("custom_rtdb_url", url?.trim()?.ifBlank { null }).apply()
    }

    fun getCustomDatabaseUrl(): String? {
        return prefs.getString("custom_rtdb_url", null)
    }

    fun getCurrentDeviceId(): String {
        return DeviceIdHelper.getDeviceId(context)
    }

    // --- LOGIN CON AUTODESTRUCCIÓN DE PRUEBA (1 HORA / 3600 SEGUNDOS) ---
    suspend fun login(usernameInput: String, passwordInput: String): LoginResult = withContext(Dispatchers.IO) {
        val username = usernameInput.trim().lowercase()
        val password = passwordInput.trim()

        if (username.isEmpty() || password.isEmpty()) {
            return@withContext LoginResult.Error("Debes completar el usuario y la contraseña")
        }

        val currentDeviceId = DeviceIdHelper.getDeviceId(context)

        if (!networkMonitor.isOnline()) {
            return@withContext LoginResult.Error("Sin conexión a Internet.")
        }

        val usersRef = getUsersRef()
            ?: return@withContext LoginResult.Error("Error de conexión con el servidor.")

        val inputHash = hashPassword(password)

        try {
            val userRef = usersRef.child(username)
            val snapshot = userRef.get().await()

            // 1. Si el usuario NO existe, NO lo creamos. Lanzamos error.
            if (!snapshot.exists()) {
                return@withContext LoginResult.Error("Usuario no autorizado. Contacte al administrador.")
            }

            // 2. LEER DATOS CRÍTICOS
            val userType = snapshot.child("user_type").getValue(String::class.java) ?: "limited"
            val trialMinutes = when (val raw = snapshot.child("trial_time_minutes").value) {
                is Long -> raw
                is Int -> raw.toLong()
                is Double -> raw.toLong()
                is Number -> raw.toLong()
                is String -> raw.toLongOrNull() ?: 10L
                else -> 10L
            }.coerceAtLeast(1L)
            val lastLogin = snapshot.child("last_login_timestamp").getValue(Long::class.java) ?: 0L
            val currentTime = System.currentTimeMillis()
            val trialDurationMs = trialMinutes * 60 * 1000L

            // 3. LÓGICA DE AUTODESTRUCCIÓN PARA USUARIOS DE PRUEBA (TIEMPO DINÁMICO)
            if (userType != "unlimited" && lastLogin != 0L) {
                if (currentTime > (lastLogin + trialDurationMs)) {
                    // EL TIEMPO EXPIRÓ: Borramos el usuario de la base de datos para siempre
                    userRef.removeValue().await()
                    clearLocalSession()
                    return@withContext LoginResult.Error("Tu cuenta de prueba ha expirado y ha sido eliminada.")
                }
            }

            val storedHash = snapshot.child("password_hash").getValue(String::class.java) ?: ""
            val sessionActive = snapshot.child("session_active").getValue(Boolean::class.java) ?: false
            val activeDeviceId = snapshot.child("active_device_id").getValue(String::class.java) ?: ""

            // 4. Validación de contraseña estricta (solo hashes)
            if (storedHash != inputHash) {
                return@withContext LoginResult.Error("Contraseña incorrecta")
            }

            // 5. Bloqueo de sesión simultánea
            if (sessionActive && activeDeviceId.isNotEmpty() && activeDeviceId != currentDeviceId) {
                return@withContext LoginResult.SessionBlocked(
                    "Sesión activa en otro dispositivo. Cierre la sesión en el otro equipo primero."
                )
            }

            // 6. Si llegamos aquí, el acceso es concedido. Actualizamos Firebase.
            val updates = mapOf(
                "session_active" to true,
                "active_device_id" to currentDeviceId,
                "device_model" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "last_login_timestamp" to currentTime
            )
            userRef.updateChildren(updates).await()

            saveLocalSession(username, currentDeviceId, userType, currentTime, trialMinutes)
            return@withContext LoginResult.Success(username)

        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext LoginResult.Error("Error: ${e.localizedMessage ?: e.message}")
        }
    }

    // --- LOGOUT ---
    suspend fun logout(usernameInput: String? = null): LogoutResult = withContext(Dispatchers.IO) {
        val username = usernameInput?.trim()?.lowercase() ?: getSavedUsername() ?: ""

        // 1. Check internet connectivity
        if (!networkMonitor.isOnline()) {
            return@withContext LogoutResult.NoInternet(
                "Sin conexión a Internet. Conéctate a una red para notificar la desactivación de tu sesión."
            )
        }

        if (username.isEmpty()) {
            clearLocalSession()
            return@withContext LogoutResult.Success
        }

        val usersRef = getUsersRef()
        if (usersRef == null) {
            clearLocalSession()
            return@withContext LogoutResult.Success
        }

        val currentDeviceId = DeviceIdHelper.getDeviceId(context)

        try {
            val userRef = usersRef.child(username)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                val userType = snapshot.child("user_type").getValue(String::class.java) ?: "limited"
                val trialMinutes = when (val raw = snapshot.child("trial_time_minutes").value) {
                    is Long -> raw
                    is Int -> raw.toLong()
                    is Double -> raw.toLong()
                    is Number -> raw.toLong()
                    is String -> raw.toLongOrNull() ?: getTrialMinutes()
                    else -> getTrialMinutes()
                }.coerceAtLeast(1L)
                val lastLogin = snapshot.child("last_login_timestamp").getValue(Long::class.java) ?: 0L
                val currentTime = System.currentTimeMillis()
                val trialDurationMs = trialMinutes * 60 * 1000L

                // Si es usuario de prueba y su tiempo de prueba se cumplió, lo borramos definitivamente
                if (userType != "unlimited" && lastLogin != 0L && currentTime >= (lastLogin + trialDurationMs)) {
                    userRef.removeValue().await()
                } else {
                    val activeDeviceId = snapshot.child("active_device_id").getValue(String::class.java) ?: ""
                    // Only clear Firebase if this device is still the recorded active device
                    if (activeDeviceId.isEmpty() || activeDeviceId == currentDeviceId) {
                        val updates = mapOf(
                            "session_active" to false,
                            "active_device_id" to "",
                            "last_logout_timestamp" to currentTime
                        )
                        userRef.updateChildren(updates).await()
                    }
                }
            }

            // Clear local session after successful Firebase update
            clearLocalSession()
            return@withContext LogoutResult.Success
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext LogoutResult.Error("Error al notificar cierre de sesión: ${e.localizedMessage ?: e.message}")
        }
    }

    // --- CHECK SESSION STATUS ON LAUNCH ---
    suspend fun checkSessionStatus(usernameInput: String? = null): Boolean = withContext(Dispatchers.IO) {
        val username = usernameInput?.trim()?.lowercase() ?: getSavedUsername() ?: return@withContext false
        val currentDeviceId = DeviceIdHelper.getDeviceId(context)

        if (!networkMonitor.isOnline()) {
            // Keep local state when offline so user can work uninterrupted if they were previously logged in on this device
            return@withContext isLoggedIn()
        }

        val usersRef = getUsersRef() ?: return@withContext isLoggedIn()

        try {
            val userRef = usersRef.child(username)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                val sessionActive = snapshot.child("session_active").getValue(Boolean::class.java) ?: false
                val activeDeviceId = snapshot.child("active_device_id").getValue(String::class.java) ?: ""

                if (!sessionActive || (activeDeviceId.isNotEmpty() && activeDeviceId != currentDeviceId)) {
                    // Session was closed from another device or deactivated
                    clearLocalSession()
                    return@withContext false
                }
                return@withContext true
            }
            return@withContext isLoggedIn()
        } catch (e: Exception) {
            return@withContext isLoggedIn()
        }
    }

    // --- REALTIME LISTENER FOR SESSION TERMINATION ---
    fun listenToSession(username: String, onSessionRevoked: () -> Unit): ValueEventListener? {
        val usersRef = getUsersRef() ?: return null
        val currentDeviceId = DeviceIdHelper.getDeviceId(context)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val sessionActive = snapshot.child("session_active").getValue(Boolean::class.java) ?: false
                    val activeDeviceId = snapshot.child("active_device_id").getValue(String::class.java) ?: ""

                    if (!sessionActive || (activeDeviceId.isNotEmpty() && activeDeviceId != currentDeviceId)) {
                        clearLocalSession()
                        onSessionRevoked()
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Ignore cancellation
            }
        }

        usersRef.child(username.lowercase()).addValueEventListener(listener)
        return listener
    }

    fun removeSessionListener(username: String, listener: ValueEventListener) {
        getUsersRef()?.child(username.lowercase())?.removeEventListener(listener)
    }

    // --- PASSWORD HASHING (SHA-256) ---
    fun hashPassword(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(StandardCharsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    // --- LOCAL SESSION MANAGEMENT ---
    private fun saveLocalSession(username: String, deviceId: String, userType: String, loginTime: Long, trialMinutes: Long = 10L) {
        prefs.edit().apply {
            putString("auth_username", username)
            putString("auth_device_id", deviceId)
            putString("auth_user_type", userType)
            putLong("auth_login_time", loginTime)
            putLong("auth_trial_minutes", trialMinutes)
            putBoolean("auth_is_logged_in", true)
            apply()
        }
    }

    fun clearLocalSession() {
        prefs.edit().apply {
            remove("auth_username")
            remove("auth_device_id")
            remove("auth_user_type")
            remove("auth_login_time")
            remove("auth_trial_minutes")
            putBoolean("auth_is_logged_in", false)
            apply()
        }
    }

    fun getUserType(): String = prefs.getString("auth_user_type", "limited") ?: "limited"

    fun getLoginTime(): Long = prefs.getLong("auth_login_time", 0L)

    fun getTrialMinutes(): Long = prefs.getLong("auth_trial_minutes", 10L).coerceAtLeast(1L)

    fun isUnlimited(): Boolean = getUserType().equals("unlimited", ignoreCase = true)

    fun getSavedUsername(): String? {
        return prefs.getString("auth_username", null)
    }

    fun getSavedDeviceId(): String? {
        return prefs.getString("auth_device_id", null)
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean("auth_is_logged_in", false)
    }
}
