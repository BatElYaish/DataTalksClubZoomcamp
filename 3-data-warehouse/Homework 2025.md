# 2024 Homework

## Setup

1. Run pipeline `gcp_load_taxi_data_backfill` in Kestra and load green taxi data for year 2022 using subflow id variable `gcp_load_taxi_data_parquet`
2. Create an external table using the January 2024 - June 2024 Yellow Taxi Trip Records.

    ```sql
    CREATE OR REPLACE EXTERNAL TABLE `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024_ext`
    OPTIONS(
    format = 'PARQUET',
    uris = ['gs://de-zoomcamp-47-bucket/Raw/2024/yellow_tripdata_2024*.parquet']
    );
    ```

3. Create a table in BQ using the Yellow Taxi Trip Records for 2024 (do not partition or cluster this table)

    ```sql
    CREATE OR REPLACE TABLE `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024` AS
    SELECT * FROM `de-zoomcamp-47.de_zoomcamp.yellow_tripdata_2024_ext`;
    ```

## Q1 - What is count of records for the 2024 Yellow Taxi Data?

![Q1](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/3-data-warehouse/Images/Q1.png "Q1")

Answer : 20,332,093

## Q2 - Estimated amount of data for External and Materialized Table

![Q2](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/3-data-warehouse/Images/Q2.png "Q2")

Answer:  0 MB for the External Table and 155.12 MB for the Materialized Table

## Q3 - Why are the estimated number of Bytes different from reading 1 column to reading 2 columns?

Answer: BigQuery is a columnar database, and it only scans the specific columns requested in the query. Querying two columns (PULocationID, DOLocationID) requires reading more data than querying one column (PULocationID), leading to a higher estimated number of bytes processed.

## Q4 - How many records have a fare_amount of 0?

![Q4](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/3-data-warehouse/Images/Q4.png "Q4")

Answer: 8,333

## Q5 - What is the best strategy to make an optimized table in Big Query if your query will always filter based on tpep_dropoff_datetime and order the results by VendorID?

Answer : Partition by tpep_dropoff_datetime and Cluster on VendorID

## Q6 -  Estimated bytes processed between Materialized Table and Materialized Table with partitioning and clustering

![Q6](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/3-data-warehouse/Images/Q6.png "Q6")

Answer : 310.24 MB for non-partitioned table and 26.84 MB for the partitioned table

## Q7 - Where is the data stored in the External Table you created?

Answer: GCP Bucket

## Q8 -  It is best practice in Big Query to always cluster your data?

Answer: False

## Q9 - Write a SELECT count(*) query FROM the materialized table you created. How many bytes does it estimate will be read? Why?

Answer: `count(*)` scans the entire table. BigQuery  knows the table size in materialized tables from the metadata, so little to no data will be read.
