package com.example.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.app.common.Constants
import kotlinx.coroutines.flow.first

private val Context.authDataStore by preferencesDataStore(name = "auth_store")

data class RememberedLoginInfo(
    val account: String = "",
    val password: String = "",
    val rememberPassword: Boolean = false,
    val autoLoginEnabled: Boolean = false
)

class TokenStore(private val context: Context) {

    companion object {
        private val TOKEN_KEY = stringPreferencesKey(Constants.PREFS_TOKEN)
        private val USER_ID_KEY = longPreferencesKey(Constants.PREFS_USER_ID)
        private val USERNAME_KEY = stringPreferencesKey(Constants.PREFS_USERNAME)

        private val LAST_LOGIN_ACCOUNT_KEY = stringPreferencesKey("last_login_account")
        private val REMEMBERED_PASSWORD_KEY = stringPreferencesKey("remembered_password")
        private val REMEMBER_PASSWORD_ENABLED_KEY = booleanPreferencesKey("remember_password_enabled")

        private val AUTO_LOGIN_ENABLED_KEY = booleanPreferencesKey("auto_login_enabled")
    }

    suspend fun saveLoginSession(
        token: String,
        userId: Long? = null,
        username: String? = null
    ) {
        context.authDataStore.edit { prefs ->
            prefs[TOKEN_KEY] = token

            if (userId != null) {
                prefs[USER_ID_KEY] = userId
            } else {
                prefs.remove(USER_ID_KEY)
            }

            if (!username.isNullOrBlank()) {
                prefs[USERNAME_KEY] = username
            } else {
                prefs.remove(USERNAME_KEY)
            }
        }
    }

    suspend fun getToken(): String? {
        val prefs = context.authDataStore.data.first()
        return prefs[TOKEN_KEY]
    }

    suspend fun getUsername(): String? {
        val prefs = context.authDataStore.data.first()
        return prefs[USERNAME_KEY]
    }

    /**
     * 只清除 token 会话，不清除账号密码和自动登录配置。
     *
     * 这样做是为了支持：
     * token 过期后，仍然可以使用已经保存的账号密码重新登录获取新 token。
     */
    suspend fun clearLoginSession() {
        context.authDataStore.edit { prefs ->
            prefs.remove(TOKEN_KEY)
            prefs.remove(USER_ID_KEY)
            prefs.remove(USERNAME_KEY)
        }
    }

    /**
     * 手动退出登录、自动登录失败时，可以单独关闭自动登录。
     * 不清除账号密码，方便用户重新点击登录。
     */
    suspend fun setAutoLoginEnabled(enabled: Boolean) {
        context.authDataStore.edit { prefs ->
            prefs[AUTO_LOGIN_ENABLED_KEY] = enabled
        }
    }

    suspend fun clearAll() {
        context.authDataStore.edit { prefs ->
            prefs.clear()
        }
    }

    suspend fun saveRememberedLoginInfo(
        account: String,
        password: String,
        rememberPassword: Boolean,
        autoLoginEnabled: Boolean
    ) {
        val finalRememberPassword = rememberPassword || autoLoginEnabled

        context.authDataStore.edit { prefs ->
            prefs[LAST_LOGIN_ACCOUNT_KEY] = account
            prefs[REMEMBER_PASSWORD_ENABLED_KEY] = finalRememberPassword
            prefs[AUTO_LOGIN_ENABLED_KEY] = autoLoginEnabled

            if (finalRememberPassword) {
                prefs[REMEMBERED_PASSWORD_KEY] = password
            } else {
                prefs.remove(REMEMBERED_PASSWORD_KEY)
            }
        }
    }

    suspend fun getRememberedLoginInfo(): RememberedLoginInfo {
        val prefs = context.authDataStore.data.first()

        return RememberedLoginInfo(
            account = prefs[LAST_LOGIN_ACCOUNT_KEY].orEmpty(),
            password = prefs[REMEMBERED_PASSWORD_KEY].orEmpty(),
            rememberPassword = prefs[REMEMBER_PASSWORD_ENABLED_KEY] ?: false,
            autoLoginEnabled = prefs[AUTO_LOGIN_ENABLED_KEY] ?: false
        )
    }
}