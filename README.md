# 🚀 Java Backend for AI Infrastructure Monitoring

This Spring Boot (WebFlux) project provides the AI-powered root cause analysis and automated PR creation features for your infrastructure monitoring system. It connects with a FastAPI-based RCA microservice to summarize incidents and can open GitHub pull requests with fixes.

---

## 📦 Features

- GitHub OAuth (secure user authentication)
- REST APIs to submit logs and trigger RCA
- Auto-create pull requests for code fixes
- Supports multiple repositories per user
- Stores alerts with RCA summary, confidence, timestamp, PR URL
- Works asynchronously with a FastAPI Python microservice
- Uses Spring Data JPA for persistent storage
- Built with reactive programming (Spring WebFlux)

---

## ⚙️ Technologies

- Java 17+
- Spring Boot 3 (WebFlux)
- Spring Data JPA
- PostgreSQL (or H2 for development)
- FastAPI (Python RCA microservice)
- GitHub REST API
- Docker (optional)

---

## 📁 API Endpoints

### `GET /github/login`
Starts the GitHub OAuth flow

### `GET /github/callback`
GitHub returns to this endpoint after OAuth login

### `GET /github/is-connected?userId={id}`
Checks if a user is connected to GitHub

### `GET /github/user-repos?userId={id}`
Lists the user’s repositories from GitHub

### `POST /repo-mappings`
Create a repository mapping for PR workflows

### `GET /repo-mappings/{userId}`
List a user’s mapped repositories

### `POST /alerts?userId={id}&repoName={repo}`
Send a log for root cause analysis, store the alert, and (optionally) trigger a PR

### `GET /alerts?userId={id}`
Retrieve a user’s alerts from storage

---

## 🛠️ Setup Instructions

1. **Clone the repository**
```bash
git clone https://github.com/yourusername/ai-monitoring-backend.git
cd ai-monitoring-backend
```

2. **Set environment variables**
```bash
export GITHUB_CLIENT_ID=your_github_client_id
export GITHUB_CLIENT_SECRET=your_github_client_secret
export GITHUB_REDIRECT_URI=http://localhost:8080/github/callback
```

Or in `application.properties`:
```properties
github.client.id=your_github_client_id
github.client.secret=your_github_client_secret
github.redirect.uri=http://localhost:8080/github/callback
```

3. **Run the backend**
```bash
./mvnw spring-boot:run
```

---

## 🧪 Testing

You can write tests using:

- JUnit 5
- Spring Boot Test
- Mockito (for mocking GitHub calls or RCA)

Example:
```java
@WebFluxTest(AlertController.class)
public class AlertControllerTest {
    // tests here
}
```

Run all tests with:
```bash
./mvnw test
```

If you wish to run integration tests against a local Postgres:
- Spin up Postgres on `localhost:5432`
- Use `testcontainers` if you prefer ephemeral containers

---

## 🐳 Docker

To build and run with Docker:
```bash
docker build -t ai-monitoring-backend .
docker run -p 8080:8080 --env-file .env ai-monitoring-backend
```

---

## 🔗 Python RCA Microservice

This backend expects the Python RCA microservice to be running on:
```
http://localhost:8000/rca
```

---

## 🤝 Contributing

Feel free to submit issues, ideas, or pull requests.  
Let’s build a smarter, more resilient monitoring stack together! 💙

---

MIT License © 2025 YourName
