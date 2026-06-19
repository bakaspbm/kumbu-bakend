package com.kumbu.backend.security;

public final class AdminRoleExpressions {

    private AdminRoleExpressions() {}

    public static final String SUPER_ADMIN = "hasAuthority('ROLE_SUPER_ADMIN')";
    public static final String ADMIN_OR_ABOVE = "hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN')";
    public static final String ANY_ADMIN = "hasAnyAuthority('ROLE_SUPER_ADMIN','ROLE_ADMIN','ROLE_SUPPORT')";
}
