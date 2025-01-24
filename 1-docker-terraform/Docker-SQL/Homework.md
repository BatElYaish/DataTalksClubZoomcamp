# Homework

Homework - <https://github.com/DataTalksClub/data-engineering-zoomcamp/blob/main/cohorts/2025/01-docker-terraform/homework.md>

## Question 1. Understanding docker first run

### Run docker with the python:3.12.8 image in an interactive mode, use the entrypoint bash

`docker run -it --entrypoint bash python:3.12.8`

### What's the version of pip in the image?

`pip --version -> pip 24.3.1`

## Question 2. Understanding Docker networking and docker-compose

### What is the hostname and port that pgadmin should use to connect to the postgres database?

hostname = `db`

port = `5432`

## Question 3. Trip Segmentation Count

### During the period of October 1st 2019 (inclusive) and November 1st 2019 (exclusive), how many trips, respectively, happened

```sql
SELECT CASE WHEN trip_distance <= 1 THEN  '1.Up to 1 mile'
 WHEN trip_distance > 1 AND trip_distance <= 3 THEN  '2.BETWEEN 1 AND 3 miles'
 WHEN trip_distance > 3 AND trip_distance <= 7 THEN  '3.BETWEEN 3 AND 7 miles'
 WHEN trip_distance > 7 AND trip_distance <= 10 THEN '4.BETWEEN 7 AND 10 miles'
 ELSE '5.Over 10 miles' END AS "Miles_buckets",
 count(1) AS "Trips_count"
FROM public.green_taxi_trips
WHERE lpep_dropoff_datetime >= '2019-10-01'
AND   lpep_dropoff_datetime < '2019-11-01'
GROUP BY "Miles_buckets"
ORDER BY 1
```

Result

|Miles_buckets              | Trips_count |
|---------------------------|-------------|
|"1.Up to 1 mile"           |  104802     |
|"2.BETWEEN 1 AND 3 miles"  |  198924     |
|"3.BETWEEN 3 AND 7 miles"  |  109603     |
|"4.BETWEEN 7 AND 10 miles" |   27678     |
|"5.Over 10 miles"          |   35189     |

## Question 4. Longest trip for each day

Which was the pick up day with the longest trip distance? Use the pick up time for your calculations.

```sql
SELECT CAST(lpep_pickup_datetime AS DATE) AS "pick_up_day",
   MAX(trip_distance) AS "Max_trip_distance"
FROM public.green_taxi_trips
GROUP BY "pick_up_day"
ORDER BY 2 DESC
LIMIT 1
```

|"pick_up_day" |"Max_trip_distance"|
|--------------|------------------ |
|"2019-10-31"  |515.89             |

## Question 5. Three biggest pickup zones

Which were the top pickup locations with over 13,000 in total_amount (across all trips) for 2019-10-18?

```sql
SELECT "Zone" AS "Top_pickup_locations"
FROM public.green_taxi_trips t
LEFT JOIN public.zones z
ON t."PULocationID" = z."LocationID"
WHERE CAST(lpep_pickup_datetime AS DATE) = '2019-10-18'
GROUP BY "Zone"
HAVING SUM(total_amount) >= 13000
```

Result

|"Top_pickup_locations" |
|-----------------------|
|"East Harlem North"    |
|"East Harlem South"    |
|"Morningside Heights"  |

## Question 6. Largest tip

For the passengers picked up in October 2019 in the zone name "East Harlem North" which was the drop off zone that had the largest tip?

```sql
SELECT zdo."Zone" AS "Drop_Off_Zone", MAX(tip_amount)  AS "Max_Tip"
FROM public.green_taxi_trips t
LEFT JOIN public.zones zpu
ON t."PULocationID" = zpu."LocationID"
LEFT JOIN public.zones zdo
ON t."DOLocationID" = zdo."LocationID"
WHERE DATE(DATE_TRUNC('month', lpep_pickup_datetime)) = '2019-10-01'
AND zpu."Zone" = 'East Harlem North'
GROUP BY zdo."Zone"
ORDER BY 2 DESC
LIMIT 1
```

Result

|"Drop_Off_Zone" |"Max_Tip"|
|----------------|---------|
|"JFK Airport"   |87.3     |
