# Kafka Theory

![Kafka Concepts](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/6-Streaming/Kafka/Images/kafka_concepts.png "Kafka Concepts")

Kafka clients consist of consumers and producers.

## Producer

Producers write data to topics. The producer decides in advance which partition to write to.  
We can send a key with the message. If the key is null, then data will be written to partitions in a circular order (0,1,2...) using a round-robin approach. This way, the load is balanced.  
If the key has a value, all messages with the same key will go to the same partition. This is used when ordering is essential.

**Producer Acknowledgment (acks)**  
Producers can choose to receive verification (acknowledgment) that the data was written.  
Acknowledgment types:

* `acks=0`: Producer won't wait for acknowledgment (possible data loss).  
  Producers consider messages as "written successfully" the moment the message is sent, without waiting for the broker to accept it.  
  If the broker goes offline or an exception occurs, we won't know and will lose data. This setting is useful for data where it's okay to potentially lose messages, such as metrics collection.  
  Produces the highest throughput because network overhead is minimized.

* `acks=1`: Producer will wait for leader acknowledgment (limited data loss).  
  Producers consider messages as "written successfully" when the message is acknowledged by the leader only.  
  Leader response is requested, but replication is not guaranteed as it happens in the background.  
  If the leader broker goes offline unexpectedly but replicas haven't replicated the data yet, data loss occurs.  
  If an ack is not received, the producer may retry the request.  
  This setting provides more overhead but more safety, with no guarantee that data is replicated.

* `acks=all`/`acks=-1`: Leader and all in-sync replicas (ISR) acknowledgment (no data loss, unless all ISR go down at once).  
  Producers consider messages as "written successfully" when the message is accepted by all in-sync replicas (ISR).  
  This setting works with `min.insync.replicas`. The leader replica for a partition checks to see if there are enough in-sync replicas for safely writing the message.  
  `min.insync.replicas=1`: Only the broker leader needs to successfully ack.  
  `min.insync.replicas=2`: At least the broker leader and one replica need to ack.  
  If `min.insync.replicas=N` and you only have `<N` brokers, the broker will respond `NOT_ENOUGH_REPLICAS`, preferring not to write the data if there is a potential data loss.

  **Topic Availability** (considering RF=3):  
  * `acks=0 & acks=1`: If one partition is up and considered an ISR, the topic will be available for writes.  
  * `acks=all`:  
    * `min.insync.replicas=1` (default): The topic must have at least 1 partition up as an ISR (including the leader), allowing two brokers to be down.  
    * `min.insync.replicas=2`: The topic must have at least 2 ISR up, allowing at most one broker to be down (with a replication factor of 3), ensuring that for every write, data will be written at least twice.  
    * `min.insync.replicas=3`: Makes little sense for a replication factor of 3 as no broker could be down.

  In summary, when `acks=all` with a replication factor=N and `min.insync.replicas`=M, we can tolerate N-M brokers going down for topic availability purposes.

  `acks=all` and `min.insync.replicas=2` with `RF=3` is the most popular option for data durability and availability, allowing you to withstand the loss of at most one Kafka broker.

**Producer Settings**  
* `retry.backoff.ms` (default 100ms): How much time to wait before the next retry.  
* `delivery.timeout.ms` (default 2 minutes): How long from the moment you send until it is received by Kafka, otherwise the send will fail.  
* `enable.idempotence`: When a producer sends a message that is received by Kafka but doesn't get the ack back due to a network error, it will retry sending the message again. Kafka will commit the message even though it's a duplicate and send an ack back to the producer. When set to idempotent, Kafka will not commit the duplicate and will send an ack back.  
* `compression.type`: How to compress the data before sending it to Kafka. Options: none (default), gzip, lz4, snappy, zstd. Compression can be set at the Broker (all topics) or at a topic level.  
  Compression is more effective with larger batches of messages sent to Kafka.

  **Advantages**:  
  * Much smaller producer request size.  
  * Faster to transfer data over the network, resulting in less latency.  
  * Better throughput and disk utilization in Kafka (stored messages on disk are smaller).

  **Disadvantages**:  
  * Producers and consumers must commit some CPU cycles to compression and decompression.

* `max.in.flight.requests.per.connection` (default 5): Maximum message batches being in-flight (sent between the producer and the broker) at a time. If the maximum number is reached and more messages are incoming, Kafka will start to batch the messages before the next send. This increases throughput while maintaining low latency, with the added benefit of better compression in batches.
* `linger.ms` (default 0): How long to wait until we send a batch. Adding a small number, for example, 5ms, helps add more messages in the batch at the expense of latency.
* `batch.size` (default 16KB): If a batch is filled before `linger.ms`, increase the batch size. Increasing the batch size can help increase compression, throughput, and efficiency of requests because we'll be sending fewer of them. If a message is bigger than the batch size, it will not be batched. A batch is allocated per partition, so setting it too high will waste memory.

