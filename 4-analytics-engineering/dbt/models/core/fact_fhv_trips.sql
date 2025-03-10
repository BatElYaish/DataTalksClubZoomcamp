{{ config( materialized='table' )}}

with fhv_tripdata as (
    select *, 
        'fhv' as service_type
    from {{ ref('stg_fhv_tripdata') }}
    where pickup_locationid is not null
    and dropoff_locationid is not null
), 
dim_zones as (
    select * from {{ ref('dim_zones') }}
    where borough != 'Unknown'
)
select fhv_tripdata.tripid, 
    EXTRACT(YEAR FROM pickup_datetime) as trip_year,
    EXTRACT(MONTH FROM pickup_datetime) as trip_month,
    fhv_tripdata.pickup_locationid, 
    pickup_zone.borough as pickup_borough, 
    pickup_zone.zone as pickup_zone, 
    fhv_tripdata.dropoff_locationid,
    dropoff_zone.borough as dropoff_borough, 
    dropoff_zone.zone as dropoff_zone,  
    fhv_tripdata.pickup_datetime, 
    fhv_tripdata.dropoff_datetime,
    TIMESTAMP_DIFF(fhv_tripdata.dropoff_datetime, fhv_tripdata.pickup_datetime, SECOND) AS trip_duration,
    fhv_tripdata.sr_flag
from fhv_tripdata
inner join dim_zones as pickup_zone
on fhv_tripdata.pickup_locationid = pickup_zone.locationid
inner join dim_zones as dropoff_zone
on fhv_tripdata.dropoff_locationid = dropoff_zone.locationid