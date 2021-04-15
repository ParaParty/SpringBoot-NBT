package com.superexercisebook.nbtdemo.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.superexercisebook.jackson.nbt.NbtMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration

@Configuration
open class AppConfig {
    @Bean
    open fun ObjectMapper(): ObjectMapper = NbtMapper()
}