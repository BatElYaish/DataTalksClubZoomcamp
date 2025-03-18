# Flink notes

## PyFlink job structure

1. Set up the execution environment : `env = StreamExecutionEnvironment.get_execution_environment()`
2. Set up the table environment
    modes:
    * Batch : `TableEnvironment`
    * Stream : `StreamTableEnvironment`
3. Register source\sink table - Optional sources: files, socket, kafka, jdbc
4. read source- > do transformations -> write to sink

## Environment Settings

**Batch vs. Stream**  
Batch - Flink will read the data from the stream and end the job once done. `settings = EnvironmentSettings.new_instance().in_batch_mode().build()`  
Stream - Flink will keep on listening for new data. `settings = EnvironmentSettings.new_instance().in_streaming_mode().build()`

**Checkpointing**  
[checkpointing](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/datastream/fault-tolerance/checkpointing/) will create a snapshot of the state of the job. Checkpoints allow Flink to recover state and positions in the streams. By default, checkpointing is disabled. To enable checkpointing, call enable_checkpointing(n) on the StreamExecutionEnvironment, where n is the checkpoint interval in milliseconds. When you restart the job after a fail it will start from checkpoint.  
Checkpointing is only for a specific job from a specific entry point. If you restart a job from a different entrypoint it will create a new job and will not notice the checkpoint and will consume the data from the start depending on your settings.  
`checkpoint storage`: You can set the location where checkpoint snapshots are made durable. By default Flink will use the JobManager’s heap. For production deployments it is recommended to instead use a durable filesystem.  
`exactly-once` vs. `at-least-once`: You can optionally pass a mode to the `enable_checkpointing(n)` method to choose between the two guarantee levels. Exactly-once is preferable for most applications. At-least-once may be relevant for certain super-low-latency (consistently few milliseconds) applications.`env.get_checkpoint_config().set_checkpointing_mode(CheckpointingMode.EXACTLY_ONCE)`

**Parallelism**
Parallelism is set by the key of the data - what you'll be grouping on

## Connectors

[connectors](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/connectors/table/overview/) - what type of source and sink to connect to.

1. [Kafka](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/connectors/table/kafka/)
    Options:  
    * connector (Mandatory): `'connector' = 'kafka'`
    * properties.bootstrap.servers (Mandatory): `'properties.bootstrap.servers' = 'redpanda-1:29092'`
    * format (Mandatory) : [format](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/connectors/table/formats/overview/) types like CSV, JSON, AVRO.  
    * `topic` : name of the topic to read from. also supports topic list for source by separating topic by semicolon like 'topic-1;topic-2'.
    * `scan.startup.mode` :  
        `group-offsets` (default): consume from last committed offsets of a specific consumer group.  
        `earliest-offset`: consume from the earliest offset possible.  
        `latest-offset`: start from the latest offset.  
        `timestamp`: start from user-supplied timestamp for each partition. Another config option `scan.startup.timestamp-millis` is required to specify a specific startup timestamp  
        `specific-offsets`: start from user-supplied specific offsets for each partition.
    * `properties.auto.offset.reset` :
         properties - can be used to pass arbitrary Kafka configurations, Suffix names must match the configuration key defined in [Kafka Configuration documentation](https://kafka.apache.org/documentation/#configuration).
         `auto.offset.reset` - What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server (e.g. because that data has been deleted):  
        >`earliest`: automatically reset the offset to the earliest offset  
        >`latest` (default): automatically reset the offset to the latest offset  
        >`none`: throw exception to the consumer if no previous offset is found for the consumer's group  
        >`anything else`: throw exception to the consumer

## Table options

**[Watermark](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/datastream/event-time/generating_watermarks/)**  
Specifies the tolerance for out of order events of not existing data. for example if you have data that is coming not in ordered fashioned, the data that you are grouping on doesn't exists yet or might be out of order.
for example - if you set it to 15 seconds, when you run your group by or window , if you have any events that are out of order , it will wait 15 seconds to capture any events that may be out of order.  

**[Windows](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/datastream/operators/windows/)**

Code structure:  
```python
input \
    .key_by(<key selector>) \
    .window(<window type>) \
    .<windowed transformation>(<window function>)
```

* Tumble window - Tumbling windows have a fixed size and do not overlap. For example, if you specify a tumbling window with a size of 5 minutes, the current window will be evaluated and a new window will be started every five minutes.  
    Time intervals can be specified by using one of Time.milliseconds(x), Time.seconds(x), Time.minutes(x), and so on
* sliding windows - assigns elements to windows of fixed length.
    parameters: size of the windows is configured by the window size parameter. window slide parameter controls how frequently a sliding window is started.  
    sliding windows can be overlapping if the slide is smaller than the window size. In this case elements are assigned to multiple windows.  
    For example, you could have windows of size 10 minutes that slides by 5 minutes. With this you get every 5 minutes a window that contains the events that arrived during the last 10 minutes
* Session Windows - groups elements by sessions of activity. Session windows do not overlap and do not have a fixed start and end time. Instead a session window closes when it does not receive elements for a certain period of time, i.e., when a gap of inactivity occurred. A session window assigner can be configured with either a static session gap or with a session gap extractor function which defines how long the period of inactivity is. When this period expires, the current session closes and subsequent elements are assigned to a new session window.
* Global Windows - assigns all elements with the same key to the same single global window. This windowing scheme is only useful if you also specify a custom trigger. Otherwise, no computation will be performed, as the global window does not have a natural end at which we could process the aggregated elements.




## Links

https://github.com/apache/flink/tree/master/flink-python/pyflink/examples
https://github.com/decodableco/examples/tree/main
https://github.com/decodableco/examples/tree/main/pyflink-intro
https://www.decodable.co/blog/a-hands-on-introduction-to-pyflink
https://pyflink.readthedocs.io/en/main/getting_started/installation/local.html
https://quix.io/blog/pyflink-deep-dive
https://nightlies.apache.org/flink/flink-docs-master/api/python/reference/pyflink.datastream/functions.html