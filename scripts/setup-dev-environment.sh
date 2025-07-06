#!/bin/bash
# DCBEV Development Environment Setup Script
# This script sets up the complete development environment for DCBEV

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Logging functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Print banner
print_banner() {
    echo -e "${BLUE}"
    echo "=============================================="
    echo "    DCBEV Development Environment Setup"
    echo "=============================================="
    echo -e "${NC}"
}

# Check prerequisites
check_prerequisites() {
    log_info "Checking prerequisites..."
    
    local missing_tools=()
    
    # Check for required tools
    if ! command_exists git; then
        missing_tools+=("git")
    fi
    
    if ! command_exists python3; then
        missing_tools+=("python3")
    fi
    
    if ! command_exists pip3; then
        missing_tools+=("pip3")
    fi
    
    if ! command_exists node; then
        missing_tools+=("node")
    fi
    
    if ! command_exists npm; then
        missing_tools+=("npm")
    fi
    
    if ! command_exists docker; then
        missing_tools+=("docker")
    fi
    
    if ! command_exists docker-compose; then
        missing_tools+=("docker-compose")
    fi
    
    if [ ${#missing_tools[@]} -gt 0 ]; then
        log_error "Missing required tools: ${missing_tools[*]}"
        log_info "Please install the missing tools and run this script again."
        exit 1
    fi
    
    log_success "All prerequisites are installed"
}

# Setup environment files
setup_environment_files() {
    log_info "Setting up environment configuration files..."
    
    # Backend environment
    if [ ! -f "src/backend/.env" ]; then
        cp config/environments/.env.example src/backend/.env
        log_success "Created backend .env file from template"
        log_warning "Please edit src/backend/.env with your actual configuration values"
    else
        log_info "Backend .env file already exists"
    fi
    
    # Android local properties
    if [ ! -f "src/android/local.properties" ]; then
        cat > src/android/local.properties << EOF
# Android local properties
sdk.dir=/path/to/your/android/sdk

# Backend configuration for development
backend.url.debug=http://10.0.2.2:8000
backend.url.release=https://your-production-backend.com

# API Keys (Development)
openai.api.key=your_openai_api_key_here
dealerscloud.api.key=your_dealerscloud_api_key_here

# Firebase configuration
firebase.project.id=your_firebase_project_id
EOF
        log_success "Created Android local.properties file from template"
        log_warning "Please edit src/android/local.properties with your actual configuration values"
    else
        log_info "Android local.properties file already exists"
    fi
}

# Setup Python backend
setup_backend() {
    log_info "Setting up Python backend..."
    
    cd src/backend
    
    # Create virtual environment
    if [ ! -d "venv" ]; then
        log_info "Creating Python virtual environment..."
        python3 -m venv venv
        log_success "Created virtual environment"
    fi
    
    # Activate virtual environment
    source venv/bin/activate
    
    # Upgrade pip
    pip install --upgrade pip
    
    # Install dependencies
    if [ -f "requirements.txt" ]; then
        log_info "Installing Python dependencies..."
        pip install -r requirements.txt
        log_success "Installed production dependencies"
    fi
    
    if [ -f "requirements-dev.txt" ]; then
        log_info "Installing development dependencies..."
        pip install -r requirements-dev.txt
        log_success "Installed development dependencies"
    fi
    
    # Setup database
    log_info "Setting up database..."
    if command_exists alembic; then
        alembic upgrade head
        log_success "Database migrations applied"
    else
        log_warning "Alembic not found, skipping database setup"
    fi
    
    cd ../..
}

# Setup Android development
setup_android() {
    log_info "Setting up Android development environment..."
    
    # Check for Android SDK
    if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
        log_warning "ANDROID_HOME or ANDROID_SDK_ROOT not set"
        log_info "Please set up Android SDK and environment variables"
    else
        log_success "Android SDK environment detected"
    fi
    
    # Check for Java
    if ! command_exists java; then
        log_warning "Java not found. Please install Java 17 or later"
    else
        java_version=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2)
        log_success "Java version: $java_version"
    fi
    
    cd src/android
    
    # Make gradlew executable
    if [ -f "gradlew" ]; then
        chmod +x gradlew
        log_success "Made gradlew executable"
    fi
    
    cd ../..
}

# Setup Docker development
setup_docker() {
    log_info "Setting up Docker development environment..."
    
    # Check if Docker is running
    if ! docker info >/dev/null 2>&1; then
        log_warning "Docker is not running. Please start Docker and run this script again."
        return 1
    fi
    
    # Build development images
    log_info "Building Docker images..."
    cd docker
    docker-compose build
    log_success "Docker images built successfully"
    cd ..
}

# Setup development tools
setup_dev_tools() {
    log_info "Setting up development tools..."
    
    # Install pre-commit hooks if available
    if command_exists pre-commit; then
        pre-commit install
        log_success "Pre-commit hooks installed"
    else
        log_info "Installing pre-commit..."
        pip3 install pre-commit
        pre-commit install
        log_success "Pre-commit installed and hooks configured"
    fi
    
    # Setup code formatting tools
    if command_exists npm; then
        log_info "Installing JavaScript/TypeScript tools..."
        npm install -g prettier eslint
        log_success "Code formatting tools installed"
    fi
}

# Create necessary directories
create_directories() {
    log_info "Creating necessary directories..."
    
    directories=(
        "logs"
        "uploads"
        "backups"
        "tmp"
        "src/backend/uploads"
        "src/backend/logs"
        "tests/fixtures"
        "docs/api"
        "examples"
    )
    
    for dir in "${directories[@]}"; do
        if [ ! -d "$dir" ]; then
            mkdir -p "$dir"
            log_info "Created directory: $dir"
        fi
    done
    
    log_success "All directories created"
}

# Setup git hooks
setup_git_hooks() {
    log_info "Setting up Git hooks..."
    
    # Copy commit message template
    if [ -f ".github/COMMIT_TEMPLATE.md" ]; then
        git config commit.template .github/COMMIT_TEMPLATE.md
        log_success "Git commit template configured"
    fi
    
    # Set up git aliases
    git config alias.co checkout
    git config alias.br branch
    git config alias.ci commit
    git config alias.st status
    
    log_success "Git configuration updated"
}

# Run tests
run_initial_tests() {
    log_info "Running initial tests..."
    
    # Backend tests
    cd src/backend
    if [ -d "venv" ]; then
        source venv/bin/activate
        if command_exists pytest; then
            pytest tests/ -v --tb=short
            log_success "Backend tests passed"
        else
            log_warning "pytest not found, skipping backend tests"
        fi
    fi
    cd ../..
    
    # Android tests
    cd src/android
    if [ -f "gradlew" ]; then
        ./gradlew test
        log_success "Android tests passed"
    fi
    cd ../..
}

# Display next steps
show_next_steps() {
    echo -e "${GREEN}"
    echo "=============================================="
    echo "    ? Setup Complete!"
    echo "=============================================="
    echo -e "${NC}"
    
    echo "Next steps:"
    echo "1. Edit configuration files with your actual values:"
    echo "   - src/backend/.env"
    echo "   - src/android/local.properties"
    echo ""
    echo "2. Start the development environment:"
    echo "   - Backend: cd src/backend && source venv/bin/activate && python -m uvicorn main:app --reload"
    echo "   - Docker: cd docker && docker-compose up -d"
    echo ""
    echo "3. Open Android Studio and import the src/android project"
    echo ""
    echo "4. Visit the documentation at docs/ for detailed guides"
    echo ""
    echo "5. Check the README.md for additional setup instructions"
    echo ""
    echo -e "${BLUE}Happy coding! ?${NC}"
}

# Main execution
main() {
    print_banner
    
    # Run setup steps
    check_prerequisites
    create_directories
    setup_environment_files
    setup_backend
    setup_android
    setup_docker
    setup_dev_tools
    setup_git_hooks
    
    # Optional: run tests
    if [ "$1" = "--with-tests" ]; then
        run_initial_tests
    fi
    
    show_next_steps
}

# Execute main function with all arguments
main "$@"