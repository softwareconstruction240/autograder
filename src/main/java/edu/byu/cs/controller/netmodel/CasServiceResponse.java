package edu.byu.cs.controller.netmodel;

public class CasServiceResponse {

    private AuthenticationSuccess authenticationSuccess;

    public AuthenticationSuccess getAuthenticationSuccess() {
        return authenticationSuccess;
    }

    public void setAuthenticationSuccess(AuthenticationSuccess authenticationSuccess) {
        this.authenticationSuccess = authenticationSuccess;
    }


    public static class AuthenticationSuccess {

        private String user;

        public String getUser() {
            return user;
        }

        public void setUser(String user) {
            this.user = user;
        }
    }
}
