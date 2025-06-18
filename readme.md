# 📘 Kafka Avro with Docker Compose (Ubuntu + WSL2)

This guide explains how to set up a complete Kafka Avro infrastructure using Docker Compose inside an Ubuntu subsystem (WSL2) on Windows.

---

## 🧰 Step 1: Install Ubuntu via WSL2

1. Open **Microsoft Store** and install "Ubuntu 22.04 LTS".
2. Once installed, launch Ubuntu from Start Menu.
3. Update packages:
   ```bash
   sudo apt update && sudo apt upgrade -y
   ```

---

## 🐳 Step 2: Install Docker in Ubuntu

1. Install Docker dependencies:

   ```bash
   sudo apt install apt-transport-https ca-certificates curl software-properties-common -y
   ```

2. Add Docker GPG key and repository:

   ```bash
   curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
   echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
   ```

3. Install Docker:

   ```bash
   sudo apt update
   sudo apt install docker-ce docker-ce-cli containerd.io -y
   ```

4. Enable Docker without `sudo`:

   ```bash
   sudo usermod -aG docker $USER
   newgrp docker
   ```

5. Test Docker:

   ```bash
   docker --version
   docker run hello-world
   ```

---

## 🧱 Step 3: Kafka Infrastructure with Docker Compose

We will use Docker Compose to run the following components:

- Zookeeper
- Kafka Broker (with Avro support)
- Schema Registry
- Kafka UI
- Schema Registry UI

### Create a `docker-compose.yml` file:

```yaml
version: '3.8'

services:
   zookeeper:
      image: confluentinc/cp-zookeeper:7.5.0
      container_name: zookeeper
      ports:
         - "7181:2181"
      environment:
         ZOOKEEPER_CLIENT_PORT: 2181
         ZOOKEEPER_TICK_TIME: 2000

   kafka:
      image: confluentinc/cp-kafka:7.5.0
      container_name: kafka
      depends_on:
         - zookeeper
      ports:
         - "7092:9092"
      environment:
         KAFKA_BROKER_ID: 1
         KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
         KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://localhost:7092,PLAINTEXT_INTERNAL://kafka:9092
         KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_INTERNAL:PLAINTEXT
         KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
         KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT_INTERNAL
         KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1

   schema-registry:
      image: confluentinc/cp-schema-registry:7.5.0
      container_name: schema-registry
      depends_on:
         - kafka
      ports:
         - "7081:8081"
      environment:
         SCHEMA_REGISTRY_HOST_NAME: schema-registry
         SCHEMA_REGISTRY_LISTENERS: http://0.0.0.0:8081
         SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS: PLAINTEXT://kafka:9092

   kafka-ui:
      image: provectuslabs/kafka-ui:latest
      container_name: kafka-ui
      ports:
         - "7080:8080"
      depends_on:
         - kafka
         - schema-registry
      environment:
         KAFKA_CLUSTERS_0_NAME: local-kafka
         KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
         KAFKA_CLUSTERS_0_ZOOKEEPER: zookeeper:2181
         KAFKA_CLUSTERS_0_SCHEMAREGISTRY: http://schema-registry:8081

   schema-registry-ui:
      image: landoop/schema-registry-ui:latest
      container_name: schema-registry-ui
      ports:
         - "7000:8000"
      depends_on:
         - schema-registry
      environment:
         SCHEMAREGISTRY_URL: http://schema-registry:8081
         PROXY: "true"
```

---

## 🚀 Bring Up the Infrastructure

```bash
docker compose down -v
docker compose up -d --build
```

---

✅ All containers will be accessible on the following ports:

- Kafka Broker: `localhost:7092`
- Zookeeper: `localhost:7181`
- Schema Registry: `localhost:7081`
- Kafka UI: `localhost:7080`
- Schema Registry UI: `localhost:7000`

---

## 🧪 Step 4: Create Kafka Topic (Avro Enabled)

After bringing up the Kafka infrastructure, we need to create a Kafka topic to publish and consume Avro-encoded messages.

Run the following command to create a topic:

```bash
docker exec -it kafka kafka-topics \
  --create \
  --topic test-avro-topic \
  --bootstrap-server localhost:7092 \
  --partitions 3 \
  --replication-factor 1
```

### 🔍 Explanation:

- `docker exec -it kafka`: Executes the command inside the Kafka container.
- `--create`: Tells Kafka to create a new topic.
- `--topic test-avro-topic`: The name of the topic.
- `--bootstrap-server localhost:7092`: Uses the exposed port of the Kafka broker to connect.
- `--partitions 3`: Splits the topic into 3 partitions (good for parallelism).
- `--replication-factor 1`: Sets replication to 1 since we only have one Kafka broker running in this setup.

✅ This topic will be used by the Spring Boot Producer to send Avro messages and the Consumer to read them.

---

## 🧬 Step 5: Register Avro Schema in Schema Registry

To publish Avro messages, you need to register your schema with the Schema Registry.

### ✅ Register Schema using `curl`

```bash
curl --location 'http://localhost:7081/subjects/test-avro-topic-value/versions' \
--header 'Content-Type: application/vnd.schemaregistry.v1+json' \
--data '{
  "schema": "{\"type\":\"record\",\"name\":\"User\",\"namespace\":\"com.sachin.kafka\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"age\",\"type\":\"int\"}]}"
}'
```

### 🔍 Explanation:

- `http://localhost:7081`: Points to the Schema Registry running inside Docker.
- `subjects/test-avro-topic-value/versions`: Registers schema for the value of topic `test-avro-topic`.
- `schema`: Avro schema JSON string for a `User` record with `name` and `age`.

---

## 🧪 Step 6: Run Spring Boot Producer and Consumers

You already have Spring Boot applications (`producer` and `consumer`) built as `.jar` files. Run them like this:

### ✅ Start Producer

```bash
java -jar kafka.producer-1.0.jar --server.port=8091 --spring.kafka.bootstrap-servers=localhost:7092
```

### ✅ Start Consumers (Multiple Instances)

```bash
java -jar kafka.consumer-1.0.jar --server.port=8092 --spring.application.name=kafka-consumer-first
java -jar kafka.consumer-1.0.jar --server.port=8093 --spring.application.name=kafka-consumer-second
java -jar kafka.consumer-1.0.jar --server.port=8094 --spring.application.name=kafka-consumer-third
```

### 🔍 Explanation:

- Each consumer runs on a different port with a unique `spring.application.name`.
- All connect to the same Kafka broker on `localhost:7092`.
- These apps will consume messages published by the producer to `test-avro-topic`.

---

## 🔗 GitHub Repository

📁 You can find the full source code and Spring Boot integration here:

👉 [Spring BootConsumer Repo](https://github.com/sachinnagode/kafka.consumer)

