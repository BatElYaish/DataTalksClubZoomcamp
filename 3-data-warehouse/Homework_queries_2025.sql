-- Setup
-- External table
CREATE OR REPLACE EXTERNAL TABLE `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024_ext`
OPTIONS(
  format = 'PARQUET',
  uris = ['gs://de-zoomcamp-47-bucket/Raw/2024/yellow_tripdata_2024*.parquet']
);

-- Materialized table
CREATE OR REPLACE TABLE `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024` AS
SELECT * FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024_ext`;

-- Q1
SELECT count(1) cnt
FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024`;

-- Q2
SELECT count(DISTINCT PULocationID)
FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024_ext`;

SELECT count(DISTINCT PULocationID)
FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024`;

-- Q3
SELECT PULocationID
FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024`;

SELECT PULocationID, DOLocationID
FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024`;

-- Q4
SELECT count(*) row_count
FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024`
WHERE fare_amount = 0;

-- Q5
CREATE OR REPLACE TABLE `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024_pc`
PARTITION BY DATE(tpep_dropoff_datetime )
CLUSTER BY VendorID AS
SELECT * FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024`;

-- Q6
SELECT DISTINCT VendorID
FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024`
WHERE DATE(tpep_dropoff_datetime) BETWEEN '2024-03-01' AND '2024-03-15';

SELECT DISTINCT VendorID
FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024_pc`
WHERE DATE(tpep_dropoff_datetime) BETWEEN '2024-03-01' AND '2024-03-15';

-- Q9
SELECT count(*) row_count
FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024`;