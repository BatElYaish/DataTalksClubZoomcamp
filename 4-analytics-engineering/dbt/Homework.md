# Homework

## Setup

1. Run flow Kestra `gcp_load_taxi_data_backfill` with the `subflowId: gcp_load_taxi_data_to_bucket_csv`, load 2019-2020 for yellow and green taxi data ,and 2019 for fhv
2. Create tables over yellow, green and fhv

```sql
-- External table Yellow Taxi dataset (2019 and 2020)
CREATE OR REPLACE EXTERNAL TABLE `<GCP ProjectID>.de_zoomcamp.yellow_tripdata_csv`
OPTIONS(
  format = 'CSV',
  uris = ['gs://<GCP ProjectID>-bucket/yellow/yellow_tripdata*.csv']
);

-- External table Green Taxi dataset (2019 and 2020)
CREATE OR REPLACE EXTERNAL TABLE `<GCP ProjectID>.de_zoomcamp.green_tripdata_csv`
OPTIONS(
  format = 'CSV',
  uris = ['gs://<GCP ProjectID>-bucket/green/green_tripdata*.csv']
);

-- External table For Hire Vehicle dataset (2019)
CREATE OR REPLACE EXTERNAL TABLE `<GCP ProjectID>.de_zoomcamp.fhv_tripdata_csv`
OPTIONS(
  format = 'csv',
  uris = ['gs://<GCP ProjectID>-bucket/fhv/fhv_tripdata_2019*.csv']
);
```

## Q1 - Understanding dbt model resolution

Answer: `select * from myproject.raw_nyc_tripdata.ext_green_taxi`

## Q2 - dbt Variables & Dynamic Models

1. Create env variable in dbt - go to Deploy -> environments -> Environment variables

    ![env_var](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/4-analytics-engineering/Images/env_var.png "env_var")

2. Create a new model:

    ```sql
    select *
    from {{ ref('fact_trips') }}
    where CAST(pickup_datetime AS DATETIME) >= CURRENT_DATE() - INTERVAL  '{{ var("days_back", env_var("DBT_DAYS_BACK", "30")) }}' DAY
    ```

3. Build model:  `dbt build --select fact_Q2.sql --vars '{'days_back': 6}'`
4. Run model: `dbt run --select fact_Q2.sql --vars '{'days_back': 6}'`

Answer:
    If we didn't define the `DBT_DAYS_BACK` env variable and `days_back` then days = 30  
    If we compile the code in dbt (dev env) then days = 7 , the value we set `DBT_DAYS_BACK`  
    If we build and run the model with `--vars '{'days_back': 6}'` then days = 6
    In production when `DBT_DAYS_BACK` is set to 30 then days = 30

## Q3: dbt Data Lineage and Execution

Select the option that does NOT apply for materializing `fct_taxi_monthly_zone_revenue`:

1. `dbt run`  -> Will run all models  
1. `dbt run --select +models/core/dim_taxi_trips.sql+ --target prod` -> Will run all models downstream and upstream of dim_taxi_trips, this will also run `fct_taxi_monthly_zone_revenue` because it's upstream of it.  
1. `dbt run --select +models/core/fct_taxi_monthly_zone_revenue.sql` -> Will run `fct_taxi_monthly_zone_revenue` and all downstream
1. `dbt run --select +models/core/`  -> Will run all the models in core folder and all upstream
1. `dbt run --select models/staging/+`  -> Will run all the models in staging folder and all downstream

Answer - `dbt run --select models/staging/+` will not run `fct_taxi_monthly_zone_revenue`

## Q4: dbt Macros and Jinja

select all statements that are true to the models using it:

V - Setting a value for DBT_BIGQUERY_TARGET_DATASET env var is mandatory, or it'll fail to compile - we don't have a default for it.  
X - Setting a value for DBT_BIGQUERY_STAGING_DATASET env var is mandatory, or it'll fail to compile - if we have value for `'DBT_BIGQUERY_STAGING_DATASET'` then it will compile
V - When using core, it materializes in the dataset defined in DBT_BIGQUERY_TARGET_DATASET  
V - When using stg, it materializes in the dataset defined in DBT_BIGQUERY_STAGING_DATASET, or defaults to DBT_BIGQUERY_TARGET_DATASET  
X - When using staging, it materializes in the dataset defined in DBT_BIGQUERY_STAGING_DATASET, or defaults to DBT_BIGQUERY_TARGET_DATASET  

## Q5: Taxi Quarterly Revenue Growth

sql:

```sql
WITH qoq_calc AS (
SELECT service_type, year_quarter,
ROUND((revenue_amount - LAG(revenue_amount) OVER (PARTITION BY service_type ORDER BY trip_quarter, trip_year))/
    LAG(revenue_amount) OVER (PARTITION BY service_type, trip_quarter ORDER BY trip_year)*100,2) QoQ
FROM <GCP ProjectID>.taxi_rides_ny.fct_taxi_trips_quarterly_revenue
WHERE trip_year IN (2019,2020) )
SELECT *
FROM qoq_calc
WHERE QoQ IS NOT NULL
order by 3
```

Answer: green: {best: 2020/Q1, worst: 2020/Q2}, yellow: {best: 2020/Q1, worst: 2020/Q2}

## Q6: P97/P95/P90 Taxi Monthly Fare

sql:

```sql
SELECT *
FROM <GCP ProjectID>.taxi_rides_ny.fct_taxi_trips_monthly_fare_p95
WHERE trip_year = 2020 and trip_month = 4
```

Answer: green: {p97: 55.0, p95: 45.0, p90: 26.5}, yellow: {p97: 31.5, p95: 25.5, p90: 19.0}

## Q7: Top #Nth longest P90 travel time Location for FHV

sql:

```sql
WITH rnk_per AS (
  select
  pickup_zone,
  dropoff_zone,
  row_number() OVER(PARTITION BY pickup_zone ORDER BY p90 DESC) rn
  FROM <GCP ProjectID>.taxi_rides_ny.fct_fhv_monthly_zone_traveltime_p90
  WHERE trip_year = 2019 and trip_month = 11
        and pickup_zone IN ('Newark Airport','SoHo','Yorkville East')
)
SELECT pickup_zone,dropoff_zone
FROM rnk_per
WHERE rn = 2
```

Answer: LaGuardia Airport, Chinatown, Garment District
