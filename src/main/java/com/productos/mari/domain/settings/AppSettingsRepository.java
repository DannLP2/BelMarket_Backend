package com.productos.mari.domain.settings;

import com.productos.mari.domain.settings.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AppSettingsRepository extends JpaRepository<AppSettings, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT s.logoUrl FROM AppSettings s WHERE s.logoUrl IS NOT NULL")
    java.util.List<String> findAllLogoUrls();

    @org.springframework.data.jpa.repository.Query("SELECT s.bgLightUrl FROM AppSettings s WHERE s.bgLightUrl IS NOT NULL")
    java.util.List<String> findAllBgLightUrls();

    @org.springframework.data.jpa.repository.Query("SELECT s.bgDarkUrl FROM AppSettings s WHERE s.bgDarkUrl IS NOT NULL")
    java.util.List<String> findAllBgDarkUrls();

    @org.springframework.data.jpa.repository.Query("SELECT s.faviconUrl FROM AppSettings s WHERE s.faviconUrl IS NOT NULL")
    java.util.List<String> findAllFaviconUrls();
}
