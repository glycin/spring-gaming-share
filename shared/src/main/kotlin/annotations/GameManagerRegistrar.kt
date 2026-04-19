package com.glycin.annotations

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner
import org.springframework.core.type.filter.AnnotationTypeFilter

class GameManagerRegistrar : BeanDefinitionRegistryPostProcessor {

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        val scanner = ClassPathBeanDefinitionScanner(registry)
        scanner.addIncludeFilter(AnnotationTypeFilter(GameManager::class.java))
        scanner.scan("com.glycin")
    }

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {}
}
