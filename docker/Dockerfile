FROM ingensi/oracle-jdk
MAINTAINER zinking <zinking3@gmail.com>

RUN yum update -y && yum install -y unzip
RUN curl -O http://downloads.typesafe.com/typesafe-activator/1.3.6/typesafe-activator-1.3.6.zip 
RUN unzip typesafe-activator-1.3.6.zip -d / 
RUN rm typesafe-activator-1.3.6.zip 
RUN chmod a+x /activator-dist-1.3.6/activator
ENV PATH $PATH:/activator-dist-1.3.6
ENV env prod

RUN mkdir -p /mining-solution/mining-play
#RUN ln -sf /dev/stdout /mining-solution/mining-play/log/application.log
#RUN ln -sf /dev/stderr /mining-solution/mining-play/log/application.log

EXPOSE 9000 9000

ADD start.sh /var/tmp/

WORKDIR /mining-solution/mining-play
#CMD ["activator", "run"]
CMD bash /var/tmp/start.sh


# sudo add-apt-repository ppa:webupd8team/java
# sudo apt-get update
# sudo apt-get install oracle-java8-installer



#docker run -d -v /home/awang/workspace/mining-solution/mining-play:/mining-solution/mining-play:rw -p 9000:9000 zinking:mine
#docker run -d -v /Users/awang/os/zhenw/mining-solution/mining-play:/mining-solution/mining-play:rw -p 9000:9000 zinking:mine
#docker run -i -t -d -v /Users/awang/os/zhenw/mining-solution/mining-play:/mining-solution/mining-play:rw -p 9000:9000  --entrypoint /bin/bash zinking:mine
