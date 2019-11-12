FROM ubuntu:18.04

SHELL ["/bin/bash", "-c"]

RUN apt-get update -y
RUN apt-get install -y openssh-server
RUN apt-get install -y openjdk-8-jdk
RUN apt-get install -y maven

COPY . /tmp/classifier/

WORKDIR /tmp/classifier/
RUN mkdir -p tmpFiles

RUN ssh-keygen -A

RUN cat /dev/zero | ssh-keygen -t rsa -P ""
RUN cat $HOME/.ssh/id_rsa.pub >> $HOME/.ssh/authorized_keys

RUN echo -e "root\nroot" | passwd

RUN cp ./docker/environment.txt ./config/environment.txt
RUN cp ./docker/.bashrc ~/.bashrc
RUN cp ./docker/hadoop-env.sh ./hadoop/etc/hadoop/hadoop-env.sh
RUN cp ./docker/hdfs-site.xml ./hadoop/etc/hadoop/hdfs-site.xml
RUN cp ./docker/init.sh ./init.sh && chmod +x init.sh
RUN cp ./docker/ssh_config /etc/ssh/ssh_config
RUN mkdir -p /tmp/classifier/hadoop/hadoop_store/hdfs/datanode
RUN mkdir -p /tmp/classifier/hadoop/hadoop_data/hdfs/namenode
RUN echo "yes" | /tmp/classifier/hadoop/bin/hdfs namenode -format

RUN wget http://central.maven.org/maven2/org/apache/mahout/mahout-examples/0.12.2/mahout-examples-0.12.2-job.jar -O ./mahout/mahout-examples-0.12.2-job.jar

RUN mvn clean install package -DskipTests=true

CMD ["./init.sh"]
