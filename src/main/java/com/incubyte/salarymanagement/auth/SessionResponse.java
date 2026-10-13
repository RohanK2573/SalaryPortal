package com.incubyte.salarymanagement.auth;

public record SessionResponse(boolean authenticated, String email) {
    public static SessionResponse anonymous() { return new SessionResponse(false, null); }
    public static SessionResponse authenticated(String email) { return new SessionResponse(true, email); }
}
