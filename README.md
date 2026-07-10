# Readit - Local Development Infrastructure

This Docker Compose stack runs the backing services the team needs while
building the Readit application. The Spring Boot + Vaadin app runs separately
from your IDE and connects to these services.

## Services

| Service    | Image                            | Host port | Purpose                                                        |
|------------|----------------------------------|-----------|----------------------------------------------------------------|
| PostgreSQL | postgres:16-alpine               | 5432      | Primary database (users, posts, comments, votes, outbox, etc.) |
| Redis      | redis:7-alpine                   | 6379      | Cache, CQRS feed views, session store                          |
| Kafka      | confluentinc/cp-kafka:7.9.0      | 9092      | Event streaming                                                |
| Zookeeper  | confluentinc/cp-zookeeper:7.9.0  | (internal)| Coordination service for Kafka                                 |
| Kafka UI   | ghcr.io/kafbat/kafka-ui:latest   | 8081      | Optional dashboard (only with the `tools` profile)             |

## Prerequisites

- Docker Desktop installed and running

## Quick start

```bash
# from the folder that contains docker-compose.yml
docker compose up -d

# check everything is up and healthy
docker compose ps
```

Wait until Postgres, Redis, and Kafka all show `healthy`. Kafka takes the
longest (it waits for Zookeeper first).

To also start the optional Kafka dashboard:

```bash
docker compose --profile tools up -d
# then open http://localhost:8080
```

## Verifying each service

```bash
# PostgreSQL
docker compose exec postgres psql -U readit -d readit -c "\l"

# Redis
docker compose exec redis redis-cli ping        # expect: PONG

# Kafka (list topics)
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
```

## Connecting from the Spring Boot app

Run the app from IntelliJ against these services. The relevant
`application.yml` settings:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/readit
    username: readit
    password: readit
  jpa:
    hibernate:
      ddl-auto: update
    properties:
      hibernate.dialect: org.hibernate.dialect.PostgreSQLDialect
  data:
    redis:
      host: localhost
      port: 6379
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: readit
      auto-offset-reset: earliest
```

## Kafka topics

Auto topic creation is turned on for convenience during early development, so
the app will not break before topics are defined. For the event driven work,
the topics should be created explicitly with chosen partition counts. This is
part of the events workstream. The commands:

```bash
docker compose exec kafka kafka-topics --create --if-not-exists \
  --topic post.events --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1

docker compose exec kafka kafka-topics --create --if-not-exists \
  --topic vote.events --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1

docker compose exec kafka kafka-topics --create --if-not-exists \
  --topic comment.events --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1

docker compose exec kafka kafka-topics --create --if-not-exists \
  --topic notification.events --bootstrap-server localhost:9092 \
  --partitions 3 --replication-factor 1
```

## Stopping and resetting

```bash
docker compose down       # stop containers, keep data
docker compose down -v     # stop containers and delete all data volumes
```

Use `down -v` whenever you want a completely clean database and broker.

## Note on Zookeeper vs KRaft

This stack uses Zookeeper because that is what the team agreed in the design
document. Kafka removed Zookeeper support in version 4.0, so the Kafka and
Zookeeper images here are pinned to Confluent Platform 7.9 (Kafka 3.9), the
last release that still supports Zookeeper. If the team later decides to drop
Zookeeper, the modern replacement is KRaft mode, which runs Kafka as a single
container with no Zookeeper at all. The switch is a small change to this file.
