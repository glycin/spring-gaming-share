package com.glycin.springshooter

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Service
import kotlin.random.Random

private val EXCLUDED_PACKAGES = listOf(
    "com.glycin.springshooter",
)

private val EXCLUDED_BEANS = setOf(
    "springShooterApplication",
    "gameLoop",
    "gameRenderService",
    "springApplicationContextService",
)

@Service
class SpringApplicationContextService(
    private val applicationContext: ConfigurableApplicationContext,
) {

    private val logger = LoggerFactory.getLogger(SpringApplicationContextService::class.java)
    private val beanFactory: DefaultListableBeanFactory = applicationContext.beanFactory as DefaultListableBeanFactory

    fun getEnemies(): List<Enemy> {
        val definedBeans = beanFactory.beanDefinitionNames.toSet()
        val singletonBeans = beanFactory.singletonNames.toSet()
        val allBeanNames = definedBeans + singletonBeans

        val enemies = allBeanNames
            .filter { name -> name !in EXCLUDED_BEANS && !isGameBean(name) }
            .map { name ->
                val beanType = beanFactory.getType(name)
                val className = beanType?.simpleName ?: "Unknown"
                val definition = if (beanFactory.containsBeanDefinition(name)) beanFactory.getBeanDefinition(name) else null
                val random = Random(name.hashCode())

                val enemyType = if (random.nextBoolean()) EnemyType.ELITE else EnemyType.GRUNT

                Enemy(
                    beanName = name,
                    beanClassName = className,
                    x = 0.0,
                    y = 0.0,
                    health = calculateHealth(beanType, definition),
                    size = calculateSize(definition),
                    type = enemyType,
                    alive = false,
                )
            }

        logger.info("Loaded {} enemy beans from ApplicationContext ({} defined + {} direct singletons)",
            enemies.size, definedBeans.size, singletonBeans.size - definedBeans.size)

        return enemies
    }

    fun removeBean(enemy: Enemy) {
        if (beanFactory.containsBeanDefinition(enemy.beanName)) {
            logger.info("Destroying bean '{}' ({}) — enemy killed!", enemy.beanName, enemy.beanClassName)
            beanFactory.destroySingleton(enemy.beanName)
            beanFactory.removeBeanDefinition(enemy.beanName)
        } else if (beanFactory.containsSingleton(enemy.beanName)) {
            logger.info("Destroying singleton '{}' ({}) — enemy killed!", enemy.beanName, enemy.beanClassName)
            beanFactory.destroySingleton(enemy.beanName)
        } else {
            logger.warn("Bean '{}' already removed from context", enemy.beanName)
        }
    }

    fun closeContext() {
        logger.info("GAME OVER — closing ApplicationContext!")
        applicationContext.close()
    }

    fun restartWithProfile(profile: String) {
        logger.info("Restarting ApplicationContext with profile '{}'", profile)
        nextProfile = profile
        applicationContext.close()
    }

    companion object {
        @Volatile
        @JvmStatic
        var nextProfile: String? = null
    }

    private fun isGameBean(beanName: String): Boolean {
        val beanType = beanFactory.getType(beanName) ?: return false
        return EXCLUDED_PACKAGES.any { prefix -> beanType.name.startsWith(prefix) }
    }

    private fun calculateHealth(beanType: Class<*>?, definition: BeanDefinition?): Int {
        val baseHp = when {
            definition?.isLazyInit == true -> 30
            definition?.scope == BeanDefinition.SCOPE_PROTOTYPE -> 40
            else -> 50
        }
        val methodCount = beanType?.declaredMethods?.size ?: 0
        val dependencyCount = definition?.dependsOn?.size ?: 0
        return (baseHp + methodCount * 8 + dependencyCount * 15).coerceIn(10, 200)
    }

    private fun calculateSize(definition: BeanDefinition?): Double {
        return when (definition?.scope) {
            BeanDefinition.SCOPE_PROTOTYPE -> 0.6
            BeanDefinition.SCOPE_SINGLETON, "", null -> 1.0
            else -> 0.8
        }
    }
}
