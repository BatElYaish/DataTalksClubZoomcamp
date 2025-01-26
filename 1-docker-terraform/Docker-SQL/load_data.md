# How to load the data

Data files are location : <https://github.com/DataTalksClub/nyc-tlc-data/>

1. Create .env file with the following variables:

    ```bash
    POSTGRES_USER=postgres
    POSTGRES_DB=ny_taxi
    POSTGRES_PASSWORD=postgres
    POSTGRES_PORT=5432
    PGADMIN_EMAIL=postgres@postgres.com
    PGADMIN_PASSWORD=postgres
    PGADMIN_PORT=8080
    ```

2. Build docker file for python
`docker build -f Dockerfile_python -t python-app .`
3. Run the docker compose file
`docker compose -p zoomcamp-postgres up --build -d`
4. Run `docker ps --filter "name=zoomcamp-postgres-python-app"` to get your python CONTAINER ID
5. Open a bash terminal inside python container
`docker exec -it <CONTAINER ID> /bin/bash`
6. Run `ls` to make sure that the you can see the files
7. Make the bash script executable
`chmod +x load_all_data.sh`
8. Run bash script to load all data `bash load_all_data.sh`
9. Run pgcli to check the data loaded (you can also do this from the postgres container or pgAdmin)

    * first set environment variable temporarily for the session so you won't be prompt to insert a password (if you want it to be permanent add it to `.bashrc` file)
    `export PGPASSWORD=$POSTGRES_PASSWORD`
    * run pgcli `pgcli -h $POSTGRES_HOST -U $POSTGRES_USER -d $POSTGRES_DB -p $POSTGRES_PORT`

10. Check that all tables loaded using `/dt`
you should see something like this:

    | Schema | Name             | Type  | Owner    |
    |--------|------------------|-------|----------|
    | public | green_taxi_data  | table | postgres |
    | public | yellow_taxi_data | table | postgres |
    | public | zones            | table | postgres |

11. Rum `\q` to exit pgcli
12. Stop docker and remove container from host terminal `docker compose -p zoomcamp-postgres down`
