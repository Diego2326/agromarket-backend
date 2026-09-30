package com.agromarket.agromarket_backend.auth.infrastructure.security

import com.agromarket.agromarket_backend.user.domain.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val tokens: JwtTokenProvider,
    private val users: UserRepository,
) : OncePerRequestFilter() {
    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        val authorization = request.getHeader("Authorization")
        if (authorization?.startsWith("Bearer ", ignoreCase = true) == true &&
            SecurityContextHolder.getContext().authentication == null
        ) {
            val token = authorization.substringAfter(' ').trim()
            try {
                val userId = tokens.verifyAndGetUserId(token)
                val user = users.findById(userId)
                if (user?.enabled == true) {
                    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                        AuthenticatedUser(user.id),
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_USER")),
                    )
                }
            } catch (_: InvalidJwtException) {
                SecurityContextHolder.clearContext()
            }
        }
        filterChain.doFilter(request, response)
    }
}
