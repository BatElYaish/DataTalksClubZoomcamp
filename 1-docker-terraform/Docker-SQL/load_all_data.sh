#!/bin/bash

# Run the Python script for zones
python upload_data.py --table_name=zones --url=https://github.com/DataTalksClub/nyc-tlc-data/releases/download/misc/taxi_zone_lookup.csv

# Run the Python script for yellow taxi trips
python upload_data.py --table_name=yellow_taxi_data --url=https://github.com/DataTalksClub/nyc-tlc-data/releases/download/yellow/yellow_tripdata_2021-01.csv.gz

# Run the Python script for green taxi trips
python upload_data.py --table_name=green_taxi_data --url=https://github.com/DataTalksClub/nyc-tlc-data/releases/download/green/green_tripdata_2019-10.csv.gz
