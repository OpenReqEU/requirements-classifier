# Requirements Classifier

_This service was created as a result of the OpenReq project funded by the European Union Horizon 2020 Research and Innovation programme under grant agreement No 732463._

## Introduction

The **requirements classifier** component is a service addressed to apply a binary classification algorithm to a set of requirements based on the type of this requirement.

Given a set of requirements of a project, the user of the service can perform 3 different actions:

1. **Create a model**. The dataset of requirements is used to train a classifier by creating a model that can be used for classification.
2. **Classify requirements**. The dataset is used against an already created model to get classification results based on their types.
3. **Train and test**. The dataset is used both for training a classifier and testing it, splitting the input data in several datasets.

Requirement classification is an essential step in the OpenReq project that applies to critical tasks in Requirements Engineering (RE) such as dealing and structuring large amounts of requirements data. Using the common data structure in the OpenReq framework, requirements can be classified according to its type, so stakeholders can improve their efficiency in this kind of steps of RE.

## Technical description

Next sections provide a general overview of the technical details of the requirements classifier.

### Main functionalities

As introduced before, the classifier can be used with 3 different purposes.

#### Create a model

Classification requires to provide a dataset to train a classifier in order to build a model that can be used for future classification tasks. For this purpose, we need to provide:

- The **OpenReq JSON format** with the list of requirements
- The **company** to which the model belongs to (i.e. the stakeholder proprietary of the mdoel)
- The **property** of the classifier (i.e. the req. type to apply the classifier)

Note that models are build based on a binary classification which states whether a requirement is from a type or not.

#### Classify requirements

Using a specific model (i.e. a classifier), the requirements dataset is classified against that model. For this purpose, we need to provide:

- The **OpenReq JSON format** with the list of requirements
- The **company** to which the model belongs
- The **property** of the Classifier

Company and property values are used to reference a unique model between the classifier.

#### Train and test

Previous functionalities are combined to build a model and test a dataset against it. For this purpose we need:

- The **OpenReq JSON format** with the list of requirements
- An integer **_k_** used for the number of tests to apply

Basically a _k_ cross-validation is performed using the requirements recieved in the request. The service plits the requirements in k groups, trains a classifier for each group with all of the requirements recieved except the ones in the group, and tests it with the requirements in the group.

As a result we get a series of statistics which give us a general overview in the performance and reliability of the classifier

### Used technologies

The classification service relies on two different technologies that are used together to build and perform classification.

1. **Apache Mahout**. This Apache framework is a distributed linear algebra framework which includes APIs and tools to perform different mathematical algorithms, including the instantiation and execution of classification algorithms. The Requirements Classifier service uses the Naive Bayes implementation of this framework to perform the data classification.

2. **Apache Hadoop**. This Apache framework allows developers to manage the distributed processing of large amounts of data sets across clusters. We use this technology as a requirement for Mahout, which uses Hadoop to store and manage models data.

### How to install

#### Docker installation

The project includes a Dockerfile for running an instance of the requirements-classifier service. In order to build and run the service with Docker, run the following commands:

```docker-compose build```
```docker-compose up```

You can make sure the Docker image is running successfully by checking the status with:

```docker container ls```

##### Running multiple instances

To launch multiple docker instances of the *requirements-classifier* service, it is necessary to configure and launch both requirements-classifier and Hadoop services in different ports. Therefore, for each *build* of the Docker container, you need to:

1. **requirements-classifier**
·1. Edit *./src/main/resources/application.properties* and change *server.port=9402* to the port where you want to launch the new instance
⋅2. Edit *./docker-compose.yml* and change *'- "9402:9402"'* to the new port where the *requirements-classifier* will be launched

1. **hadoop**
·1. Edit *./docker/core-site.xml* and change *<value>hdfs://localhost:9403</value>* to the port where you want to launch the new instance of Hadoop
⋅2. Edit *./docker-compose.yml* and change *'- "9403:9403"'* to the new port where *hadoop* will be launched

#### Manual installation