**To achieve a high throughput producer**:

* Increase `linger.ms`, and the producer will wait a few milliseconds for the batches to fill up before sending them.  
* If you're sending full batches and have memory to spare, increase `batch.size` to send larger batches.  
* Introduce some producer-level compression in `compression.type` for more efficiency in sends.

**Partitioner Key**  
When a key is used, it goes through a process of "key hashing" to map it to a partition. Using a hash algorithm, the same key will go to the same partition. If the number of partitions is increased, the connection of key-partition is not guaranteed, and data might get into the wrong partition. It's best to start a new topic if partitions need to be added.  
You can create your own hashing algorithm class and use it in the producer property `partitioner.class`.

When the key is null, the producer uses the default partitioner.  
In Kafka <= 2.3, the default is Round-robin, leading to more batches (one batch per partition) and smaller batches (one message per batch).  
In Kafka >= 2.4, the default is a sticky partitioner. When the batch is full or `linger.ms` has elapsed, the records are sent to a partition, then move to the next batch in the next partition. This results in larger batches and reduces latency because larger requests and `batch.size` are more likely to be reached.

**Partition Count**  
More partitions imply:

* Better parallelism.  
* Better throughput.  
* Ability to run more consumers in a group to scale (max as many consumers per group as partitions).  
* Ability to leverage more brokers if you have a large cluster.  
* BUT more elections to perform for Zookeeper (if using Zookeeper).  
* BUT more files opened on Kafka.

**Guidelines**:

* Small cluster (< 6 brokers): 3 x # of brokers.  
* Big cluster (> 12 brokers): 2 x # of brokers.  
* Adjust for the number of consumers you need to run in parallel at peak throughput.  
* Adjust for producer throughput (increase if super-high throughput or projected increase in the next 2 years).  
* TEST! Every Kafka cluster will have different performance.

## Topic

A topic is a container holding a collection of events.  
Each topic has partitions, and data is written to these partitions. Each write gets an incremental ID (offset). Each partition has its own offset numbering, independent of other partitions. Order is guaranteed **only within a partition**, not across partitions.  
For example, in partition 0, offset 0 comes before offset 1, but in partition 1, offset 2 can come before partition 0, offset 1.  
Data is assigned randomly to a partition unless you specify a key.  
You can only write data to a Kafka topic; no deletes or updates (immutable).  
Data is kept only for a limited time (default: 1 week).

**Topic Replication Factor**  
Topic durability: Topics in Kafka should have replications across brokers, usually 3. This way, if a broker is down, another broker that has a replica of the topic can serve the data. As a rule, for a replication factor of **N**, you can permanently lose up to **N-1** brokers and still recover the data **only if those brokers were not leaders**.  
You can't have replications higher than the number of brokers that you have.

The master of a partition is called the **leader partition**, and all other partitions are replicas of the leader. If they are replicating the leader fast enough, the replicas are called **ISR (in-sync replicas)**, as opposed to **OSR (out-of-sync replicas)**.  
Producers can only send data to the broker that has the leader partition, and consumers can only read data from the broker that has the leader partition.  
If a broker is down, an ISR replica automatically becomes the new leader.

**Partition Leaders and Replica Fetching**: By default, Kafka consumers read from the partition leader, which can incur higher latency and network costs if the consumer and leader are in different data centers/availability zones (AZs).

**Rack Awareness and Consumer Replica Fetching**: Since Kafka 2.4, consumers can be configured to read from the closest replica instead of the leader, which can reduce latency and network costs, especially when using cloud infrastructure.

**Configuration Steps**:  

* Brokers must be on Kafka version 2.4 or later.  
* Brokers must be configured with their rack ID, which represents the data center/AZ.  
* Consumers must be configured to use the "rack-aware replica selector" class.  
* Consumers must be configured with the "client.rack" setting matching their data center/AZ.

**Benefits**:  

* Lower network costs when consumers read from replicas in the same data center/AZ.  
* Lower latency for consumers due to reading from the closest replica.

The key idea is to leverage Kafka's support for rack awareness and replica selection to optimize consumer performance and reduce network costs, especially when running in a multi-data center or cloud environment.

