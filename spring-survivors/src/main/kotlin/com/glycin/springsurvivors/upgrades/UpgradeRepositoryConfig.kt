package com.glycin.springsurvivors.upgrades

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class UpgradeRepositoryConfig {

    @Bean
    fun upgradeRepositoryCustomImpl(): UpgradeRepositoryCustomImpl = UpgradeRepositoryCustomImpl()
}
