FROM openjdk:17

ARG JAR_FILE=build/libs/*.jar

COPY app.jar app.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && echo "Asia/Seoul" > /etc/timezone

ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=prod", "-Duser.timezone=Asia/Seoul", "app.jar"]