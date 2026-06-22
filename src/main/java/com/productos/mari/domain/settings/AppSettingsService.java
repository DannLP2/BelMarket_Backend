package com.productos.mari.domain.settings;

import com.productos.mari.domain.settings.AppSettings;

public interface AppSettingsService {
    AppSettings getSettings();
    AppSettings updateSettings(AppSettings settings);
}
