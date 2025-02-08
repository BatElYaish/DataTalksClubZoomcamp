# Batch - Spark

## Run spark

### Import packages

`from pyspark.sql import SparkSession`

### Run spark on local

```python
spark = SparkSession.builder \
        .master("local[*]") \
        .appName("lectures") \
        .getOrCreate()
```

### Connect to GCS from local spark (Windows)

1. Download GCS connector to hadoop from <https://cloud.google.com/dataproc/docs/concepts/connectors/cloud-storage> , for newer versions use Hadoop 3.x
2. Save the jar file to spark path in jar folder, like `C:\Spark\jars`
3. If you don't know where your spark is installed, use python package findspark.
4. Use the path to your GSP credentials JSON.

```python
spark = SparkSession.builder \
        .master("local[*]") \
        .appName("GCS Reader") \  
        .config("spark.hadoop.google.cloud.auth.service.account.enable", "true") \
        .config("spark.hadoop.google.cloud.auth.service.account.json.keyfile", gcp_json) \
        .config("spark.hadoop.fs.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFileSystem") \
        .config("spark.hadoop.fs.AbstractFileSystem.gs.impl", "com.google.cloud.hadoop.fs.gcs.GoogleHadoopFS") \
        .getOrCreate()
```

### Spark standalone mode

1. Go to where your spark is installed, like `C:\Spark\`
2. To create the Master node run on Powershell `bin\spark-class org.apache.spark.deploy.master.Master`.

>![spark_master](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/5-batch-spark/Images/spark_master.png "spark_master")

3. To create workers run `bin\spark-class org.apache.spark.deploy.worker.Worker spark://<spark master host>:7077`. Take the spark host name from the run logs of the spark master.

>![Spark_standalone](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/5-batch-spark/Images/Spark_standalone.png "Spark_standalone")

4. In your SparkSession.builder change the master to the  spark master host, like:

```python
spark = SparkSession.builder \
        .master("spark://10.100.102.25:7077") \
```

5. To Stop the nodes:

    Master: `spark-class org.apache.spark.deploy.master.Master stop`

    Worker: `spark-class org.apache.spark.deploy.worker.Worker stop`

    If your cmd is still open run `Ctrl+C` on Master and Worker

6. To submit a job to the spark cluster run your python script:

```bash
spark-submit ^
--master spark://<spark master host>:7077 ^
C:\path\to\your\script.py
```

## Tips

* On windows you can check the ports by running  `netstat -aon | findstr /R ":404[0-4]"` on powershell.

    usually local spark will be on ports 404*, standalone will be on ports 808*
