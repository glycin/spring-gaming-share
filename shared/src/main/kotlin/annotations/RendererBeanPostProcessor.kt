package com.glycin.annotations

import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.Ordered
import org.springframework.core.annotation.AnnotationUtils

class RendererBeanPostProcessor(
    private val registry: RendererMethodRegistry,
) : BeanPostProcessor, Ordered {

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        val targetClass = AopUtils.getTargetClass(bean)
        if (!AnnotationUtils.isCandidateClass(targetClass, Renderer::class.java)) return bean

        for (method in targetClass.methods) {
            AnnotationUtils.findAnnotation(method, Renderer::class.java) ?: continue
            method.isAccessible = true
            registry.register(beanName, bean, method)
        }
        return bean
    }
}
