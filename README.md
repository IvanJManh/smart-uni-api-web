# 🏫 Smart Campus Web API

A RESTful web application for managing campus rooms and IoT sensors, built with **Java 11**, **JAX-RS (Jersey 2.x)**, and **Apache Tomcat**. The API enables real-time sensor monitoring across campus rooms with full CRUD support and structured error handling.

---

## 📋 Table of Contents

- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [API Endpoints](#api-endpoints)
- [Data Models](#data-models)
- [Error Handling](#error-handling)
- [Business Rules](#business-rules)
- [Running the App](#running-the-app)

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 11 |
| REST Framework | JAX-RS / Jersey 2.41 |
| JSON | Jackson (via Jersey Media) |
| DI Container | HK2 (bundled with Jersey) |
| Servlet Container | Apache Tomcat (WAR deployment) |
| Build Tool | Maven 3.x |
| Data Storage | In-memory (ConcurrentHashMap) |

---

## 📁 Project Structure

```
smart-campus-web/
├── pom.xml
└── src/
    └── main/
        ├── java/com/smartcampus/
        │   ├── SmartCampusApplication.java       # JAX-RS entry point (@ApplicationPath)
        │   ├── model/
        │   │   ├── Room.java                     # Room entity
        │   │   ├── Sensor.java                   # Sensor entity
        │   │   ├── SensorReading.java            # Reading entity
        │   │   └── ErrorResponse.java            # Error wrapper
        │   ├── resource/
        │   │   ├── RoomResource.java             # /api/v1/rooms
        │   │   ├── SensorResource.java           # /api/v1/sensors
        │   │   ├── SensorReadingResource.java    # /api/v1/sensors/{id}/readings
        │   │   └── DiscoveryResource.java        # /api/v1 (HATEOAS root)
        │   ├── store/
        │   │   └── DataStore.java                # Singleton in-memory data store
        │   ├── filter/
        │   │   └── LoggingFilter.java            # Request/response logger
        │   └── exception/
        │       ├── RoomNotEmptyException.java
        │       ├── SensorUnavailableException.java
        │       ├── LinkedResourceNotFoundException.java
        │       ├── GlobalExceptionMapper.java
        │       ├── RoomNotEmptyExceptionMapper.java
        │       ├── SensorUnavailableExceptionMapper.java
        │       └── LinkedResourceNotFoundExceptionMapper.java
        └── webapp/
            ├── index.html                        # Landing page
            └── WEB-INF/
                └── web.xml                       # Servlet configuration
```

---

## 🚀 Getting Started

### Prerequisites

- Java 11 or higher
- Maven 3.6+
- Apache Tomcat 9.x (or NetBeans with bundled Tomcat/GlassFish)

### Build

```bash
mvn clean package
```

This produces `target/smart-campus-web.war`.

### Deploy to Tomcat

Copy the WAR to your Tomcat `webapps/` folder:

```bash
cp target/smart-campus-web.war $TOMCAT_HOME/webapps/
```

Or deploy via the **Tomcat Manager UI** at `http://localhost:8080/manager`.

### Access the API

```
http://localhost:8080/smart-campus-web/api/v1
```

---

## 📡 API Endpoints

### Base URL
```
/api/v1
```

---

### 🚪 Rooms — `/api/v1/rooms`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/rooms` | Get all rooms |
| `GET` | `/rooms/{roomId}` | Get a room by ID |
| `POST` | `/rooms` | Create a new room |
| `DELETE` | `/rooms/{roomId}` | Delete a room (must have no sensors) |

**Create Room — Request Body:**
```json
{
  "id": "room-101",
  "name": "Lecture Hall A",
  "capacity": 120
}
```

---

### 📡 Sensors — `/api/v1/sensors`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/sensors` | Get all sensors |
| `GET` | `/sensors?type=CO2` | Filter sensors by type |
| `GET` | `/sensors/{sensorId}` | Get a sensor by ID |
| `POST` | `/sensors` | Register a new sensor |

**Create Sensor — Request Body:**
```json
{
  "id": "sensor-001",
  "type": "CO2",
  "status": "ACTIVE",
  "currentValue": 0.0,
  "roomId": "room-101"
}
```

> **Note:** The `roomId` must reference an existing room, or a `422 Unprocessable Entity` is returned.

---

### 📊 Sensor Readings — `/api/v1/sensors/{sensorId}/readings`

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/sensors/{sensorId}/readings` | Get all readings for a sensor |
| `POST` | `/sensors/{sensorId}/readings` | Submit a new reading |

**Add Reading — Request Body:**
```json
{
  "value": 412.5
}
```

> **Note:** Sensors with status `MAINTENANCE` will reject new readings with `503 Service Unavailable`.

---

## 🗂 Data Models

### Room
```json
{
  "id": "string",
  "name": "string",
  "capacity": 0,
  "sensorIds": ["string"]
}
```

### Sensor
```json
{
  "id": "string",
  "type": "string",
  "status": "ACTIVE | MAINTENANCE",
  "currentValue": 0.0,
  "roomId": "string"
}
```

### SensorReading
```json
{
  "id": "string",
  "value": 0.0,
  "timestamp": "ISO-8601"
}
```

---

## ⚠️ Error Handling

All errors return a JSON body with an `error` field:

```json
{
  "error": "Description of what went wrong"
}
```

| HTTP Status | Meaning |
|---|---|
| `400 Bad Request` | Missing or invalid request fields |
| `404 Not Found` | Resource does not exist |
| `409 Conflict` | Resource with that ID already exists |
| `422 Unprocessable Entity` | Referenced resource (e.g. roomId) does not exist |
| `503 Service Unavailable` | Sensor is under maintenance |

---

## 📏 Business Rules

1. **Room deletion** — A room cannot be deleted if it still has sensors assigned to it. Remove all sensors first.
2. **Sensor creation** — A sensor must reference a valid, existing `roomId`.
3. **Sensor readings** — A sensor with status `MAINTENANCE` cannot accept new readings.
4. **Default sensor status** — If `status` is not provided during creation, it defaults to `ACTIVE`.
5. **Current value sync** — When a new reading is submitted, the parent sensor's `currentValue` is automatically updated to the latest reading value.

---

## 💾 Data Storage

This application uses an **in-memory data store** (`DataStore.java`) backed by `ConcurrentHashMap` for thread safety. All data is lost when the server restarts. This design is suitable for development and testing purposes.

> To add persistence, replace `DataStore` with a JPA-based repository using a database like PostgreSQL or MySQL.

---

## 🧪 Example Usage (cURL)

```bash
# Create a room
curl -X POST http://localhost:8080/smart-campus-web/api/v1/rooms \
  -H "Content-Type: application/json" \
  -d '{"id":"room-101","name":"Lecture Hall A","capacity":120}'

# Register a sensor
curl -X POST http://localhost:8080/smart-campus-web/api/v1/sensors \
  -H "Content-Type: application/json" \
  -d '{"id":"sensor-001","type":"CO2","roomId":"room-101"}'

# Submit a sensor reading
curl -X POST http://localhost:8080/smart-campus-web/api/v1/sensors/sensor-001/readings \
  -H "Content-Type: application/json" \
  -d '{"value":412.5}'

# Get all readings for a sensor
curl http://localhost:8080/smart-campus-web/api/v1/sensors/sensor-001/readings
```

---