**Choosing the Replication Factor**:  
Should be at least 2, usually 3, maximum 4.  
The higher the replication factor (N):

* Better durability of your system (N-1 brokers can fail).  
* Better availability of your system (N-min.insync.replicas if producer acks=all).  
* BUT more replication (higher latency if acks=all).  
* BUT more disk space on your system (50% more if RF is 3 instead of 2).

**Guidelines**:

* Set it to 3 to get started (you must have at least 3 brokers for that).  
* If replication performance is an issue, get a better broker instead of less RF.  
* Never set it to 1 in production.

![Leader Partition](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/6-Streaming/Kafka/Images/leader_partition.png "Leader Partition")

**Topic Naming Conventions**  
`<message type>.<dataset name>.<data name>.<data format>`

**Message Type**:

* logging: For logging data (slf4j, syslog, etc).  
* queuing: For classical queuing use cases.  
* tracking: For tracking events such as user clicks, page views, ad views, etc.  
* etl/db: For ETL and CDC use cases such as database feeds.  
* streaming: For intermediate topics created by stream processing pipelines.  
* push: For data that's being pushed from offline (batch computation) environments into online environments.  
* user: For user-specific data such as scratch and test topics.

The dataset name is analogous to a database name in traditional RDBMS systems. It's used as a category to group topics together.  
The data name field is analogous to a table name in traditional RDBMS systems, though it's fine to include further dotted notation if developers wish to impose their own hierarchy within the dataset namespace.  
The data format, for example, .avro, .json, .text, .protobuf, .csv, .log.

**Segments**  
Each partition is made of segments (files), and there is only one active file in a partition at a time - the file we are currently writing to. We know the starting offset, but we don't know its ending offset because we haven't finished writing to it.  
Segments have 2 indexes:  

* An offset-to-position index: Helps Kafka find where to read from to find a message.  
* A timestamp-to-offset index: Helps Kafka find messages with a specific timestamp.

![Segment Indexes](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/6-Streaming/Kafka/Images/segment_indexes.png "Segment Indexes")

**Logs**  
Kafka topics have data that expire according to a policy (default 1 week). A "log cleanup" occurs when data expires.  
Log cleanup happens whenever a segment is created. If you have smaller or more segments, log cleanup will occur more often. It takes up CPU and RAM resources to decide what to delete, so it shouldn't happen too often. The "cleaner" checks for work every 15 seconds.
Log compaction will keep the latest data for each key in the topic. It does not change the offsets; it just deletes keys whenever new keys are available. Any consumer reading from the tail of a log (most current data) will still see all the messages sent to the topic. The ordering of messages is kept; log compaction only removes some messages but does not re-order them. The offset of a message is immutable (it never changes). Offsets are just skipped if a message is missing. Deleted records can still be seen by consumers for a period of `delete.retention.ms` (default is 24 hours).

![Log Compaction](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/6-Streaming/Kafka/Images/log_compaction.png "Log Compaction")

**Topic Configuration**  
**Segment Settings**:

* `log.segment.bytes` (default 1GB): The max size of a single segment in bytes. If it becomes over 1GB, the file will close and another one will open.  
  If you set a smaller `log.segment.bytes`, log compaction happens more often, but Kafka must keep more files opened, which can cause the error: "Too many open files." If you have very high throughput, no need to change, but if you have very low throughput, maybe it is best to change it so you can have more segments and trigger log compaction more often.  
* `log.segment.ms` (default 1 week): The time Kafka will wait before committing the segment if not full.  
  If you set a smaller `log.segment.ms`, you set a max frequency for log compaction (more segments mean more frequent triggers).  
* `log.retention.hours` (default is 168 - one week): The number of hours to keep data for. A higher number means more disk space. A lower number means that less data is retained (if your consumers are down for too long, they can miss data). Other parameters allowed: `log.retention.ms`, `log.retention.minutes` (smaller unit has precedence).  
* `log.retention.bytes` (default is -1 - infinite): Max size in Bytes for each log partition. Useful to keep the size of a log under a threshold.  
* `log.cleanup.policy` (default delete): Delete based on data age (default 1 week) or delete based on the max size of the log (default is -1 == infinite).  
  compact - Deletes messages based on the last occurrence of the key; it will delete old duplicate keys after the active segment is committed.  
