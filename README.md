# 🚀 Java Backend for AI Infrastructure Monitoring

This Spring Boot project acts as the core orchestrator for telemetry ingestion, anomaly detection, and AI-powered root cause analysis. It connects to the Python-based GPT RCA microservice to generate intelligent incident summaries.

---

## 📦 Features

- REST API to accept telemetry logs
- Asynchronous call to Python RCA service (FastAPI + GPT or mock)
- Stores alerts in memory with summary, confidence, and timestamp
- Exposes alert list via REST for frontend use

---

## 📁 Endpoints

### `POST /telemetry`
Accepts logs and sends them to the RCA engine.

**Request Body:**
```json
{
  "log": "Service crash due to missing DB_PASSWORD env variable"
}
```

**Response:**
```json
{
  "originalLog": "...",
  "summary": "...",
  "confidence": 0.75,
  "timestamp": "2025-06-21T13:00:00Z"
}
```

---

### `GET /alerts`
Returns all past alerts (in memory).

---

## ⚙️ Technologies

- Java 17+
- Spring Boot (WebFlux)
- WebClient (for async HTTP to Python service)
- FastAPI (Python RCA microservice)
- OpenTelemetry-ready (future enhancement)

---

## 🔧 Setup Instructions

### Prerequisites:
- Java 17+
- Maven
- Python (with `uvicorn` and FastAPI running on port 8000)

### Run the Backend:
```bash
./mvnw spring-boot:run
```

---

## 🔗 RCA Microservice (Python)

Make sure your Python service is running at:
```
http://localhost:8000/rca
```

---

## 🤝 Contributions Welcome

Let's build an intelligent, resilient cloud monitoring stack! Pull requests, ideas, and feedback are welcome.