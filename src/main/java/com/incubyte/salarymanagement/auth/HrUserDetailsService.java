package com.incubyte.salarymanagement.auth;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class HrUserDetailsService implements UserDetailsService {
    private final HrUserRepository users;

    public HrUserDetailsService(HrUserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return users.findByEmail(EmailNormalizer.normalize(email))
                .filter(HrUser::isActive)
                .map(user -> User.withUsername(user.getEmail()).password(user.getPasswordHash()).roles("HR").build())
                .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}
