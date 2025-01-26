# Docker notes

run docker postgres

`docker run -it -e POSTGRES_USER="postgres" -e POSTGRES_PASSWORD="postgres" -e POSTGRES_DB="ny_taxi" -v ./ny_taxi_postgres_data:/var/lib/postgresql/data -p 5432:5432 postgres:13 postgres`

run pgcli

`pgcli -h localhost -p 5432 -u postgres -d ny_taxi`

pull the docker image

`docker pull dpage/pgadmin4`

docker run pgadmin

`docker run -it -e PGADMIN_DEFAULT_EMAIL="postgres@postgres.com" -e PGADMIN_DEFAULT_PASSWORD="postgres" -p 9090:80 dpage/pgadmin4`

docker create network
We want that pgAdmin will be able to connect to postgres

`docker network create pg-network`

run docker postgres with network

`docker run -it -e POSTGRES_USER="postgres" -e POSTGRES_PASSWORD="postgres" -e POSTGRES_DB="ny_taxi" -v ./ny_taxi_postgres_data:/var/lib/postgresql/data  -p 5432:5432 --network=pg-network --name pg-database postgres:13  postgres`

docker run pgadmin with network

`docker run -it -e PGADMIN_DEFAULT_EMAIL="postgres@postgres.com" -e PGADMIN_DEFAULT_PASSWORD="postgres" -p 9090:80 --network=pg-network --name pgadmin dpage/pgadmin4`

convert ipynb to py

`jupyter nbconvert --to=script upload_data.ipynb to script`

run the python script to load the data

`python upload_data.py  --user=postgres --password=postgres --host=localhost --port=5432 --db=ny_taxi --table_name=yellow_taxi_trips  --url=https://github.com/DataTalksClub/nyc-tlc-data/releases/download/yellow/yellow_tripdata_2021-01.csv.gz`

build docker ingest pipeline

`docker build -f Dockerfile -t taxi_ingest:v001 .`

run docker pipeline

`docker run -it --network pg-network taxi_ingest:v001 --user=postgres --password=postgres --host=postgres --port=5432 --db=ny_taxi --table_name=yellow_taxi_trips --url=https://github.com/DataTalksClub/nyc-tlc-data/releases/download/yellow/yellow_tripdata_2021-01.csv.gz`
