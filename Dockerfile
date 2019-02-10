FROM openjdk:8-jdk-alpine
VOLUME /work

#todo: copy data only if the volume /work does not exist?
# in production, work dir. contains actual application.properties, data, index, db -
#COPY work /work
# but in development, test data, index get generated in the project's ./target/work dir.
COPY target/work /work

COPY target/cpath2.war /work
ENV CPATH2_HOME /work
WORKDIR /work
#it worked, but in production, should use "-Xmx64g" and other tuning java options
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-Dfile.encoding=UTF-8","-Xmx64g","-Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider","-Dspring.profiles.active=web","-jar","cpath2.war"]
EXPOSE 8080
