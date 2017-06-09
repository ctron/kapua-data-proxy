FROM centos:7

ENV MAVEN_VERSION=3.5.0

RUN yum -y update
RUN yum install -y java-1.8.0-openjdk-devel
RUN yum install -y wget which

RUN wget http://apache.cs.uu.nl/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz && \
  tar xzf apache-maven-${MAVEN_VERSION}-bin.tar.gz

COPY . /
RUN apache-maven-${MAVEN_VERSION}/bin/mvn package && mv target/*-app.jar /app.jar

ENTRYPOINT ["/usr/bin/java", "-jar", "/app.jar" ]