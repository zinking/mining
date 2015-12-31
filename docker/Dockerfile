FROM ingensi/oracle-jdk
MAINTAINER zinking <zinking3@gmail.com>

RUN yum update -y && yum install -y unzip
RUN curl -O http://downloads.typesafe.com/typesafe-activator/1.3.6/typesafe-activator-1.3.6.zip 
RUN unzip typesafe-activator-1.3.6.zip -d / && rm typesafe-activator-1.3.6.zip && chmod a+x /activator-1.3.6/activator
ENV PATH $PATH:/activator-1.3.6

EXPOSE 9000 8888
RUN mkdir /mining-solution
WORKDIR /mining-solution

CMD ["activator", "run"]
