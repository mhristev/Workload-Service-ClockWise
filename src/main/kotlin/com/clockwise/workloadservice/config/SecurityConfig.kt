package com.clockwise.workloadservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Value("\${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private lateinit var jwkSetUri: String

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }
            .cors { it.and() }
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers(
                        "/actuator/**",
                        "/health",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                    ).permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtDecoder(jwtDecoder())
                        .jwtAuthenticationConverter(jwtAuthenticationConverter())
                }
            }
            .build()
    }

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
    }

    @Bean
    fun jwtAuthenticationConverter(): ReactiveJwtAuthenticationConverterAdapter {
        val converter = JwtAuthenticationConverter()
        converter.setJwtGrantedAuthoritiesConverter { jwt: Jwt ->
            val authorities = mutableListOf<SimpleGrantedAuthority>()
            
            // Extract roles from JWT claims
            val realmAccess = jwt.getClaimAsMap("realm_access")
            @Suppress("UNCHECKED_CAST")
            val roles = realmAccess?.get("roles") as? List<String>
            roles?.forEach { role ->
                authorities.add(SimpleGrantedAuthority("ROLE_$role"))
            }
            
            // Extract resource access if present
            val resourceAccess = jwt.getClaimAsMap("resource_access")
            resourceAccess?.forEach { (client, access) ->
                @Suppress("UNCHECKED_CAST")
                val clientRoles = (access as? Map<String, Any>)?.get("roles") as? List<String>
                clientRoles?.forEach { role ->
                    authorities.add(SimpleGrantedAuthority("ROLE_${client}_$role"))
                }
            }
            
            authorities as Collection<org.springframework.security.core.GrantedAuthority>
        }
        
        return ReactiveJwtAuthenticationConverterAdapter(converter)
    }
} 