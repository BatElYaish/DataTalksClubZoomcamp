{{ config(materialized='table') }}


select DISTINCT
trip_year,
trip_month,
pickup_zone,
dropoff_zone,
PERCENTILE_CONT(trip_duration,0.90) OVER(PARTITION BY trip_year,trip_month,pickup_locationid,dropoff_locationid) as p90
from {{ ref('fact_fhv_trips') }}
