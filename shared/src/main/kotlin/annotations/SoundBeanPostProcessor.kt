package com.glycin.annotations

import com.glycin.sound.SoundClip
import com.glycin.sound.SoundPlayer
import org.aopalliance.intercept.MethodInterceptor
import org.springframework.aop.framework.ProxyFactory
import org.springframework.aop.support.AopUtils
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.env.Environment
import org.springframework.context.EnvironmentAware
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

class SoundBeanPostProcessor : BeanPostProcessor, EnvironmentAware {

    private val soundPlayers = ConcurrentHashMap<String, SoundPlayer>()
    private val stopConditionMethods = ConcurrentHashMap<String, Method>()
    private lateinit var environment: Environment

    override fun setEnvironment(environment: Environment) {
        this.environment = environment
    }

    private fun resolvePath(path: String): String = environment.resolvePlaceholders(path)

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any {
        val targetClass = AopUtils.getTargetClass(bean)
        val hasSoundAnnotation = targetClass.methods.any {
            AnnotationUtils.findAnnotation(it, WithSound::class.java) != null ||
                AnnotationUtils.findAnnotation(it, WithLoopingSound::class.java) != null
        }
        if (!hasSoundAnnotation) return bean

        return ProxyFactory(bean).apply {
            isProxyTargetClass = true
            addAdvice(MethodInterceptor { invocation ->
                val withSound = invocation.method.getAnnotation(WithSound::class.java)
                if (withSound != null) {
                    val resolvedPath = resolvePath(withSound.value)
                    soundPlayers.computeIfAbsent(resolvedPath) {
                        SoundPlayer(SoundClip(it))
                    }.play()
                }

                val withLoopingSound = invocation.method.getAnnotation(WithLoopingSound::class.java)
                if (withLoopingSound != null) {
                    val resolvedPath = resolvePath(withLoopingSound.value)
                    val player = soundPlayers.computeIfAbsent(resolvedPath) {
                        SoundPlayer(SoundClip(it))
                    }
                    val shouldStop = withLoopingSound.stopCondition.takeIf { it.isNotEmpty() }?.let { condition ->
                        val method = stopConditionMethods.computeIfAbsent(condition) {
                            AopUtils.getTargetClass(invocation.`this`!!).getMethod(it)
                        }
                        method.invoke(invocation.`this`) as Boolean
                    } ?: false

                    if (shouldStop) {
                        player.stop()
                    } else if (!player.isPlaying()) {
                        player.setVolume(withLoopingSound.volume)
                        player.loop()
                    }
                }

                invocation.proceed()
            })
        }.proxy
    }
}
