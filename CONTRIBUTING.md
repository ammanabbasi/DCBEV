# Contributing to DCBEV

Thank you for your interest in contributing to DCBEV! This guide will help you get started with contributing to our enterprise-grade AI assistant for automotive dealerships.

## ? Getting Started

### Prerequisites
- **Git**: Latest version
- **Android Studio**: Arctic Fox or later
- **Python**: 3.9+
- **Node.js**: 16+
- **Docker**: For containerized development

### Development Setup
```bash
# Fork and clone the repository
git clone https://github.com/YOUR_USERNAME/DCBEV.git
cd DCBEV

# Set up development environment
./scripts/setup-dev-environment.sh

# Create your feature branch
git checkout -b feature/your-feature-name
```

## ? Development Workflow

### 1. Issue Creation
- Check existing issues before creating new ones
- Use appropriate issue templates
- Provide clear description and acceptance criteria
- Add relevant labels and milestones

### 2. Branch Strategy
```bash
# Feature branches
git checkout -b feature/chat-streaming-improvements

# Bug fixes
git checkout -b fix/auth-token-expiry

# Documentation
git checkout -b docs/api-documentation-update

# Hotfixes
git checkout -b hotfix/critical-security-patch
```

### 3. Code Standards

#### Android/Kotlin
```kotlin
// Use descriptive variable names
private val chatRepository: ChatRepository by lazy { ... }

// Follow dependency injection patterns
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ChatRepository
) : ViewModel() { ... }

// Use coroutines for async operations
suspend fun sendMessage(message: String) = viewModelScope.launch { ... }
```

#### Python/Backend
```python
# Follow PEP 8 style guide
from typing import Optional, List
from pydantic import BaseModel

class ChatRequest(BaseModel):
    message: str
    session_id: Optional[str] = None
    dealership_id: str

# Use type hints
async def process_chat_request(request: ChatRequest) -> ChatResponse:
    """Process incoming chat request with proper error handling."""
    try:
        # Implementation
        pass
    except Exception as e:
        logger.error(f"Error processing chat request: {e}")
        raise
```

### 4. Testing Requirements

#### Minimum Test Coverage
- **Unit Tests**: 80% minimum
- **Integration Tests**: Critical paths covered
- **E2E Tests**: Main user flows

#### Running Tests
```bash
# Android tests
./gradlew test

# Backend tests
cd backend && python -m pytest --cov=./ --cov-report=term-missing

# Full test suite
./scripts/run-tests.sh
```

### 5. Code Quality Checks

#### Pre-commit Hooks
```bash
# Install pre-commit hooks
pre-commit install

# Run manually
pre-commit run --all-files
```

#### Static Analysis
```bash
# Android - ktlint
./gradlew ktlintCheck

# Python - flake8, mypy
flake8 backend/
mypy backend/
```

## ? Code Review Process

### Before Submitting PR
- [ ] Code follows style guidelines
- [ ] Tests are written and passing
- [ ] Documentation is updated
- [ ] Security considerations addressed
- [ ] Performance impact assessed

### PR Requirements
- **Title**: Clear, descriptive title
- **Description**: Detailed description using PR template
- **Tests**: Include test results and coverage
- **Documentation**: Update relevant docs
- **Breaking Changes**: Clearly marked and documented

### Review Criteria
- **Functionality**: Does it work as expected?
- **Code Quality**: Is it readable and maintainable?
- **Performance**: Any performance implications?
- **Security**: Are there security considerations?
- **Tests**: Adequate test coverage?

## ? Documentation Standards

### Code Documentation
```kotlin
/**
 * Streams chat responses from the AI service
 * @param message User's input message
 * @param sessionId Optional session identifier
 * @return Flow of chat response chunks
 */
suspend fun streamChatResponse(
    message: String,
    sessionId: String? = null
): Flow<ChatResponseChunk>
```

### API Documentation
- Use OpenAPI/Swagger for REST APIs
- Include request/response examples
- Document error codes and responses
- Provide integration examples

### Architecture Documentation
- Keep architecture diagrams updated
- Document design decisions
- Explain integration patterns
- Include deployment guides

## ?? Architecture Guidelines

### Android Architecture
```
UI Layer (Compose) ? ViewModel ? Repository ? API Client
                                    ?
                               Local Database (Room)
```

### Backend Architecture
```
API Routes ? Services ? Repositories ? Database
     ?
External APIs (OpenAI, DealersCloud)
```

### Key Principles
- **Single Responsibility**: Each class has one purpose
- **Dependency Injection**: Use Hilt for Android, FastAPI DI for backend
- **Error Handling**: Comprehensive error handling at all layers
- **Async/Await**: Use coroutines (Kotlin) and async/await (Python)

## ? Security Guidelines

### Authentication
- Never hardcode credentials
- Use environment variables for secrets
- Implement proper token validation
- Follow OAuth2/JWT best practices

### Data Protection
- Sanitize all user inputs
- Use parameterized queries
- Encrypt sensitive data
- Implement proper logging (no sensitive data)

### API Security
- Rate limiting on all endpoints
- Input validation
- HTTPS only
- Proper CORS configuration

## ? Performance Guidelines

### Android Performance
- Use `LazyColumn` for large lists
- Implement proper state management
- Optimize image loading
- Monitor memory usage

### Backend Performance
- Database query optimization
- Async processing for long operations
- Proper caching strategies
- Resource cleanup

## ? Bug Reporting

### Bug Report Template
```markdown
**Bug Description**: Clear description of the issue
**Steps to Reproduce**: Detailed steps
**Expected Behavior**: What should happen
**Actual Behavior**: What actually happens
**Environment**: OS, versions, device info
**Screenshots**: If applicable
**Additional Context**: Any other relevant information
```

### Priority Levels
- **Critical**: Security issues, data loss, system down
- **High**: Major functionality broken
- **Medium**: Minor functionality issues
- **Low**: Cosmetic issues, enhancements

## ? Feature Requests

### Feature Request Template
```markdown
**Feature Description**: Clear description of the feature
**Use Case**: Why is this needed?
**Acceptance Criteria**: What defines "done"?
**Implementation Ideas**: Suggested approach
**Additional Context**: Any other relevant information
```

## ? Communication

### Channels
- **GitHub Issues**: Bug reports, feature requests
- **GitHub Discussions**: General discussions, Q&A
- **Pull Requests**: Code review discussions
- **Email**: security@dcbev.com for security issues

### Communication Guidelines
- Be respectful and professional
- Provide clear, detailed information
- Use appropriate channels
- Follow up on issues and PRs

## ? Recognition

### Contributors
- Contributors are recognized in README
- Outstanding contributions highlighted in releases
- Community voting for significant contributions

### Contribution Types
- **Code**: New features, bug fixes, improvements
- **Documentation**: API docs, guides, tutorials
- **Testing**: Test coverage, test automation
- **Design**: UI/UX improvements, design systems
- **Community**: Helping other contributors, issue triage

## ? License

By contributing to DCBEV, you agree that your contributions will be licensed under the MIT License.

---

**Thank you for contributing to DCBEV! ?**