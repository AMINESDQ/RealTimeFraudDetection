# Kafka Streams Fraud Detection

## Table of Contents

1. [Introduction](#introduction)
2. [Project Architecture](#project-architecture)
3. [Features](#features)
4. [Requirements](#requirements)
5. [Setup Instructions](#setup-instructions)
6. [Testing and Validation](#testing-and-validation)
7. [Grafana Configuration](#grafana-configuration)
8. [Conclusion](#conclusion)

---

## Introduction

This project demonstrates the implementation of a **real-time fraud detection application** using Kafka Streams. The application processes financial transactions from a Kafka topic, detects suspicious transactions based on predefined rules, stores these transactions in InfluxDB, and visualizes the data in Grafana.

## Project Architecture

```plaintext
                      +------------------+
                      |  Transactions   |
                      |      Source     |
                      +------------------+
                              |
                              v
                    +--------------------+
                    |   Kafka Topic:    |
                    | transactions-input |
                    +--------------------+
                              |
                              v
               +-----------------------------+
               |    Kafka Streams App        |
               |  Fraud Detection Rules      |
               +-----------------------------+
                              |
                              v
       +-----------------------+        +--------------------+
       | Kafka Topic:          |        |   InfluxDB         |
       | fraud-alerts          |        | (Time-Series DB)   |
       +-----------------------+        +--------------------+
                              |
                              v
                       +-------------+
                       |   Grafana   |
                       |  Dashboard  |
                       +-------------+
```

## Features

1. **Real-Time Processing**:
   - Reads financial transactions from the `transactions-input` topic.
   - Detects suspicious transactions where the `amount` exceeds 10,000.

2. **Kafka Streams Integration**:
   - Processes and publishes suspicious transactions to the `fraud-alerts` topic.

3. **Time-Series Database**:
   - Stores suspicious transactions in InfluxDB for analysis and visualization.

4. **Real-Time Dashboard**:
   - Grafana visualizes the following:
     - Number of suspicious transactions per user.
     - Total suspicious transaction amounts over time.

## Requirements

- **Kafka** (Confluent Kafka recommended)
- **Java JDK 17**
- **InfluxDB**
- **Grafana**
- **Docker and Docker Compose**

## Setup Instructions

### 1. Kafka Topics

Create the necessary Kafka topics:
```bash
# Create transactions-input topic
kafka-topics --create --topic transactions-input --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1

# Create fraud-alerts topic
kafka-topics --create --topic fraud-alerts --bootstrap-server localhost:9092 --partitions 3 --replication-factor 1
```

### 2. Run Kafka Streams Application

Build and run the Java application:
```bash
# Build the application
mvn clean package

# Run the application
java -jar target/fraud-detection-app.jar
```

### 3. Deploy InfluxDB and Grafana

Using Docker Compose, deploy the required services:

**`docker-compose.yml`:**
```yaml
version: '3.8'
networks:
  fraud-detection:
    driver: bridge

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    container_name: zookeeper
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181
    ports:
      - "2181:2181"
    networks:
      - fraud-detection

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    environment:
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://host.docker.internal:9092
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    ports:
      - "9092:9092"
    networks:
      - fraud-detection
    depends_on:
      - zookeeper

  influxdb:
    image: influxdb:2.0
    ports:
      - "8086:8086"
    networks:
      - fraud-detection

  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
    networks:
      - fraud-detection

  fraud-detection:
    image: detectionapp:latest
    networks:
      - fraud-detection
```

Start the containers:
```bash
docker-compose up -d
```

### 4. Configure InfluxDB

1. Access InfluxDB at `http://localhost:8086`.
2. Create a new database:
   ```bash
   CREATE DATABASE fraud_detection
   ```

### 5. Connect Kafka Streams to InfluxDB

Ensure your Kafka Streams application writes detected transactions to InfluxDB using the following schema:
- **Measurement:** `suspicious_transactions`
- **Tags:** `userId`, `transactionId`
- **Fields:** `amount`, `timestamp`

## Testing and Validation

1. **Simulate Transactions**:
   Produce sample JSON transactions to the `transactions-input` topic:
   ```bash
   kafka-console-producer --topic transactions-input --bootstrap-server localhost:9092
   > {"userId": "12345", "amount": 15000, "timestamp": "2024-12-04T15:00:00Z"}
   > {"userId": "67890", "amount": 5000, "timestamp": "2024-12-04T15:05:00Z"}
   ```

   Alternatively, you can use the following Python script:

   **`producer.py`:**
   ```python
   from confluent_kafka import Producer
   import json
   import time
   import random

   # Configuration de Kafka
   BOOTSTRAP_SERVERS = 'localhost:9092'
   TOPIC_INPUT = 'transactions-input'

   # Configuration pour 2000 transactions en 120 secondes
   TOTAL_TRANSACTIONS = 5000
   DURATION_SECONDS = 300
   INTERVAL = DURATION_SECONDS / TOTAL_TRANSACTIONS  # Intervalle entre les envois

   # Fonction pour produire des transactions
   def delivery_report(err, msg):
       """
       Callback pour signaler le succès ou l'échec de la livraison d'un message.
       """
       if err is not None:
           print(f"Échec de l'envoi du message : {err}")
       else:
           print(f"Message envoyé avec succès au topic {msg.topic()} [partition {msg.partition()}]")

   def produce_transactions():
       # Configuration du Producer Kafka
       producer_config = {
           'bootstrap.servers': BOOTSTRAP_SERVERS,
           'linger.ms': 10,  # Réduire la latence d'envoi
       }
       producer = Producer(producer_config)

       print(f"Envoi de {TOTAL_TRANSACTIONS} transactions au topic '{TOPIC_INPUT}' en {DURATION_SECONDS} secondes...")

       user_ids = ['11111', '22222', '33333', '44444', '55555']
       for i in range(TOTAL_TRANSACTIONS):
           user_id = random.choice(user_ids)
           amount = random.randint(1000, 20000)  # Montant entre 1000 et 20000
           timestamp = time.strftime('%Y-%m-%dT%H:%M:%SZ', time.gmtime())
           transaction = {
               "userId": user_id,
               "amount": amount,
               "timestamp": timestamp
           }

           # Envoi au topic Kafka
           try:
               producer.produce(
                   topic=TOPIC_INPUT,
                   key=str(user_id),
                   value=json.dumps(transaction),
                   callback=delivery_report
               )
           except Exception as e:
               print(f"Erreur lors de l'envoi : {e}")

           # Pause pour respecter l'intervalle
           time.sleep(INTERVAL)

       # Assurez-vous que tous les messages ont été transmis avant de fermer
       producer.flush()

       print("Toutes les transactions ont été envoyées avec succès.")

   if __name__ == '__main__':
       produce_transactions()
   ```

2. **Verify Fraud Detection**:
   - Check that suspicious transactions appear in the `fraud-alerts` topic.
   ```bash
   kafka-console-consumer --topic fraud-alerts --bootstrap-server localhost:9092 --from-beginning
   ```

3. **Validate InfluxDB Storage**:
   - Ensure suspicious transactions are being written to InfluxDB.
   - Query the database to confirm:
     ```bash
     influx
     > USE fraud_detection
     > SELECT * FROM suspicious_transactions
     ```

4. **Dashboard Testing**:
   - Access Grafana at `http://localhost:3000`.
   - Configure a data source for InfluxDB:
     - URL: `http://influxdb:8086`
     - Database: `fraud_detection`


---

## Grafana Configuration

### Dashboard Setup

1. **Create a Data Source**:
   - Navigate to **Configuration > Data Sources**.
   - Add a new InfluxDB data source with the following details:
     - URL: `http://influxdb:8086`
     - Database: `fraud_detection`

2. **Build Panels**:
   - Use the query editor to fetch data from InfluxDB:
     ```plaintext
     SELECT sum("amount") FROM "suspicious_transactions" WHERE $timeFilter GROUP BY time($__interval)
     ```

3. **Save the Dashboard**:
   - Customize titles, axes, and thresholds for better visualization.
   - Share the dashboard link for team access.

---

## Conclusion

This project highlights the power of Kafka Streams for real-time data processing and InfluxDB with Grafana for time-series analysis and visualization. By implementing a scalable and modular architecture, this solution can adapt to various fraud detection scenarios, offering organizations an effective tool to mitigate financial risks.

### Future Enhancements

- **Machine Learning Integration**:
  Use ML models to enhance fraud detection rules for more accuracy.

- **Alerting System**:
  Configure Grafana to send alerts via email or Slack for critical events.

- **Extended Monitoring**:
  Add Prometheus for system metrics monitoring alongside transaction analysis.

With these additions, the system can evolve into a comprehensive fraud detection and monitoring solution.

---

This completes the document with all necessary instructions, configurations, and future prospects. Let me know if you'd like additional sections or refinements!
```