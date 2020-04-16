FROM debian:stretch
RUN apt-get update && apt-get install -y openjdk-8-jre openjdk-8-jdk unzip wget
RUN wget -O payara https://search.maven.org/remotecontent?filepath=fish/payara/distributions/payara/5.201/payara-5.201.zip 
RUN unzip payara
ENTRYPOINT payara5/bin/asadmin start-domain --verbose 
