FROM spark-base

# -- Runtime
ARG spark_worker_web_ui=8081

EXPOSE ${spark_worker_web_ui}

CMD ["bash", "-c", "bin/spark-class org.apache.spark.deploy.worker.Worker spark://${SPARK_MASTER_HOST}:${SPARK_MASTER_PORT} >> /dev/stdout"]