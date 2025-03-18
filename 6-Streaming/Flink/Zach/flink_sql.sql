
SELECT test_data,event_timestamp FROM processed_events;

CREATE TABLE processed_events (
    test_data INTEGER,
    event_timestamp TIMESTAMP
)

CREATE TABLE processed_events_aggregated (
    event_hour TIMESTAMP,
    test_data INTEGER,
    num_hits INTEGER 
)

drop table processed_events_agg_PU_DO

TRUNCATE table processed_events_agg_PU_DO

CREATE TABLE processed_events_agg_PU_DO (
            window_start TIMESTAMP(3),
            PULocationID VARCHAR,            
            DOLocationID VARCHAR,            
            trip_streak BIGINT,
            PRIMARY KEY (window_start) 
        ) 

SELECT * FROM processed_events_agg_PU_DO

DROP TABLE taxi_events

CREATE TABLE taxi_events (
            VendorID INTEGER,
            lpep_pickup_datetime VARCHAR,
            lpep_dropoff_datetime VARCHAR,
            store_and_fwd_flag VARCHAR,
            RatecodeID INTEGER ,
            PULocationID INTEGER,
            DOLocationID INTEGER,
            passenger_count INTEGER,
            trip_distance NUMERIC,
            fare_amount NUMERIC,
            extra NUMERIC,
            mta_tax NUMERIC,
            tip_amount NUMERIC,
            tolls_amount NUMERIC,
            ehail_fee NUMERIC,
            improvement_surcharge NUMERIC,
            total_amount NUMERIC,
            payment_type INTEGER,
            trip_type INTEGER,
            congestion_surcharge NUMERIC,
            pickup_timestamp TIMESTAMP(3)
        )

truncate table taxi_events

SELECT * FROM taxi_events