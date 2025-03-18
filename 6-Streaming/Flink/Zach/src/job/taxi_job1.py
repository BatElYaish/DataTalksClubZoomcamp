from pyflink.datastream import StreamExecutionEnvironment
from pyflink.table import EnvironmentSettings, StreamTableEnvironment


def create_taxi_events_sink_postgres(t_env):
    table_name = 'taxi_events'
    sink_ddl = f"""
        CREATE OR REPLACE TABLE {table_name} (
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
        ) WITH (
            'connector' = 'jdbc',
            'url' = 'jdbc:postgresql://postgres:5432/postgres',
            'table-name' = '{table_name}',
            'username' = 'postgres',
            'password' = 'postgres',
            'driver' = 'org.postgresql.Driver'
        );
        """
    t_env.execute_sql(sink_ddl)
    return table_name


def create_events_source_kafka(t_env):
    table_name = "taxi_events_kafka"
    pattern = "yyyy-MM-dd HH:mm:ss"
    source_ddl = f"""
        CREATE OR REPLACE TABLE {table_name} (
            VendorID VARCHAR,
            lpep_pickup_datetime VARCHAR,
            lpep_dropoff_datetime VARCHAR,
            store_and_fwd_flag VARCHAR,
            RatecodeID VARCHAR ,
            PULocationID VARCHAR,
            DOLocationID VARCHAR,
            passenger_count VARCHAR,
            trip_distance VARCHAR,
            fare_amount VARCHAR,
            extra VARCHAR,
            mta_tax VARCHAR,
            tip_amount VARCHAR,
            tolls_amount VARCHAR,
            ehail_fee VARCHAR,
            improvement_surcharge VARCHAR,
            total_amount VARCHAR,
            payment_type VARCHAR,
            trip_type VARCHAR,
            congestion_surcharge VARCHAR,
            pickup_timestamp AS TO_TIMESTAMP(lpep_pickup_datetime, '{pattern}'),
            WATERMARK FOR pickup_timestamp AS pickup_timestamp - INTERVAL '15' SECOND
        ) WITH (
            'connector' = 'kafka',
            'properties.bootstrap.servers' = 'redpanda-1:29092',
            'topic' = 'green-trips-full',
            'scan.startup.mode' = 'earliest-offset',
            'properties.auto.offset.reset' = 'earliest',
            'format' = 'json'
        );
        """
    t_env.execute_sql(source_ddl)
    return table_name

def log_processing():
    # Set up the execution environment
    env = StreamExecutionEnvironment.get_execution_environment()
    env.enable_checkpointing(10 * 1000)
    env.set_parallelism(1)

    # Set up the table environment
    settings = EnvironmentSettings.new_instance().in_streaming_mode().build()
    t_env = StreamTableEnvironment.create(env, environment_settings=settings)
    try:
        # t_env.execute_sql("""DROP TABLE taxi_events""")
        # Create Kafka table
        source_table = create_events_source_kafka(t_env)
        postgres_sink = create_taxi_events_sink_postgres(t_env)
        # write records to postgres too!
        t_env.execute_sql(
                f"""
                INSERT INTO {postgres_sink}
                SELECT 
                    CASE WHEN VendorID = '' THEN NULL ELSE CAST(VendorID AS INTEGER) END,
                    lpep_pickup_datetime,
                    lpep_dropoff_datetime,
                    store_and_fwd_flag,
                    CASE WHEN RatecodeID = '' THEN NULL ELSE CAST(RatecodeID AS INTEGER) END,
                    CASE WHEN PULocationID = '' THEN NULL ELSE CAST(PULocationID AS INTEGER) END,
                    CASE WHEN DOLocationID = '' THEN NULL ELSE CAST(DOLocationID AS INTEGER) END,
                    CASE WHEN passenger_count = '' THEN NULL ELSE CAST(passenger_count AS INTEGER) END,
                    CASE WHEN trip_distance = '' THEN NULL ELSE CAST(trip_distance AS NUMERIC) END,
                    CASE WHEN fare_amount = '' THEN NULL ELSE CAST(fare_amount AS NUMERIC) END,
                    CASE WHEN extra = '' THEN NULL ELSE CAST(extra AS NUMERIC) END,
                    CASE WHEN mta_tax = '' THEN NULL ELSE CAST(mta_tax AS NUMERIC) END,
                    CASE WHEN tip_amount = '' THEN NULL ELSE CAST(tip_amount AS NUMERIC) END,
                    CASE WHEN tolls_amount = '' THEN NULL ELSE CAST(tolls_amount AS NUMERIC) END,
                    CASE WHEN ehail_fee = '' THEN NULL ELSE CAST(ehail_fee AS NUMERIC) END,
                    CASE WHEN improvement_surcharge = '' THEN NULL ELSE CAST(improvement_surcharge AS NUMERIC) END,
                    CASE WHEN total_amount = '' THEN NULL ELSE CAST(total_amount AS NUMERIC) END,
                    CASE WHEN payment_type = '' THEN NULL ELSE CAST(payment_type AS INTEGER) END,
                    CASE WHEN trip_type = '' THEN NULL ELSE CAST(trip_type AS INTEGER) END,
                    CASE WHEN congestion_surcharge = '' THEN NULL ELSE CAST(congestion_surcharge AS NUMERIC) END,
                    pickup_timestamp
                FROM {source_table}
                """
            ).wait()

    except Exception as e:
        print("Writing records from Kafka to JDBC failed:", str(e))


if __name__ == '__main__':
    log_processing()
