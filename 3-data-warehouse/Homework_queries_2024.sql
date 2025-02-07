-- Setup
CREATE OR REPLACE EXTERNAL TABLE `de-zoomcamp-47.de_zoomcamp.external_green_tripdata_2022`
OPTIONS(
  format = 'PARQUET',
  uris = ['gs://de-zoomcamp-47-bucket/Raw/parquet/green_tripdata_2022-*.parquet']
);


CREATE OR REPLACE TABLE `de-zoomcamp-47.de_zoomcamp.green_tripdata_2022` AS
SELECT * FROM `de-zoomcamp-47.de_zoomcamp.external_green_tripdata_2022`;

-- Q1
SELECT count(*) row_count
FROM de-zoomcamp-47.de_zoomcamp.external_green_tripdata_2022;

-- Q2
SELECT count(PULocationID)
FROM de-zoomcamp-47.de_zoomcamp.external_green_tripdata_2022;

SELECT count(PULocationID)
FROM de-zoomcamp-47.de_zoomcamp.green_tripdata_2022;

-- Q3
SELECT count(*) row_count
FROM de-zoomcamp-47.de_zoomcamp.green_tripdata_2022
WHERE fare_amount = 0;

-- Q4
CREATE OR REPLACE TABLE `de-zoomcamp-47.de_zoomcamp.green_tripdata_2022_pc`
PARTITION BY DATE(lpep_pickup_datetime)
CLUSTER BY PUlocationID AS
SELECT * FROM `de-zoomcamp-47.de_zoomcamp.external_green_tripdata_2022`;

-- Q5
SELECT DISTINCT PUlocationID
FROM de-zoomcamp-47.de_zoomcamp.green_tripdata_2022_pc
WHERE DATE(lpep_pickup_datetime) BETWEEN '2022-06-01' AND '2022-06-30';

SELECT DISTINCT PUlocationID
FROM de-zoomcamp-47.de_zoomcamp.green_tripdata_2022
WHERE DATE(lpep_pickup_datetime) BETWEEN '2022-06-01' AND '2022-06-30';