* `log.cleaner.backoff.ms` (default 15 seconds): How often the cleaner checks for work.  
* `delete.retention.ms` (default is 24 hours): How long consumers can still see deleted records.  
* `segment.ms` (default 7 days): Max amount of time to wait to close an active segment.  
* `segment.bytes` (default 1G): Max size of a segment.  
* `min.compaction.lag.ms` (default 0): How long to wait before a message can be compacted.  
* `delete.retention.ms` (default 24 hours): Wait before deleting data marked for compaction.  
* `min.cleanable.dirty.ratio` (default 0.5): Higher => less, more efficient cleaning. Lower => more cleaning, less efficient.

## Consumer

Consumes the topic that it is registered to.

**Consumer Group**  
A consumer group consists of multiple consumers that work together to read data from a topic. The **consumer group as a whole** reads all the data in the topic, but each **individual consumer in the group only reads from a subset of partitions**.  
If there are more consumers than partitions, some consumers will be inactive.

![Consumer Group](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/6-Streaming/Kafka/Images/consumer_group.png "Consumer Group")

**Partition Rebalance**  
Partition rebalancing involves moving partitions between consumers. This happens when a consumer joins or leaves the group or if new partitions are added to the topic.

**Rebalance Strategies**:  

* **Eager Rebalance** - Default. When a consumer joins the group, all other consumers stop; no consumer is reading from any partition. During a short period, the entire consumer group stops processing. Then all consumers join the group they were in and get a new partition assignment. The partitions are assigned randomly, with no guarantee that a consumer will get the same partition.  
* **Cooperative Rebalance (Incremental Rebalance)** - This will reassign a small subset of partitions from one consumer to another; other consumers without reassigned partitions can still process uninterrupted. It may go through several iterations until a stable assignment is found. This avoids all consumers stopping.

**In Kafka Consumer Properties**: `partition.assignment.strategy`

* `RangeAssignor`: Assign partitions on a per-topic basis (can lead to imbalance) (Eager).  
* `RoundRobin`: Assign partitions across all topics in a round-robin fashion, optimal balance (Eager).  
* `StickyAssignor`: Balanced like RoundRobin, and then minimizes partition movements when consumers join/leave the group to minimize movements (Eager).  
* `CooperativeStickyAssignor`: Rebalance strategy is identical to StickyAssignor but supports cooperative rebalance, allowing consumers to keep on consuming from the topic.

The default assignor is `[RangeAssignor, CooperativeStickyAssignor]`, which will use the RangeAssignor by default but allows upgrading to the CooperativeStickyAssignor with just a single rolling bounce that removes the RangeAssignor from the list.

**Kafka Log After a Consumer in the Group is Down**:  
Partitions for the current consumer are reassigned to it:  

* Assigned partitions: [demo_java-0, demo_java-1, demo_java-2]  
* Current owned partitions: [demo_java-1]  
* Added partitions (assigned - owned): [demo_java-0, demo_java-2]  
* Revoked partitions (owned - assigned): []

**Static Group Membership**  
By default, when a consumer leaves a group, its partitions are revoked and reassigned. If it joins back, it will have a new "member ID" and new partitions assigned.  
If you specify `group.instance.id`, it makes the consumer a "static member".  
Upon leaving, the consumer has up to `session.timeout.ms` to join back and get back its partitions (else they will be reassigned), without triggering a rebalance.  
This is helpful when consumers maintain local state and cache (to avoid rebuilding the cache).

**Consumer Offset**  
Kafka stores the offset at which a consumer group has been reading in a special topic named `__consumer_offsets`.  
When a consumer in a group has processed the data from Kafka, it should periodically tell Kafka to update its progress by committing the offset. This way, if a consumer dies, it will be able to resume from where it left off.

**Commit Strategies**:

* (Easy) `enable.auto.commit=true` & synchronous processing of batches  
  Offsets are auto-committed asynchronously when you call `.poll` and `auto.commit.interval.ms` has elapsed.

    ```java
    while(true){
        List<Records> batch = consumer.poll(Duration.ofMillis(100))
        doSomethingSynchronous(batch)
    }
    ```

* (Medium) `enable.auto.commit=false` & manual commit of offsets  
  This way, you have control over when you commit the offsets and the conditions for committing them.

    ```java
    while(true){
        batch += consumer.poll(Duration.ofMillis(100))
        if isReady(batch) {
        doSomethingSynchronous(batch)
        consumer.commitAsync();
        }
    }
    ```

**Delivery Semantics for Consumers**  
By default, Java consumers will automatically commit offsets (**at least once**).  
If you choose to commit manually:

* **At least once (usually preferred):**  
  Offsets are committed after the message is processed. If processing fails, the message will be read again, which can result in duplicate processing. Ensure that your processing is **idempotent** (i.e., processing the same message twice won’t impact your system).  
