FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/ticket-service.jar /ticket-service/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/ticket-service/app.jar"]
