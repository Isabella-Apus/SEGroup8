package com.segroup8.secondhand.security;

public record AuthenticatedUser(long userId, String username, String role) {
}
