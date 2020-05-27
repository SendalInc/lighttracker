FROM openjdk:11

COPY target/lighttracker-1.0-SNAPSHOT.jar /home
COPY config-production.yml /home/config.yml
EXPOSE 8023
CMD ["java", "-Xmx200m", "-jar", "/home/lighttracker-1.0-SNAPSHOT.jar","server", "/home/config.yml"]
