#!/bin/bash

# Project Management Tool - Multi-Service Setup Script
# This script helps you set up all services from different repositories

set -e

echo "🚀 Setting up Project Management Tool Multi-Service Environment"

# Configuration
SERVICES=(
    "auth-service:https://github.com/your-org/auth-service.git"
    "project-service:https://github.com/your-org/project-service.git"
    "notification-service:https://github.com/your-org/notification-service.git"
    "frontend:https://github.com/your-org/frontend.git"
)

# Function to clone or update repositories
setup_repositories() {
    echo "📂 Setting up repositories..."

    for service_info in "${SERVICES[@]}"; do
        IFS=':' read -r service_name repo_url <<< "$service_info"

        if [ -d "$service_name" ]; then
            echo "📄 Updating $service_name..."
            cd "$service_name"
            git pull origin main || git pull origin master
            cd ..
        else
            echo "📥 Cloning $service_name..."
            git clone "$repo_url" "$service_name"
        fi
    done
}

# Function to check prerequisites
check_prerequisites() {
    echo "🔍 Checking prerequisites..."

    # Check Docker
    if ! command -v docker &> /dev/null; then
        echo "❌ Docker is not installed. Please install Docker first."
        exit 1
    fi

    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo "❌ Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi

    echo "✅ Prerequisites check passed!"
}

# Function to setup environment files
setup_environment() {
    echo "⚙️ Setting up environment files..."

    if [ ! -f ".env" ]; then
        if [ -f ".env.docker" ]; then
            cp .env.docker .env
            echo "📋 Copied .env.docker to .env"
        else
            echo "⚠️ Please create a .env file with your configuration"
        fi
    fi

    echo "📝 Don't forget to update your email credentials in .env file!"
}

# Function to build and start services
start_services() {
    echo "🏗️ Building and starting services..."

    # Build images
    docker-compose build --no-cache

    # Start services
    docker-compose up -d

    echo "⏳ Waiting for services to be healthy..."
    sleep 30

    # Check service health
    echo "🏥 Checking service health..."
    docker-compose ps
}

# Function to show service URLs
show_urls() {
    echo ""
    echo "🌐 Service URLs:"
    echo "├── Frontend:           http://localhost:3000"
    echo "├── Auth Service:       http://localhost:8082"
    echo "├── Project Service:    http://localhost:8083"
    echo "├── Notification Service: http://localhost:8084"
    echo "├── API Gateway:        http://localhost"
    echo "└── PostgreSQL:         localhost:5434"
    echo ""
}

# Function to show useful commands
show_commands() {
    echo "📋 Useful Commands:"
    echo "├── View logs:          docker-compose logs -f [service-name]"
    echo "├── Stop services:      docker-compose down"
    echo "├── Restart service:    docker-compose restart [service-name]"
    echo "├── Rebuild service:    docker-compose up -d --build [service-name]"
    echo "└─��� Clean up:           docker-compose down -v --remove-orphans"
    echo ""
}

# Main execution
main() {
    check_prerequisites
    setup_repositories
    setup_environment
    start_services
    show_urls
    show_commands

    echo "🎉 Project Management Tool is now running!"
    echo "💡 Check the logs with: docker-compose logs -f"
}

# Handle script arguments
case "${1:-setup}" in
    "setup")
        main
        ;;
    "update")
        setup_repositories
        docker-compose up -d --build
        ;;
    "clean")
        docker-compose down -v --remove-orphans
        docker system prune -f
        ;;
    "logs")
        docker-compose logs -f "${2:-}"
        ;;
    *)
        echo "Usage: $0 {setup|update|clean|logs [service-name]}"
        exit 1
        ;;
esac