* **At most once:**  
  Offsets are committed as soon as messages are received. If processing fails, some messages will be lost (they won’t be read again).  
* **Exactly once:**  
  * For **Kafka → Kafka** workflows: use the **Transactional API** (easy with Kafka Streams API).  
  * For **Kafka → External Systems** workflows: use **Kafka Transactions** to ensure **exactly-once delivery** when interacting with external systems.

**Methods to Keep the Consumer Idempotent**:

1. If the message doesn't contain any ID, create a composite ID of the message from the Kafka record's coordinates: `record.topic() + record.partition() + record.offset()`.  
2. Use the ID of the records. Extract the ID from the record and use that as the file name/index.

**Consumer Threads**  
The heartbeat thread is the consumers sending messages to the broker periodically, indicating they are up.

* `heartbeat.interval.ms` (default 3 seconds) - How often to send heartbeats. Usually set to 1/3rd of `session.timeout.ms`.  
* `session.timeout.ms` (default 45 seconds) - Heartbeats are sent periodically to the broker. If no heartbeat is sent during that period, the consumer is considered dead. Set even lower for faster consumer re-balances.

The poll thread is responsible for other brokers thinking that the consumers are up because they are still requesting data from Kafka.

* `max.poll.interval.ms` (default 5 minutes) - Maximum amount of time between two `.poll()` calls before declaring the consumer dead. This is relevant for Big Data frameworks like Spark in case the processing takes time. This mechanism detects a data processing issue with the consumer (consumer is "stuck").  
* `max.poll.records` (default 500) - Controls how many records to receive per poll request. Increase if your messages are very small and have a lot of available RAM. Good to monitor how many records are polled per request. Lower if it takes too much time to process records.  
* `fetch.min.bytes` (default 1) - Controls how much data you want to pull at least on each request. Helps improve throughput and decrease request number at the cost of latency.  
* `fetch.max.wait.ms` (default 500) - The maximum amount of time the Kafka broker will block before answering the fetch request if there isn't sufficient data to immediately satisfy the requirement given by `fetch.min.bytes`. This means that until the requirement of `fetch.min.bytes` is satisfied, you will have up to 500 ms of latency before the fetch returns data to the consumer (introducing a potential delay to be more efficient in requests).  
* `max.partition.fetch.bytes` (default 1MB) - The maximum amount of data per partition the server will return. If you read from 100 partitions, you'll need a lot of memory (100 MB RAM).  
* `fetch.max.bytes` (default 55MB) - Maximum data returned for each fetch request. If you have available memory, try increasing `fetch.max.bytes` to allow the consumer to read more data in each request.

## Broker

A Kafka cluster is composed of multiple brokers (servers).  
Each broker is identified with its ID (integer).  
Each broker contains certain topic partitions.  
After connecting to any broker (**bootstrap broker**), you will be connected to the entire cluster.  
A good starting point is 3 brokers, but some large clusters have over 100 brokers.

**Kafka Broker Discovery**  
Every Kafka broker is also called a **bootstrap broker**, meaning you only need to connect to one broker, and the Kafka clients will discover the rest of the cluster.  
The Kafka client connects to a bootstrap broker and requests metadata. In return, it gets a list of all brokers, enabling it to connect to all brokers in the cluster.

![kafka_broker](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/6-Streaming/Kafka/Images/kafka_broker.png "kafka_broker")

## Zookeeper

* Zookeeper manages brokers (keeps a list of them).  
* Zookeeper helps in performing leader election for partitions.  
* Zookeeper sends notifications to Kafka in case of changes (e.g., new topic, broker dies, broker comes up, delete topics, etc.).  
* Zookeeper operates with an **odd number of servers** (1, 3, 5, 7).  
* Zookeeper has a **leader (writes)**, while the rest are **followers (reads)**.  
* (Zookeeper does **NOT** store consumer offsets for Kafka > v0.10).  

### Kafka Without Zookeeper (KRaft)

* In 2020, Apache Kafka started working on removing Zookeeper due to its **scalability issues** in clusters with **>100,000 partitions**.  
* From **Kafka 2.8+**, a **Zookeeper-less mode (KRaft - Kafka Raft)** was introduced.  
* From **Kafka 3.5+**, KRaft is **production-ready**.  

By removing Zookeeper, Kafka can:  
✔ Scale to millions of partitions, making it easier to maintain.  
✔ Improve stability, making monitoring and administration simpler.  
✔ Have a **single security model** for the whole system.  
✔ Reduce controller shutdown and recovery time.
