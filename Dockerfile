FROM maven:3.9.6-amazoncorretto-21-debian-bookworm AS builder

WORKDIR /app

RUN apt-get update && \
    apt-get install -y git && \
   apt-get install -y curl && \
   curl -sL https://deb.nodesource.com/setup_21.x | bash - && \
   apt-get install -y nodejs && \
   npm install -g yarn

COPY . /app

RUN cd src/main/resources/frontend && \
   yarn && \
   yarn build

RUN mvn clean package

ENV DB_URL=db:3306
ENV DB_USER=root
ENV DB_PASSWORD=root
ENV DB_NAME=autograder

EXPOSE 8080

CMD ["java", "-Dlog4j2.configurationFile=log4j.properties", "-Dlog4j2.debug=false", "-jar", "/app/target/automatico-1.0-SNAPSHOT.jar"]
