package com.incubyte.salarymanagement.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class HrUserBootstrapTest {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4);

    @Test
    void createsOnlyNormalizedEmailAndPasswordHash() {
        var repository = new RepositoryDouble(null);
        var environment = environment(" HR.User@Example.COM ", "secret-password");

        new HrUserBootstrap(repository.proxy(), passwordEncoder, environment).run(new DefaultApplicationArguments());

        assertThat(repository.savedUser.getEmail()).isEqualTo("hr.user@example.com");
        assertThat(repository.savedUser.getPasswordHash()).isNotEqualTo("secret-password");
        assertThat(passwordEncoder.matches("secret-password", repository.savedUser.getPasswordHash())).isTrue();
    }

    @Test
    void rerunForSameEmailDoesNotReplaceTheAccount() {
        var repository = new RepositoryDouble(new HrUser("hr.user@example.com", "old-hash"));

        new HrUserBootstrap(repository.proxy(), passwordEncoder,
                environment("HR.User@example.com", "new-secret")).run(new DefaultApplicationArguments());

        assertThat(repository.savedUser).isNull();
    }

    @Test
    void refusesToCreateAnAccountWithDifferentEmail() {
        var repository = new RepositoryDouble(new HrUser("hr.user@example.com", "hash"));

        assertThatThrownBy(() -> new HrUserBootstrap(repository.proxy(), passwordEncoder,
                environment("other@example.com", "secret")).run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different email");
        assertThat(repository.savedUser).isNull();
    }

    private Environment environment(String email, String password) {
        return (Environment) Proxy.newProxyInstance(Environment.class.getClassLoader(), new Class<?>[]{Environment.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getRequiredProperty")) {
                        return switch ((String) args[0]) {
                            case "HR_INITIAL_EMAIL" -> email;
                            case "HR_INITIAL_PASSWORD" -> password;
                            default -> throw new IllegalArgumentException("Unexpected property");
                        };
                    }
                    if (method.getName().equals("toString")) return "TestEnvironment";
                    if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
                    if (method.getName().equals("equals")) return proxy == args[0];
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    private static final class RepositoryDouble {
        private final HrUser existingUser;
        private HrUser savedUser;

        private RepositoryDouble(HrUser existingUser) {
            this.existingUser = existingUser;
        }

        private HrUserRepository proxy() {
            return (HrUserRepository) Proxy.newProxyInstance(HrUserRepository.class.getClassLoader(),
                    new Class<?>[]{HrUserRepository.class}, (proxy, method, args) -> switch (method.getName()) {
                        case "findById" -> Optional.ofNullable(existingUser);
                        case "save" -> savedUser = (HrUser) args[0];
                        case "toString" -> "TestHrUserRepository";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    });
        }
    }
}
