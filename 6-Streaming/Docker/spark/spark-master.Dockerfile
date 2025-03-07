FROM spark-base

# -- Runtime
ARG spark_master_web_ui=8080

EXPOSE ${spark_master_web_ui} ${SPARK_MASTER_PORT}

CMD ["bash", "-c", "bin/spark-class org.apache.spark.deploy.master.Master >> /dev/stdout"]