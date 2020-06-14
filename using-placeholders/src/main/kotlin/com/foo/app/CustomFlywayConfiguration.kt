package com.foo.app

import org.flywaydb.core.api.configuration.FluentConfiguration
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer
import org.springframework.context.annotation.Configuration

@Configuration
class CustomFlywayConfiguration : FlywayConfigurationCustomizer {
    override fun customize(configuration: FluentConfiguration?) {
        configuration?.placeholders?.put("client", "local")
    }
}