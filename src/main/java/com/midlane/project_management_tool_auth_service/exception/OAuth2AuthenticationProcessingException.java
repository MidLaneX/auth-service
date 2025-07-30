package com.midlane.project_management_tool_auth_service.exception;

public class OAuth2AuthenticationProcessingException extends RuntimeException {

    public OAuth2AuthenticationProcessingException(String message) {
        super(message);
    }

    public OAuth2AuthenticationProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
