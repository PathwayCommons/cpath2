# builds the image after maven build from local sources
# todo: test (perhaps get the data from PC2 and the fat JAR/WAR from M2 repo instead of using local build)
FROM openjdk:17
ARG WRK=target/work
VOLUME /work
WORKDIR /work
COPY target/cpath2.war .
# copy the data except listed in .dockerignore
COPY $WRK .
ENV CPATH2_HOME /work
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dfile.encoding=UTF-8", "-Xmx64g",\
"-Dspring.profiles.active=docker", "-jar", "cpath2.war", "--server", \
"--add-opens=java.base/java.lang=ALL-UNNAMED", \
"--add-opens=java.base/java.lang.reflect=ALL-UNNAMED"]
EXPOSE 8080
