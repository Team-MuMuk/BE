FROM eclipse-temurin:17-jre-alpine

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

# 시간대 설정
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone && \
    apk del tzdata

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms512m -Xmx1024m -Duser.timezone=Asia/Seoul"

# 애플리케이션 포트 노출
EXPOSE 8080

# 시스템 진입점 정의
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar app.jar"]