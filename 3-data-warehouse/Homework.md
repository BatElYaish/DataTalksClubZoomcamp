# 2024 Homework

## Setup

1. Run pipeline `gcp_load_taxi_data_backfill` in Kestra and load green taxi data for year 2022 using subflow id variable `gcp_load_taxi_data_parquet`
2. Create an external table using the Green Taxi Trip Records Data for 2022. 

    ```sql
    CREATE OR REPLACE EXTERNAL TABLE `de-zoomcamp-47.de_zoomcamp.external_green_tripdata_2022`
    OPTIONS(
    format = 'PARQUET',
    uris = ['gs://de-zoomcamp-47-bucket/Raw/parquet/green_tripdata_2022-*.parquet']
    );
    ```

3. Create a table in BQ using the Green Taxi Trip Records for 2022 (do not partition or cluster this table)

```sql
CREATE OR REPLACE TABLE `de-zoomcamp-47.de_zoomcamp.green_tripdata_2022` AS
SELECT * FROM `de-zoomcamp-47.de_zoomcamp.external_green_tripdata_2022`;
```

## Q1 - What is count of records for the 2022 Green Taxi Data?

Answer : 840,402

## Q2 - Estimated amount of data for External and Materialized Table

Answer:  0 MB for the External Table and 6.41MB for the Materialized Table

## Q3 - How many records have a fare_amount of 0?

Answer: 1,622

## Q4 - What is the best strategy to make an optimized table in Big Query if your query will always order the results by PUlocationID and filter based on lpep_pickup_datetime?

Answer : Partition by lpep_pickup_datetime Cluster on PUlocationID

## Q5 -  Estimated bytes processed between Materialized Table and Materialized Table with prtitioning and clustering 

Answer : 12.82 MB for non-partitioned table and 1.12 MB for the partitioned table

## Q6 - Where is the data stored in the External Table you created?

Answer: GCP Bucket

## Q7 -  It is best practice in Big Query to always cluster your data?

Answer: False
