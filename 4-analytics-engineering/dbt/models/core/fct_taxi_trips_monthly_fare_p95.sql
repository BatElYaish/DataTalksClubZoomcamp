{{ config(materialized='table') }}

with trips_data as (
    select * 
    from {{ ref('fact_trips') }}
    where fare_amount > 0 and trip_distance > 0 and payment_type_description in ('Cash', 'Credit card')
)
select DISTINCT
trip_year,
trip_month,
service_type,
PERCENTILE_CONT(fare_amount,0.97) OVER(PARTITION BY service_type,trip_year,trip_month) as p97,
PERCENTILE_CONT(fare_amount,0.95) OVER(PARTITION BY service_type,trip_year,trip_month) as p95,
PERCENTILE_CONT(fare_amount,0.90) OVER(PARTITION BY service_type,trip_year,trip_month) as p90,
from trips_data
