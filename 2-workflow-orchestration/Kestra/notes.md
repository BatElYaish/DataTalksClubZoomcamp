# Workflow Orchestration

## Run kestra docker compose file to always pull the latest image

`docker compose up --pull always -d`

## Database

### Postgres

1. Run docker compose from folder 1-docker-terraform.

2. Change `pluginDefaults` section at the bottom of the flows to match the postgres defined in docker.
    For example:

    ```yaml
    pluginDefaults:
    - type: io.kestra.plugin.jdbc.postgresql
        values:
        url: jdbc:postgresql://postgres:5432/ny_taxi
        username: postgres
        password: postgres
    ```

### GCP

1. Add these key-value to namespace:

    * `GCP_BUCKET_NAME`
    * `GCP_DATASET`
    * `GCP_CREDS`
    * `GCP_PROJECT_ID`
    * `GCP_LOCATION`
    * `GCP_BUCKET_NAME`

2. Run flow `05_gcp_setup.yaml` to set up GCP resources, this creates a bucket and a dataset in GCP.
