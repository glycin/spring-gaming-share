package com.glycin.springpong

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class RestController(
    private val coolService: CoolService
) {

    @GetMapping("/cool")
    fun getCoolFactor() = coolService.getCoolness()
}