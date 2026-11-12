package mitolight.app

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The app's own language, independent of whatever the phone's system language happens to be
 *. [SYSTEM] is a real choice and not merely the absence of one — it is what tells
 * Android to follow the phone rather than to pin the app to English.
 */
enum class AppLocale(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    CZECH("cs"),
}

/**
 * Reads and writes the choice above. On API 33+ the system [LocaleManager] is the source of truth
 * the framework actually applies; AppCompat is kept in sync so older devices and
 * [AppCompatDelegate.getApplicationLocales] still agree. Preferences remember the choice across
 * process death so [MainActivity] can re-apply it in [attachBaseContext] before the first frame.
 */
class LocalePreferences(context: Context) {

    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences("locale", Context.MODE_PRIVATE)

    private val _locale = MutableStateFlow(read())
    val locale: StateFlow<AppLocale> = _locale.asStateFlow()

    /**
     * Applied from [MainActivity.attachBaseContext], before [android.app.Activity.onCreate], so the
     * first composition already sees the chosen resources rather than the system default.
     */
    fun applyStored() {
        apply(_locale.value)
    }

    fun set(locale: AppLocale) {
        preferences.edit().putString(KEY, locale.name).apply()
        _locale.value = locale
        apply(locale)
    }

    private fun apply(locale: AppLocale) {
        if (Build.VERSION.SDK_INT >= 33) {
            val manager = appContext.getSystemService(LocaleManager::class.java)
            manager.applicationLocales = locale.tag
                ?.let { LocaleList.forLanguageTags(it) }
                ?: LocaleList.getEmptyLocaleList()
        }

        val compat = locale.tag
            ?.let { LocaleListCompat.forLanguageTags(it) }
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(compat)
    }

    private fun read(): AppLocale {
        val stored = preferences.getString(KEY, null) ?: return AppLocale.SYSTEM
        return runCatching { AppLocale.valueOf(stored) }.getOrDefault(AppLocale.SYSTEM)
    }

    private companion object {
        const val KEY = "app_locale"
    }
}
