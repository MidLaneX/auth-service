# Project Management Tool - Auth Service Deployment Guide

## Table of Contents
1. [Prerequisites](#prerequisites)
2. [Local Development Setup](#local-development-setup)
3. [Docker Single Service](#docker-single-service)
4. [Multi-Service Docker Deployment](#multi-service-docker-deployment)
5. [Kubernetes Deployment](#kubernetes-deployment)
6. [Cloud Deployment](#cloud-deployment)
7. [CI/CD Pipeline](#cicd-pipeline)
8. [Troubleshooting](#troubleshooting)

## Prerequisites

### Required Software
- **Docker Desktop** (4.20+)
- **Docker Compose** (2.20+)
- **kubectl** (1.27+)
- **Java 21** (for local development)
- **Maven 3.9+** (for building)
- **Git**

### Cloud Platforms (Optional)
- **AWS CLI** (for EKS)
- **Google Cloud SDK** (for GKE)
- **Azure CLI** (for AKS)

## Local Development Setup

### 1. Environment Configuration
```bash
# Clone the repository
git clone <your-auth-service-repo>
cd auth-service

# Copy environment template
cp .env.example .env

# Edit environment variables
notepad .env  # Windows
# or
vim .env      # Linux/Mac
```

### 2. Required Environment Variables
```env
# Database
POSTGRES_DB=auth_db
POSTGRES_USER=auth_user
POSTGRES_PASSWORD=your_password
POSTGRES_HOST=localhost
POSTGRES_PORT=5432

# JWT Configuration
JWT_SECRET=your-super-secret-key-at-least-256-bits-long
JWT_EXPIRATION=86400

# Email Configuration
SPRING_MAIL_USERNAME=your-email@gmail.com
SPRING_MAIL_PASSWORD=your-app-password

# Social Auth
GOOGLE_CLIENT_ID=your-google-client-id
FACEBOOK_CLIENT_ID=your-facebook-client-id

# Server Configuration
SERVER_PORT=8081
```

### 3. Local Database Setup
```bash
# Start PostgreSQL with Docker
docker run --name auth-postgres \
  -e POSTGRES_DB=auth_db \
  -e POSTGRES_USER=auth_user \
  -e POSTGRES_PASSWORD=your_password \
  -p 5432:5432 \
  -d postgres:15

# Or use the provided docker-compose for development
docker-compose -f docker-compose.dev.yml up -d
```

## Docker Single Service

### 1. Build Docker Image
```bash
# Build the application JAR
./mvnw clean package -DskipTests

# Build Docker image
docker build -t auth-service:latest .

# Tag for registry (optional)
docker tag auth-service:latest your-registry/auth-service:v1.0.0
```

### 2. Run Single Container
```bash
# Run with environment variables
docker run -d \
  --name auth-service \
  --env-file .env \
  -p 8081:8081 \
  auth-service:latest

# Or with inline environment variables
docker run -d \
  --name auth-service \
  -e POSTGRES_HOST=host.docker.internal \
  -e POSTGRES_PORT=5432 \
  -e POSTGRES_DB=auth_db \
  -e POSTGRES_USER=auth_user \
  -e POSTGRES_PASSWORD=your_password \
  -e JWT_SECRET=your-super-secret-key-at-least-256-bits-long \
  -p 8081:8081 \
  auth-service:latest
```

### 3. Verify Deployment
```bash
# Check container status
docker ps

# View logs
docker logs auth-service

# Test health endpoint
curl http://localhost:8081/api/auth/initial/health
```

## Multi-Service Docker Deployment

Since this is a polyrepo setup, here's how to coordinate multiple services:

### 1. Create Multi-Service Docker Compose

Create `docker-compose.multi-service.yml` in a coordination repository:

```yaml
version: '3.8'

services:
  # Database
  postgres:
    image: postgres:15
    environment:
      POSTGRES_DB: project_management_db
      POSTGRES_USER: pm_user
      POSTGRES_PASSWORD: pm_password
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    ports:
      - "5432:5432"
    networks:
      - pm-network

  # Auth Service
  auth-service:
    image: your-registry/auth-service:latest
    environment:
      POSTGRES_HOST: postgres
      POSTGRES_PORT: 5432
      POSTGRES_DB: auth_db
      POSTGRES_USER: pm_user
      POSTGRES_PASSWORD: pm_password
      JWT_SECRET: your-super-secret-key-at-least-256-bits-long
      SERVER_PORT: 8081
    ports:
      - "8081:8081"
    depends_on:
      - postgres
    networks:
      - pm-network

  # Project Service (example)
  project-service:
    image: your-registry/project-service:latest
    environment:
      POSTGRES_HOST: postgres
      POSTGRES_PORT: 5432
      POSTGRES_DB: project_db
      AUTH_SERVICE_URL: http://auth-service:8081
      SERVER_PORT: 8082
    ports:
      - "8082:8082"
    depends_on:
      - postgres
      - auth-service
    networks:
      - pm-network

  # Task Service (example)
  task-service:
    image: your-registry/task-service:latest
    environment:
      POSTGRES_HOST: postgres
      POSTGRES_PORT: 5432
      POSTGRES_DB: task_db
      AUTH_SERVICE_URL: http://auth-service:8081
      PROJECT_SERVICE_URL: http://project-service:8082
      SERVER_PORT: 8083
    ports:
      - "8083:8083"
    depends_on:
      - postgres
      - auth-service
      - project-service
    networks:
      - pm-network

  # API Gateway (optional)
  nginx:
    image: nginx:alpine
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    ports:
      - "80:80"
    depends_on:
      - auth-service
      - project-service
      - task-service
    networks:
      - pm-network

volumes:
  postgres_data:

networks:
  pm-network:
    driver: bridge
```

### 2. Build and Deploy All Services
```bash
# Create a deployment script
cat > deploy-all.sh << 'EOF'
#!/bin/bash

# Array of service repositories
services=(
    "auth-service"
    "project-service"
    "task-service"
    "notification-service"
)

# Build and push each service
for service in "${services[@]}"; do
    echo "Building $service..."
    cd ../$service
    ./mvnw clean package -DskipTests
    docker build -t your-registry/$service:latest .
    docker push your-registry/$service:latest
    cd ../deployment
done

# Deploy with docker-compose
docker-compose -f docker-compose.multi-service.yml down
docker-compose -f docker-compose.multi-service.yml pull
docker-compose -f docker-compose.multi-service.yml up -d

echo "All services deployed!"
EOF

chmod +x deploy-all.sh
./deploy-all.sh
```

## Kubernetes Deployment

### 1. Create Kubernetes Manifests

Create `k8s/` directory with the following files:

#### Namespace (`k8s/namespace.yaml`)
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: project-management
```

#### ConfigMap (`k8s/configmap.yaml`)
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: project-management
data:
  POSTGRES_HOST: "postgres-service"
  POSTGRES_PORT: "5432"
  POSTGRES_DB: "auth_db"
  SERVER_PORT: "8081"
```

#### Secret (`k8s/secret.yaml`)
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: project-management
type: Opaque
data:
  # Base64 encoded values
  POSTGRES_USER: cG1fdXNlcg==  # pm_user
  POSTGRES_PASSWORD: cG1fcGFzc3dvcmQ=  # pm_password
  JWT_SECRET: eW91ci1zdXBlci1zZWNyZXQta2V5LWF0LWxlYXN0LTI1Ni1iaXRzLWxvbmc=
```

#### PostgreSQL Deployment (`k8s/postgres.yaml`)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: project-management
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15
        env:
        - name: POSTGRES_DB
          value: "project_management_db"
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: POSTGRES_USER
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: app-secrets
              key: POSTGRES_PASSWORD
        ports:
        - containerPort: 5432
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: project-management
spec:
  selector:
    app: postgres
  ports:
  - port: 5432
    targetPort: 5432
---
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: project-management
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
```

#### Auth Service Deployment (`k8s/auth-service.yaml`)
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: auth-service
  namespace: project-management
spec:
  replicas: 2
  selector:
    matchLabels:
      app: auth-service
  template:
    metadata:
      labels:
        app: auth-service
    spec:
      containers:
      - name: auth-service
        image: your-registry/auth-service:latest
        envFrom:
        - configMapRef:
            name: app-config
        - secretRef:
            name: app-secrets
        ports:
        - containerPort: 8081
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8081
          initialDelaySeconds: 20
          periodSeconds: 5
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: auth-service
  namespace: project-management
spec:
  selector:
    app: auth-service
  ports:
  - port: 8081
    targetPort: 8081
  type: ClusterIP
```

#### Ingress (`k8s/ingress.yaml`)
```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: project-management-ingress
  namespace: project-management
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/ssl-redirect: "false"
spec:
  rules:
  - host: your-domain.com
    http:
      paths:
      - path: /api/auth
        pathType: Prefix
        backend:
          service:
            name: auth-service
            port:
              number: 8081
      - path: /api/projects
        pathType: Prefix
        backend:
          service:
            name: project-service
            port:
              number: 8082
```

### 2. Deploy to Kubernetes
```bash
# Create namespace and apply manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/auth-service.yaml
kubectl apply -f k8s/ingress.yaml

# Check deployment status
kubectl get pods -n project-management
kubectl get services -n project-management

# View logs
kubectl logs -f deployment/auth-service -n project-management
```

### 3. Local Kubernetes (minikube/kind)
```bash
# Start minikube
minikube start

# Enable ingress
minikube addons enable ingress

# Deploy applications
kubectl apply -f k8s/

# Get service URL
minikube service auth-service -n project-management --url
```

## Cloud Deployment

### AWS EKS
```bash
# Create EKS cluster
eksctl create cluster --name pm-cluster --region us-west-2 --nodes 3

# Update kubeconfig
aws eks update-kubeconfig --region us-west-2 --name pm-cluster

# Deploy applications
kubectl apply -f k8s/

# Create Load Balancer
kubectl apply -f - <<EOF
apiVersion: v1
kind: Service
metadata:
  name: auth-service-lb
  namespace: project-management
  annotations:
    service.beta.kubernetes.io/aws-load-balancer-type: nlb
spec:
  type: LoadBalancer
  selector:
    app: auth-service
  ports:
  - port: 80
    targetPort: 8081
EOF
```

### Google GKE
```bash
# Create GKE cluster
gcloud container clusters create pm-cluster \
  --zone us-central1-a \
  --num-nodes 3

# Get credentials
gcloud container clusters get-credentials pm-cluster --zone us-central1-a

# Deploy applications
kubectl apply -f k8s/
```

### Azure AKS
```bash
# Create resource group
az group create --name pm-rg --location eastus

# Create AKS cluster
az aks create \
  --resource-group pm-rg \
  --name pm-cluster \
  --node-count 3 \
  --enable-addons monitoring \
  --generate-ssh-keys

# Get credentials
az aks get-credentials --resource-group pm-rg --name pm-cluster

# Deploy applications
kubectl apply -f k8s/
```

## CI/CD Pipeline

### GitHub Actions (`.github/workflows/deploy.yml`)
```yaml
name: Build and Deploy

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
    - name: Checkout repository
      uses: actions/checkout@v3

    - name: Set up JDK 21
      uses: actions/setup-java@v3
      with:
        java-version: '21'
        distribution: 'temurin'

    - name: Cache Maven packages
      uses: actions/cache@v3
      with:
        path: ~/.m2
        key: ${{ runner.os }}-m2-${{ hashFiles('**/pom.xml') }}

    - name: Run tests
      run: ./mvnw test

    - name: Build application
      run: ./mvnw clean package -DskipTests

    - name: Log in to Container Registry
      uses: docker/login-action@v2
      with:
        registry: ${{ env.REGISTRY }}
        username: ${{ github.actor }}
        password: ${{ secrets.GITHUB_TOKEN }}

    - name: Extract metadata
      id: meta
      uses: docker/metadata-action@v4
      with:
        images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}

    - name: Build and push Docker image
      uses: docker/build-push-action@v4
      with:
        context: .
        push: true
        tags: ${{ steps.meta.outputs.tags }}
        labels: ${{ steps.meta.outputs.labels }}

  deploy:
    needs: build
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'

    steps:
    - name: Checkout repository
      uses: actions/checkout@v3

    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        aws-access-key-id: ${{ secrets.AWS_ACCESS_KEY_ID }}
        aws-secret-access-key: ${{ secrets.AWS_SECRET_ACCESS_KEY }}
        aws-region: us-west-2

    - name: Update kubeconfig
      run: aws eks update-kubeconfig --name pm-cluster

    - name: Deploy to Kubernetes
      run: |
        kubectl set image deployment/auth-service auth-service=${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:main -n project-management
        kubectl rollout status deployment/auth-service -n project-management
```

## Troubleshooting

### Common Issues

#### 1. Database Connection Issues
```bash
# Check database connectivity
kubectl exec -it deployment/auth-service -n project-management -- nc -zv postgres-service 5432

# Check database logs
kubectl logs deployment/postgres -n project-management
```

#### 2. Service Discovery Issues
```bash
# Check service endpoints
kubectl get endpoints -n project-management

# Test internal connectivity
kubectl exec -it deployment/auth-service -n project-management -- curl http://postgres-service:5432
```

#### 3. Memory/CPU Issues
```bash
# Check resource usage
kubectl top pods -n project-management

# Describe pod for events
kubectl describe pod <pod-name> -n project-management
```

#### 4. Build Issues
```bash
# Check Java version
java -version

# Clean build
./mvnw clean install -DskipTests

# Check Docker build context
docker build --no-cache -t auth-service:debug .
```

### Monitoring and Logging

#### Setup Prometheus and Grafana
```bash
# Add Helm repos
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo update

# Install Prometheus
helm install prometheus prometheus-community/kube-prometheus-stack -n monitoring --create-namespace

# Port forward to access Grafana
kubectl port-forward svc/prometheus-grafana 3000:80 -n monitoring
```

#### Application Logs
```bash
# Stream logs from all pods
kubectl logs -f -l app=auth-service -n project-management

# Get logs from specific pod
kubectl logs <pod-name> -n project-management --previous
```

## Quick Commands Reference

### Docker Commands
```bash
# Build and run locally
docker build -t auth-service .
docker run -p 8081:8081 auth-service

# Multi-service deployment
docker-compose -f docker-compose.multi-service.yml up -d

# Clean up
docker system prune -a
```

### Kubernetes Commands
```bash
# Deploy everything
kubectl apply -f k8s/

# Check status
kubectl get all -n project-management

# Scale service
kubectl scale deployment auth-service --replicas=3 -n project-management

# Update image
kubectl set image deployment/auth-service auth-service=new-image:tag -n project-management

# Delete everything
kubectl delete namespace project-management
```

### Development Commands
```bash
# Run locally
./mvnw spring-boot:run

# Run tests
./mvnw test

# Package application
./mvnw clean package

# Skip tests
./mvnw clean package -DskipTests
```

---

## Support

For issues and questions:
1. Check the [Troubleshooting](#troubleshooting) section
2. Review application logs
3. Open an issue in the repository
4. Contact the development team

---

**Last Updated:** August 2025
