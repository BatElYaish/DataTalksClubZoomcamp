# Kafka Theory

![kafka_concepts](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/6-Streaming/Kafka/Images/kafka_concepts.png "kafka_concepts")

Kafka clients = consumers and producers

## Producer

Writing data to topics. The producer decides in advance which partition to write to.  
We can send a key with the message. If the key is null, then the data will be written to partitions in a circular order 0,1,2... (round-robin). This way, the load is balanced.  
If the key has a value, all the messages that have the same key will go to the same partition. This is used when ordering is a must.  

**Producer Acknowledgment (acks)**  
Producers can choose to receive a verification (Acknowledgment) that the data was written.  
Acknowledgment types:

* `acks=0`: Producer won't wait for acknowledgment (possible data loss)
* `acks=1`: Producer will wait for leader acknowledgment (limited data loss)
* `acks=all`: Leader + all in-sync replicas (ISR) acknowledgment (no data loss, unless all ISR go down at once)

## Topic

A topic is a container holding a collection of events.  
Each topic has partitions, and the data is written to these partitions. Each write gets an incremental ID (offset). Each partition has its own offset numbering, independent of other partitions. The order that we get the data is guaranteed **only within a partition**, not across partitions.  
For example, in partition 0, offset 0 comes before offset 1, but in partition 1, offset 2 can come before partition 0, offset 1.  
The data is assigned randomly to a partition unless you specify a key.  
You can only write data to a Kafka topic; no deletes or updates (immutable).  
Data is kept only for a limited time (default: 1 week).  

**Topic Replication Factor**  
Topic durability: Topics in Kafka should have replications across brokers, usually 3. This way, if a broker is down, another broker that has a replica of the topic can serve the data. As a rule, for a replication factor of **N**, you can permanently lose up to **N-1** brokers and still recover the data **only if those brokers were not leaders**.  
You can't have replications higher than the number of brokers that you have.

The master of a partition is called the **leader partition**, and all other partitions are replicas of the leader. If they are replicating the leader fast enough, the replicas are called **ISR (in-sync replicas)**, as opposed to **OSR (out-of-sync replicas)**.  
Producers can only send data to the broker that has the leader partition, and consumers can only read data from the broker that has the leader partition.  
If a broker is down, an ISR replica automatically becomes the new leader.  

Replica fetching (Kafka v2.4+): It is possible to configure consumers to read from the closest replica and not the leader. This may help reduce latency and decrease network costs in the cloud (same data center/region).  

![leader_partition](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/6-Streaming/Kafka/Images/leader_partition.png "leader_partition")

## Consumer

Consumes the topic that it is registered to.  

**Consumer Group**  
A consumer group consists of multiple consumers that work together to read data from a topic. The **consumer group as a whole** reads all the data in the topic, but each **individual consumer in the group only reads from a subset of partitions**.  
If there are more consumers than partitions, some consumers will be inactive.  

![consumer_group](https://github.com/BatElYaish/DataTalksClubZoomcamp/blob/main/6-Streaming/Kafka/Images/consumer_group.png "consumer_group")

**Consumer Offset**  
Kafka stores the offset at which a consumer group has been reading in a special topic named `__consumer_offsets`.  
When a consumer in a group has processed the data from Kafka, it should periodically tell Kafka to update its progress by committing the offset. This way, if a consumer dies, it will be able to resume from where it left off.  

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
