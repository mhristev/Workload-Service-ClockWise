package com.clockwise.workloadservice.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
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
@EnableReactiveMethodSecurity
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
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    .pathMatchers(HttpMethod.OPTIONS).permitAll()
                    
                    // Work Session endpoints - Role hierarchy: admin > manager > user
                    // Users can clock in/out for themselves, managers can for their team, admins for anyone
                    .pathMatchers(HttpMethod.POST, "/v1/work-sessions/clock-in").hasAnyRole("admin", "manager", "user")
                    .pathMatchers(HttpMethod.POST, "/v1/work-sessions/clock-out").hasAnyRole("admin", "manager", "user")
                    
                    // Work hours viewing - managers and admins can view all users, users can view their own
                    .pathMatchers(HttpMethod.GET, "/v1/work-sessions/user/**").hasAnyRole("admin", "manager", "user")
                    
                    // Session Notes endpoints - All authenticated users can manage notes
                    .pathMatchers(HttpMethod.POST, "/v1/session-notes").hasAnyRole("admin", "manager", "user")
                    .pathMatchers(HttpMethod.GET, "/v1/session-notes/**").hasAnyRole("admin", "manager", "user")
                    
                    // All other authenticated users can read
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