# -- Software Stack Versions
SPARK_VERSION="3.3.1"
HADOOP_VERSION="3"
JUPYTERLAB_VERSION="3.6.1"
JAVA_IMAGE_TAG="17-jre"

# -- Building the Images
docker build \
  --build-arg java_image_tag="${JAVA_IMAGE_TAG}" \
  -f cluster-base.Dockerfile \
  -t cluster-base .

docker build \
  --build-arg spark_version="${SPARK_VERSION}" \
  --build-arg hadoop_version="${HADOOP_VERSION}" \
  -f spark-base.Dockerfile \
  -t spark-base .

docker build \
  -f spark-master.Dockerfile \
  -t spark-master .

docker build \
  -f spark-worker.Dockerfile \
  -t spark-worker .

docker build \
  --build-arg spark_version="${SPARK_VERSION}" \
  --build-arg jupyterlab_version="${JUPYTERLAB_VERSION}" \
  -f jupyterlab.Dockerfile \
  -t jupyterlab .