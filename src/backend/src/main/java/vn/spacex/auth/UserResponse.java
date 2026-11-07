package vn.spacex.auth;

import vn.spacex.user.Role;

public record UserResponse(String displayName, String email, Role role) {

    static UserResponse from(AppUserDetails user) {
        return new UserResponse(user.displayName(), user.email(), user.role());
    }
}
