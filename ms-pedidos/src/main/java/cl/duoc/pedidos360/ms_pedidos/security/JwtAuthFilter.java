package cl.duoc.pedidos360.ms_pedidos.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);

    @Value("${azure.activedirectory.issuer-uri}")
    private String issuerUri;

    @Value("${azure.activedirectory.client-id}")
    private String clientId;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token no proporcionado");
            return;
        }

        String token = authHeader.substring(7);

        try {
            Claims claims = Jwts.parserBuilder()
                    .build()
                    .parseClaimsJwt(splitToken(token))
                    .getBody();

            String issuer = claims.getIssuer();
            if (issuer == null || !issuer.startsWith("https://login.microsoftonline.com/")) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Issuer inválido");
                return;
            }

            Object audience = claims.getAudience();
            if (audience == null) {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Audience inválido");
                return;
            }

            List<SimpleGrantedAuthority> authorities = List.of();
            Object rolesClaim = claims.get("roles");
            if (rolesClaim instanceof List<?> rolesList) {
                authorities = rolesList.stream()
                        .map(r -> new SimpleGrantedAuthority("ROLE_" + r.toString()))
                        .collect(Collectors.toList());
            }

            String subject = claims.getSubject();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(subject, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            log.warn("JWT expirado: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token expirado");
        } catch (SignatureException e) {
            log.warn("Firma JWT inválida: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Firma inválida");
        } catch (MalformedJwtException e) {
            log.warn("JWT malformado: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token malformado");
        } catch (Exception e) {
            log.error("Error procesando JWT: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Error de autenticación");
        }
    }

    private String splitToken(String token) {
        String[] parts = token.split("\\.");
        if (parts.length < 2) throw new MalformedJwtException("Token inválido");
        return parts[0] + "." + parts[1] + ".";
    }
}
