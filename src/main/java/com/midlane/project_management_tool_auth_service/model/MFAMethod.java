package com.midlane.project_management_tool_auth_service.model;

public enum MFAMethod {
    TOTP,           // Time-based One-Time Password (Google Authenticator, Authy)
    SMS,            // SMS verification
    EMAIL,          // Email verification
    BACKUP_CODES    // Recovery backup codes
}