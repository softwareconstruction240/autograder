FROM maven:3.9.6-amazoncorretto-21-debian-bookworm AS builder

WORKDIR /app

### install yarn and nodejs
RUN apt-get update && \
    apt-get install -y git && \
   apt-get install -y curl && \
   curl -sL https://deb.nodesource.com/setup_21.x | bash - && \
   apt-get install -y nodejs && \
   npm install -g yarn

### install frontend dependencies
COPY ./src/main/resources/frontend/package.json ./src/main/resources/frontend/yarn.lock /app/src/main/resources/frontend/

RUN cd src/main/resources/frontend && \
   yarn

### install backend dependencies
COPY ./pom.xml /app

RUN mvn dependency:go-offline

### build frontend
COPY ./src/main/resources/frontend /app/src/main/resources/frontend

RUN cd src/main/resources/frontend && \
   yarn build


### build backend
COPY . /app

RUN mvn clean package -DskipTests

EXPOSE 8080

CMD ["java", "-Dlog4j2.configurationFile=log4j.properties", "-Dlog4j2.debug=false", "-jar", "/app/target/automatico-1.0-SNAPSHOT.jar"]
