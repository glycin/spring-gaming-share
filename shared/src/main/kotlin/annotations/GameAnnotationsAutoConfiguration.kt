package com.glycin.annotations

import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Role

@AutoConfiguration
class GameAnnotationsAutoConfiguration {

    companion object {
        @JvmStatic
        @Bean
        fun soundBeanPostProcessor() = SoundBeanPostProcessor()

        @JvmStatic
        @Bean
        fun gameManagerRegistrar() = GameManagerRegistrar()

        @JvmStatic
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        fun updateMethodRegistry() = UpdateMethodRegistry()

        @JvmStatic
        @Bean
        fun updateBeanPostProcessor(registry: UpdateMethodRegistry) = UpdateBeanPostProcessor(registry)

        @JvmStatic
        @Bean
        @Role(BeanDefinition.ROLE_INFRASTRUCTURE)
        fun rendererMethodRegistry() = RendererMethodRegistry()

        @JvmStatic
        @Bean
        fun rendererBeanPostProcessor(registry: RendererMethodRegistry) = RendererBeanPostProcessor(registry)

    }
}
