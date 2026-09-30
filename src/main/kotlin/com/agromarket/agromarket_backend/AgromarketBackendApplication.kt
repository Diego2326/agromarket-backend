package com.agromarket.agromarket_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class AgromarketBackendApplication

fun main(args: Array<String>) {
	runApplication<AgromarketBackendApplication>(*args)
}
