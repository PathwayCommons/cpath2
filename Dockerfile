# experimental Dockerfile

FROM openjdk:8-jdk-alpine
VOLUME /work

# in production, work dir. contains actual application.properties, data, index, db -
#COPY work /work
# but in development, test data, index get generated in the project's ./target/work dir.
COPY target/work /work
WORKDIR /work
ENV CPATH2_HOME /work

ARG JAR_FILE
COPY ${JAR_FILE} /pc2.war

#it worked, but in production, should use "-Xmx64g" and other tuning java options
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dfile.encoding=UTF-8","-Xmx4g","-Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider","-Dspring.profiles.active=web","-jar","/pc2.war"]

EXPOSE 8080
