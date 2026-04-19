package com.glycin.springsurvivors.security

import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.concurrent.ConcurrentHashMap

class PlayerAuthentication(
    private val playerName: String,
    private val playerAuthorities: MutableSet<GrantedAuthority> = ConcurrentHashMap.newKeySet(),
) : AbstractAuthenticationToken(playerAuthorities) {

    init {
        isAuthenticated = true
    }

    override fun getCredentials(): Any? = null
    override fun getPrincipal(): String = playerName

    fun grant(authority: String) {
        playerAuthorities.add(SimpleGrantedAuthority(authority))
    }

    fun revoke(authority: String) {
        playerAuthorities.removeIf { it.authority == authority }
    }

    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = playerAuthorities
}
