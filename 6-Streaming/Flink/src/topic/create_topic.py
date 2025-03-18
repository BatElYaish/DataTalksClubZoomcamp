from kafka.admin import KafkaAdminClient, NewTopic

server = 'localhost:9092'
admin_client = KafkaAdminClient(
    bootstrap_servers=[server]
)

topic_name = 'events_test'

# Create topic if it doesn't exist
try:
    topic = NewTopic(
        name=topic_name,
        num_partitions=1,
        replication_factor=1
    )
    admin_client.create_topics([topic])
    print(f"Topic {topic_name} created")
except Exception as e:
    print(f"Topic creation error (might already exist): {e}")