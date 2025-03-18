from pyflink.datastream import StreamExecutionEnvironment
from pyflink.table import EnvironmentSettings, StreamTableEnvironment
from pyflink.common.watermark_strategy import WatermarkStrategy
from pyflink.common.time import Duration
from pyflink.table.expressions import lit, col, concat
from pyflink.table.window import Session

def create_events_aggregated_sink(t_env):
    table_name = 'processed_events_agg'
    sink_ddl = f"""
        CREATE TABLE {table_name} (
            window_start TIMESTAMP(3),
            PU_DO VARCHAR,
            trip_streak BIGINT,
            PRIMARY KEY (window_start, PU_DO) NOT ENFORCED
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
    table_name = "events_agg"
    source_ddl = f"""
        CREATE TABLE {table_name} (
            lpep_pickup_datetime VARCHAR,
            lpep_dropoff_datetime VARCHAR,
            PULocationID VARCHAR,            
            DOLocationID VARCHAR, 
            passenger_count VARCHAR,
            trip_distance VARCHAR,
            tip_amount VARCHAR,
            dropoff_time AS TO_TIMESTAMP(lpep_dropoff_datetime),
            WATERMARK FOR dropoff_time AS dropoff_time - INTERVAL '5' SECONDS
        ) WITH (
            'connector' = 'kafka',
            'properties.bootstrap.servers' = 'broker:29092',
            'topic' = 'green-trips',
            'scan.startup.mode' = 'earliest-offset',
            'properties.group.id' = 'flink-consumer-group',
            'properties.auto.offset.reset' = 'earliest',
            'format' = 'json'
        );
        """
    t_env.execute_sql(source_ddl)
    return table_name


def log_aggregation():
    # Set up the execution environment
    env = StreamExecutionEnvironment.get_execution_environment()
    env.enable_checkpointing(30 * 1000)
    env.set_parallelism(1)
    
    # Set up the table environment
    settings = EnvironmentSettings.new_instance().in_streaming_mode().build()
    t_env = StreamTableEnvironment.create(env, environment_settings=settings)

    watermark_strategy = (
        WatermarkStrategy
        .for_bounded_out_of_orderness(Duration.of_seconds(5))
        .with_timestamp_assigner(
            lambda event, timestamp: event[2]  # We treat the second tuple element as the event-time (ms).
        )
    )

    try:
        # Create Kafka table
        source_table = create_events_source_kafka(t_env)
        aggregated_table = create_events_aggregated_sink(t_env)

        t_env.from_path(source_table)\
            .window(
                Session.with_gap(lit(5).minutes).on(col("dropoff_time")).alias("w")
            ).group_by(
                col("w"), 
                concat(col("PULocationID"), lit("-"), col("DOLocationID")).alias("PU_DO") 
            ).select(
                col("w").start.alias("window_start"), 
                col('PU_DO'),
                lit(1).sum.alias("trip_streak")
            ).execute_insert(aggregated_table).wait()

        # sql_query = f"""
        # INSERT INTO {aggregated_table}
        # SELECT 
        #     window_start,
        #     CONCAT(PULocationID,'-' ,DOLocationID) PU_DO,
        #     COUNT(*) as trip_streak
        # FROM TABLE(
        #     SESSION(
        #         TABLE {source_table} ,
        #         DESCRIPTOR(dropoff_time),
        #         INTERVAL '5' MINUTES
        #     )
        # )
        # GROUP BY 
        #     window_start,
        #     CONCAT(PULocationID,'-' ,DOLocationID)
        # """

        # t_env.execute_sql(sql_query).wait()

    except Exception as e:
        print("Writing records from Kafka to JDBC failed:", str(e))


if __name__ == '__main__':
    log_aggregation()
