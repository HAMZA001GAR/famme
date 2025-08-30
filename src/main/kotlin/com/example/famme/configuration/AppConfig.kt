package com.example.famme.configuration

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import com.fasterxml.jackson.databind.ObjectMapper

@Configuration
class AppConfig {

    @Bean
    fun restTemplate(): RestTemplate {
        return RestTemplate()
    }
    
    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper()
    }
}
