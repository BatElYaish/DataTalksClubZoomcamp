{{ config(materialized='table') }}


select 
-- Reveneue grouping 
CONCAT(trip_year,"/Q" ,trip_quarter) as year_quarter,
trip_year,
trip_quarter,
service_type,
-- Revenue calculation 
sum(total_amount) as revenue_amount,
from {{ ref('fact_trips') }}
group by 1,2,3,4