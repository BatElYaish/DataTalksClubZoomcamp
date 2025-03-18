# Homework

## Question 1: Redpanda version

Run :
1. `docker exec -it redpanda-1 /bin/bash`
2. `rpk --version`

Answer: rpk version v24.2.18 (rev f9a22d4430)

## Question 2. Creating a topic

`rpk topic create green-trips`
What's the output of the command for creating a topic? Include the entire output in your answer.

```cmd
TOPIC        STATUS
green-trips  OK
```

## Question 3. Connecting to the Kafka server

Q: Provided that you can connect to the server, what's the output of the last command?
A: True

## Question 4: Sending the Trip Data

Q: How much time did it take to send the entire dataset and flush? 
A: took 79.85 seconds

## Question 5: Build a Sessionization Window (2 points)

Setup:

1. Run `docker compose up -d`
2. Create a table in postgres:
    ```sql
    CREATE TABLE processed_events_agg_PU_DO (
            window_start TIMESTAMP(3),
            PULocationID VARCHAR,            
            DOLocationID VARCHAR,            
            trip_streak BIGINT,
            PRIMARY KEY (window_start, PULocationID, DOLocationID) 
        ) 
    ```
3. Check that Flink and Kafka are running
4. Create a topic `green-trips` using CLI or code
5. Save data to data Folder
6. Run producer load_taxi_data.py
7. Run Flink job `docker compose exec jobmanager /opt/flink/bin/flink run -py /opt/src/job/session_job.py --pyFiles /opt/src -d`
8. Query the postgres table :
    ```sql
    with cte_lag AS(
    SELECT window_start,
    PU_DO,
    trip_streak,
    LAG(window_start) OVER (PARTITION BY PU_DO ORDER BY window_start) lags,
    window_start - LAG(window_start) OVER (PARTITION BY PU_DO ORDER BY window_start) AS gap
    FROM processed_events_agg
    )
    SELECT *
    FROM cte_lag
    WHERE gap < INTERVAL '7 minutes'
    order by PU_DO
    ```
Answer : 129-129