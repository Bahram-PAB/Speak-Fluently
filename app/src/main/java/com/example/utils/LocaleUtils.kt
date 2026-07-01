package com.example.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleUtils {

    /**
     * Creates a localized context for the specified language code.
     */
    fun getLocalizedContext(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }

    /**
     * Direct string resolution bypassing standard system resource cache.
     */
    fun getString(context: Context, resId: Int, languageCode: String): String {
        return try {
            getLocalizedContext(context, languageCode).resources.getString(resId)
        } catch (e: Exception) {
            context.getString(resId) // Fallback
        }
    }

    /**
     * Formatted string resolution.
     */
    fun getString(context: Context, resId: Int, languageCode: String, vararg formatArgs: Any): String {
        return try {
            getLocalizedContext(context, languageCode).resources.getString(resId, *formatArgs)
        } catch (e: Exception) {
            context.getString(resId, *formatArgs) // Fallback
        }
    }
}
