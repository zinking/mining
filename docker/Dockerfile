FROM ingensi/oracle-jdk
MAINTAINER zinking <zinking3@gmail.com>

RUN yum update -y && yum install -y unzip
RUN curl -O http://downloads.typesafe.com/typesafe-activator/1.3.6/typesafe-activator-1.3.6.zip 
RUN unzip typesafe-activator-1.3.6.zip -d / 
RUN rm typesafe-activator-1.3.6.zip 
RUN chmod a+x /activator-dist-1.3.6/activator
ENV PATH $PATH:/activator-dist-1.3.6

EXPOSE 9000 9000
RUN mkdir -p /mining-solution/mining-play
WORKDIR /mining-solution/mining-play
CMD ["activator", "run"]


#docker run -d -v /home/awang/workspace/mining-solution/mining-play:/mining-solution/mining-play:rw -p 80:9000 mining-image
