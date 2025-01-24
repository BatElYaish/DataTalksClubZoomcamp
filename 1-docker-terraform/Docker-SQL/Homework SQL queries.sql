--Q3
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

--Q4
SELECT CAST(lpep_pickup_datetime AS DATE) AS "pick_up_day",
   MAX(trip_distance) AS "Max_trip_distance"
FROM public.green_taxi_trips
GROUP BY "pick_up_day"
ORDER BY 2 DESC
LIMIT 1

--Q3
SELECT "Zone" AS "Top_pickup_locations"
FROM public.green_taxi_trips t
LEFT JOIN public.zones z
ON t."PULocationID" = z."LocationID"
WHERE CAST(lpep_pickup_datetime AS DATE) = '2019-10-18'
GROUP BY "Zone"
HAVING SUM(total_amount) >= 13000
ORDER BY 2 DESC
LIMIT 4

--Q5
SELECT "Zone" AS "Top_pickup_locations"
FROM public.green_taxi_trips t
LEFT JOIN public.zones z
ON t."PULocationID" = z."LocationID"
WHERE CAST(lpep_pickup_datetime AS DATE) = '2019-10-18'
GROUP BY "Zone"
HAVING SUM(total_amount) >= 13000

--Q6
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