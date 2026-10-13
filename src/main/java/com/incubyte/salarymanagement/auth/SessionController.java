package com.incubyte.salarymanagement.auth;

import com.incubyte.salarymanagement.api.ApiProblem;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/session")
public class SessionController {
    private final AuthenticationManager authenticationManager;
    private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
    private final SecurityContextRepository securityContextRepository;
    private final CsrfTokenRepository csrfTokenRepository;
    private final LoginRateLimiter rateLimiter;
    private final ClientIpResolver clientIpResolver;

    public SessionController(AuthenticationManager authenticationManager,
            SessionAuthenticationStrategy sessionAuthenticationStrategy,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            LoginRateLimiter rateLimiter,
            ClientIpResolver clientIpResolver) {
        this.authenticationManager = authenticationManager;
        this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
        this.securityContextRepository = securityContextRepository;
        this.csrfTokenRepository = csrfTokenRepository;
        this.rateLimiter = rateLimiter;
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping
    public SessionResponse current(CsrfToken csrfToken) {
        // Resolve the deferred token so CookieCsrfTokenRepository writes the bootstrap cookie.
        csrfToken.getToken();
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return SessionResponse.anonymous();
        }
        return SessionResponse.authenticated(authentication.getName());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInRequest request, HttpServletRequest servletRequest,
            jakarta.servlet.http.HttpServletResponse servletResponse) {
        if (!rateLimiter.allow(clientIpResolver.resolve(servletRequest))) {
            return problem(HttpStatus.TOO_MANY_REQUESTS, "Too many sign-in attempts. Try again later.");
        }

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(
                    EmailNormalizer.normalize(request.email()), request.password()));
        } catch (AuthenticationException exception) {
            return problem(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }

        servletRequest.getSession(true);
        sessionAuthenticationStrategy.onAuthentication(authentication, servletRequest, servletResponse);
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, servletRequest, servletResponse);
        csrfTokenRepository.saveToken(null, servletRequest, servletResponse);
        return ResponseEntity.ok(SessionResponse.authenticated(authentication.getName()));
    }

    @DeleteMapping
    public ResponseEntity<Void> signOut(HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response, Authentication authentication) {
        new SecurityContextLogoutHandler().logout(request, response, authentication);
        csrfTokenRepository.saveToken(null, request, response);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<ApiProblem> problem(HttpStatus status, String detail) {
        var body = new ApiProblem("about:blank", status.getReasonPhrase(), status.value(), detail, List.of());
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
