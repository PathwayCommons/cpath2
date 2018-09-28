# TODO: an experimental Dockerfile
# (mvc, swagger worked...; in production, it should also use -Xmx64g)

FROM openjdk:8-jdk-alpine
#VOLUME /tmp

# for this to work, export CPATH2_HOME=tmp before runnig 'mvn install dockerfile:build' (test data, index get generated)
COPY tmp/* /work

# but in production, work dir. contains actual application.properties, data, index, db.
#COPY work /

WORKDIR /work
ENV CPATH2_HOME "/work"

ARG JAR_FILE
COPY ${JAR_FILE} pc2.jar

ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dfile.encoding=UTF-8","-Xmx4g","-Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider","-Dspring.profiles.active=web","-jar","/pc2.jar"]

EXPOSE 8080
