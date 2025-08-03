package com.midlane.project_management_tool_auth_service.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(value = "file:.env", ignoreResourceNotFound = true)
public class EnvironmentConfig {
    // This class ensures that .env file is loaded into the Spring environment
    // The ignoreResourceNotFound = true means the application won't fail if .env doesn't exist
}
