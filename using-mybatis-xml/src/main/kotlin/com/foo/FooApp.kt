package com.foo

import com.foo.app.CacheService
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class FooApp
fun main(args: Array<String>) {
    val appContext = runApplication<FooApp>(*args)

    val cacheService = appContext.getBean(CacheService::class.java)
    println(cacheService.employee)
}
