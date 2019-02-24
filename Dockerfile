FROM openjdk:8
ARG wrk=target/work
WORKDIR /work
COPY target/cpath2.war .
# copy the data except listed in .dockerignore
COPY $wrk .
ENV CPATH2_HOME /work
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom","-Dfile.encoding=UTF-8","-Xmx64g",\
"-Dpaxtools.CollectionProvider=org.biopax.paxtools.trove.TProvider", "-Dspring.profiles.active=docker", \
"-jar", "cpath2.war", "--server"]
EXPOSE 8080
