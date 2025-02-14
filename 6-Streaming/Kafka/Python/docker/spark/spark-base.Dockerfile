FROM cluster-base

# -- Layer: Apache Spark
ARG spark_version=3.3.1
ARG hadoop_version=3

RUN apt-get update -y && \
    apt-get install -y curl && \
    curl -sSL https://archive.apache.org/dist/spark/spark-${spark_version}/spark-${spark_version}-bin-hadoop${hadoop_version}.tgz | tar -xz -C /opt/ && \
    mv /opt/spark-${spark_version}-bin-hadoop${hadoop_version} /opt/spark && \
    mkdir -p /opt/spark/logs

ENV SPARK_HOME=/opt/spark
ENV SPARK_MASTER_HOST=spark-master
ENV SPARK_MASTER_PORT=7077
ENV PYSPARK_PYTHON=python3

# -- Runtime
WORKDIR ${SPARK_HOME}