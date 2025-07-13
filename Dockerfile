FROM openjdk:17

ARG JAR_FILE=build/libs/*.jar

COPY build/libs/mumuk-0.0.1-SNAPSHOT.jar app.jar

RUN ln -sf /usr/share/zoneinfo/Asia/Seoul /etc/localtime && echo "Asia/Seoul" > /etc/timezone

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT sh -c "java -Duser.timezone=Asia/Seoul -Dspring.profiles.active=$SPRING_PROFILES_ACTIVE -jar app.jar"