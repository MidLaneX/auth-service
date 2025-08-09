#!/bin/bash

# Create databases for each service
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE DATABASE auth_service_db;
    CREATE DATABASE project_service_db;
    CREATE DATABASE notification_service_db;

    -- Grant permissions
    GRANT ALL PRIVILEGES ON DATABASE auth_service_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE project_service_db TO $POSTGRES_USER;
    GRANT ALL PRIVILEGES ON DATABASE notification_service_db TO $POSTGRES_USER;

    -- Create schemas if needed
    \c auth_service_db;
    CREATE SCHEMA IF NOT EXISTS auth;

    \c project_service_db;
    CREATE SCHEMA IF NOT EXISTS projects;

    \c notification_service_db;
    CREATE SCHEMA IF NOT EXISTS notifications;
EOSQL

echo "Databases created successfully!"
