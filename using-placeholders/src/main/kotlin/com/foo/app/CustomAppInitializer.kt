package com.foo.app

import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext

class CustomAppInitializer : ApplicationContextInitializer<ConfigurableApplicationContext> {
    override fun initialize(applicationContext: ConfigurableApplicationContext) {
        // Usecase : this property source is loaded from an external zookeeper dependent config manager
//        applicationContext.environment.propertySources.addFirst(MapPropertySource("custom",
//            mapOf<String,String>("spring.flyway.placeholders.client" to "local")))
    }
}