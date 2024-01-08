FROM maven:3.9.6-amazoncorretto-21-debian-bookworm AS builder

WORKDIR /app

COPY . /app

RUN apt-get update && \
    apt-get install -y curl && \
    curl -sL https://deb.nodesource.com/setup_21.x | bash - && \
    apt-get install -y nodejs && \
    npm install -g yarn && \
    cd src/main/resources/frontend && \
    yarn && \
    yarn build

RUN mvn clean package

EXPOSE 8080

CMD ["java", "-jar", "/app/target/automatico-1.0-SNAPSHOT.jar"]