The project includes an isolated, integrated version of Hadoop and Mahout. In order to use them, it is necessary to configure the following steps:

1. **Configuration**

  Modify the file config/environment.txt and write the absolute path of all environment variables.

  All mahout and hadoop variables should start with the path of the project.

  (UNIX) modify .bashrc to include these variables:

      export JAVA_HOME=java-path
      export HADOOP_HOME=project-path/hadoop
      export PATH=$PATH:$HADOOP_HOME/bin
      export PATH=$PATH:$HADOOP_HOME/sbin
      export HADOOP_MAPRED_HOME=$HADOOP_HOME
      export HADOOP_COMMON_HOME=$HADOOP_HOME
      export HADOOP_HDFS_HOME=$HADOOP_HOME
      export MAHOUT_HOME=project-path/mahout
      export PATH=$PATH:$MAHOUT_HOME/bin
      export YARN_HOME=$HADOOP_HOME
      export HADOOP_COMMON_LIB_NATIVE_DIR=$HADOOP_HOME/lib/native
      export HADOOP_OPTS="-Djava.library.path=$HADOOP_HOME/lib"

  Modify file hadoop/etc/hadoop/hadoop-env.sh and set variable JAVA_HOME.

  Modify the file: hadoop/etc/hadoop/hdfs-site.xml and change value tags to the correct path of this files. (You should only change the path to the project, once inside the project the path is the same):

          <configuration>
                  <property>
                      <name>dfs.replication</name>
                      <value>1</value>
                  </property>
                  <property>
                      <name>dfs.namenode.name.dir</name>
                   <value>file:c</value>
                  </property>
                  <property>
                      <name>dfs.datanode.data.dir</name>
                    <value>file:<project_path>/hadoop/hadoop_store/hdfs/datanode</value>
                  </property>
          </configuration>

  Once all the configuration is done we have to create the directories containing the namenode and the datanode (if they are already created, delete them first).

        mkdir -p <project_path>/hadoop/hadoop_store/hdfs/datanode
        mkdir -p <project_path>/hadoop/hadoop_data/hdfs/namenode

  Note: this paths are the same indicated in hadoop/etc/hadoop/hdfs-site.xml

  Once all the configuration is done, execute:
        $ hdfs namenode -format
  if it asks something about reseting the data, answer yes.

  To start mahout:
        $ start-all.sh

  Execute to exit hadoop admin mode.

        hdfs dfsadmin -safemode leave


2. **Start Hadoop**
```
$WORKSPACE/hadoop/sbin/start-dfs.sh
hdfs dfsadmin -safemode leave
```

3. **Build project**

Before building the project, it is necessary to download an external dependency: http://central.maven.org/maven2/org/apache/mahout/mahout-examples/0.12.2/mahout-examples-0.12.2-job.jar

This file must be placed in the mahout/ folder

Once this has been done, build the project by running:

```
mvn clean install package
```

Make sure Hadoop is up and running. Otherwise, tests will fail.

### How to use it

You can take a look at the Swagger documentation [here](https://api.openreq.eu/requirements-classifier/swagger-ui.html), which includes specific, technical details of the REST API to communicate to the service.

### Notes for developers

### Sources

- "Building a Text Classifier in Mahout’s Spark Shell." Apache Mahout. Accessed February 05, 2019. https://mahout.apache.org/users/environment/classify-a-doc-from-the-shell.html
- "Apache Hadoop 2.9.2." Apache Hadoop. Accessed February 05, 2019. http://hadoop.apache.org/docs/stable/
- Pavan, and Pavan. "Multi-level Classification Using Apache Mahout – Pavan – Medium." Medium.com. January 26, 2014. Accessed February 05, 2019. https://medium.com/@pavantwits/multi-level-classification-using-apache-mahout-4ea08a4662ab

## How to contribute

See OpenReq project contribution [guidelines](https://github.com/OpenReqEU/OpenReq/blob/master/CONTRIBUTING.md)

## License

Free use of this software is granted under the terms of the [EPL version 2 (EPL2.0)](https://www.eclipse.org/legal/epl-2.0/)
