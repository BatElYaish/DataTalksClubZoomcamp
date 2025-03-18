# kafka CLI

## Access the CLI from docker

1. run to locate the ID of the broker `docker ps | findstr broker`
2. run `docker exec -it <container ID> /bin/bash`

## Commands

All commands has to be with a link to the kafka bootstrap server with `--bootstrap-server broker:29092`

## Topics

**Create**  
Create a topic : `kafka-topics --bootstrap-server broker:29092 --create --topic first topic`  
Create topic with 5 partitions : `kafka-topics --bootstrap-server broker:29092 --create --topic second_topic --partitions 5`  
Create a topic with replication factor of 2: `kafka-topics --bootstrap-server broker:29092 --create --topic third_topic --replication-factor 2` (Won't work on local host)

**List**  
List: `kafka-topics --bootstrap-server broker:29092 --list`  
List topics that wwe created (not internal topics starting with underscore): `kafka-topics --bootstrap-server broker:29092 --list | grep -v '^_'`  
List topics with rides in the name: `kafka-topics --bootstrap-server broker:29092 --list | grep rides`  

**Describe**  
`kafka-topics --bootstrap-server broker:29092 --topic second_topic --describe`  
output:  
Topic: second_topic     TopicId: jrjQLjZQTE-AWrHmGWHpNQ PartitionCount: 3       ReplicationFactor: 1  
Configs:  
        Topic: second_topic     Partition: 0    Leader: 1       Replicas: 1     Isr: 1  
        Topic: second_topic     Partition: 1    Leader: 1       Replicas: 1     Isr: 1  
        Topic: second_topic     Partition: 2    Leader: 1       Replicas: 1     Isr: 1  

**Delete**  
`kafka-topics --bootstrap-server broker:29092 --topic first_topic --delete`

**Configurations**
Add config:  
`kafka-configs --bootstrap-server broker:9092 --entity-type topics --entity-name configured-topic --alter --add-config min.insync replicas=2`  
Delete config:  
`kafka-configs --bootstrap-server broker:9092 --entity-type topics --entity-name configured-topic --alter --delete-config min.insync replica`

## Producer

**Produce**  
Write to kafka  
`kafka-console-producer --bootstrap-server broker:29092 --topic first_topic`  
This will show a chevron `>` that you are ready to produce, every line break is a new message. To stop producing type `ctrl+c`.

Produce with properties  
`kafka-console-producer --bootstrap-server broker:29092 --topic first_topic --producer-property acks=all`  
To produce to one partition at a time and to every partition, if not, it will send to the same partition up to 16kb of data and then it will switch partition (do not use round robin in production):  
`kafka-console-producer --bootstrap-server broker:29092 --producer-property partitioner.class=org.apache.kafka.clients.producer.RoundRobinPartitioner --topic second_topic`  

Producing with key  
`kafka-console-producer --bootstrap-server broker:29092 --topic first_topic --property parse.key=true --property key.separator=:`  
Everything before the `:` is the key, everything after is the value of the message  
`>example key:example value`  
`>name:BatEl`  

Producing to a non-existing topic - on local machine it will show warnings at the beginning but will work eventually. On server it may fail.

## Consumer

### Single Consumer

Consume from topic named "second_topic"  
From now on: `kafka-console-consumer --bootstrap-server broker:29092 --topic second_topic`  
From the first message in the topic: `kafka-console-consumer --bootstrap-server broker:29092 --topic second_topic --from-beginning`  
If we have multiple partitions it will not read by the order that we produced the data, it will read by partitions  
Print from beginning with key, value, timestamp and formatting:  
`kafka-console-consumer --bootstrap-server broker:29092 --topic second_topic --formatter kafka.tools.DefaultMessageFormatter --property print.timestamp=true --property print.key=true --property print.value=true --property print.partition=true --from-beginning`  
output:

|Timestamp|Partition|Key|Value|
|--------------------------|-------------|------|-----------------|
|CreateTime:1739612298576  |Partition:1  |null  |hello world      |
|CreateTime:1739612348200  |Partition:1  |null  |this is a test   |
|CreateTime:1739612335244  |Partition:0  |null  |this is working  |
|CreateTime:1739612483223  |Partition:0  |null  |we are here      |
|CreateTime:1739612321270  |Partition:2  |null  |my name is BatEl |
|CreateTime:1739612411111  |Partition:2  |null  |try this         |

### Consumer Group

Create consumer with a consumer group: `kafka-console-consumer --bootstrap-server broker:29092 --topic third_topic --group cons_grp`  
Creating 2 consumers to subscribe to third_topic with 3 partition, we can see that the partitions are spread across the consumers by kafka  
If we stop the consumer but keep on producing and then we create a new consumer to the same group it will catch on the lag it had from before and consume the lag
If we create a consumer in the same consumer group with the flag `--from-beginning` it won't consume anything because the offset has been committed to kafka
If we create a new consumer group and read from the beginning we'll read all the messages  

**List**  
`kafka-consumer-groups --bootstrap-server broker:29092 --list`  

**Describe**  
`kafka-consumer-groups --bootstrap-server broker:29092 --describe --group cons_grp`  

output when we have no consumers and we are producing:  

|GROUP     |TOPIC        |PARTITION |CURRENT-OFFSET | LOG-END-OFFSET|LAG   |CONSUMER-ID |HOST |CLIENT-ID |
|----------|-------------|----------|---------------|---------------|------|------------|-----|----------|
|cons_grp  |third_topic  |0         |8              |11             |3     |-           | -   |  -       |
|cons_grp  |third_topic  |1         |8              |11             |3     |-           | -   |  -       |
|cons_grp  |third_topic  |2         |9              |13             |4     |-           | -   |  -       |

CURRENT-OFFSET - the offset committed in each partition  
LOG-END-OFFSET - what is the current log offset (with the messages that haven't been consumed yet)  
LAG - the number of messages the haven't been consumed yet  
LOG-END-OFFSET - CURRENT-OFFSET = LAG  

Output when we have 2 consumers:

|GROUP     | TOPIC      | PARTITION |CURRENT-OFFSET|LOG-END-OFFSET | LAG |CONSUMER-ID                                             |HOST        |CLIENT-ID        |
|----------|------------|-----------|--------------|---------------|-----|--------------------------------------------------------|------------|-----------------|
|cons_grp  |third_topic | 0         |16            |  16           |   0 |  console-consumer-49c756f8-2b0e-488e-8a17-6e8ec7f5c2e7 |/172.19.0.3 |console-consumer |
|cons_grp  |third_topic | 1         |16            |  16           |   0 |  console-consumer-49c756f8-2b0e-488e-8a17-6e8ec7f5c2e7 |/172.19.0.3 |console-consumer |
|cons_grp  |third_topic | 2         |17            |  17           |   0 |  console-consumer-ade7c658-5bf1-49f4-b910-006eddbd670c |/172.19.0.3 |console-consumer |

The we have one consumer subscribed to partition 0 and 1 with ID of 2e7 at the end, and one consumer with ID 70c consuming partition 2.

**Resetting offsets**  

If we want to reset the committed offset so that the consumer in the consumer group will read from the beginning
The consumers of the group must be off when running reset

Dry run without executing:  
`kafka-consumer-groups --bootstrap-server broker:29092 --group cons_grp --reset-offsets --to-earliest --topic third_topic --dry-run`
output:  

|GROUP   |TOPIC       |PARTITION| NEW-OFFSET|
|--------|------------|---------|-----------|
|cons_grp|third_topic |0        | 0         |
|cons_grp|third_topic |1        | 0         |
|cons_grp|third_topic |2        | 0         |

it states that the new offset will bw 0 because it's the beginning of the topic  

Executing:  
`kafka-consumer-groups --bootstrap-server broker:29092 --group cons_grp --reset-offsets --to-earliest --topic third_topic --execute` (the output will be the same)

if we run describe group:

|GROUP     |TOPIC       |PARTITION |CURRENT-OFFSET |LOG-END-OFFSET |LAG  |
|----------|------------|----------|---------------|---------------|-----|
|cons_grp  |third_topic |0         |0              |16             |16   |
|cons_grp  |third_topic |1         |0              |16             |16   |
|cons_grp  |third_topic |2         |0              |17             |17   |

Shifting offsets:  
2 forward:  
`kafka-consumer-groups --bootstrap-server broker:29092 --group cons_grp --reset-offsets --shift-by 2 --execute --topic first_topic`  
2 backward:  
`kafka-consumer-groups --bootstrap-server broker:29092 --group cons_grp --reset-offsets --shift-by -2 --execute --topic first_topic`  
