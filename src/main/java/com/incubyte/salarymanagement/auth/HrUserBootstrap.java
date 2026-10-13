package com.incubyte.salarymanagement.auth;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(prefix = "app.bootstrap.hr-user", name = "enabled", havingValue = "true")
public class HrUserBootstrap implements ApplicationRunner {
    private final HrUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final Environment environment;

    public HrUserBootstrap(HrUserRepository users, PasswordEncoder passwordEncoder, Environment environment) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.environment = environment;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String configuredEmail = EmailNormalizer.normalize(environment.getRequiredProperty("HR_INITIAL_EMAIL"));
        String initialPassword = environment.getRequiredProperty("HR_INITIAL_PASSWORD");
        if (configuredEmail.isBlank() || initialPassword.isBlank()) {
            throw new IllegalStateException("HR_INITIAL_EMAIL and HR_INITIAL_PASSWORD must be configured for account bootstrap.");
        }

        var existing = users.findById((short) 1);
        if (existing.isPresent()) {
            if (!existing.get().getEmail().equals(configuredEmail)) {
                throw new IllegalStateException("An HR account already exists with a different email; refusing to create another account.");
            }
            return;
        }
        users.save(new HrUser(configuredEmail, passwordEncoder.encode(initialPassword)));
    }
}
