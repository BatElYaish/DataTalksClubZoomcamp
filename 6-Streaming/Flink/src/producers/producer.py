# import json
# import time
# from kafka import KafkaProducer

# def json_serializer(data):
#     return json.dumps(data).encode('utf-8')

# server = 'localhost:9092'

# producer = KafkaProducer(
#     bootstrap_servers=[server],
#     value_serializer=json_serializer
# )
# t0 = time.time()

# topic_name = 'events_test'

# for i in range(10, 1000):
#     message = {'test_data': i, 'event_timestamp': time.time() * 1000}
#     producer.send(topic_name, value=message)
#     print(f"Sent: {message}")
#     time.sleep(0.05)

# producer.flush()

# t1 = time.time()
# print(f'took {(t1 - t0):.2f} seconds')

import json
import time
from kafka import KafkaProducer
from kafka.errors import KafkaError

def json_serializer(data):
    return json.dumps(data).encode('utf-8')

def on_success(record):
    print(f"Message successfully delivered to topic {record.topic}, partition {record.partition}, offset {record.offset}")

def on_error(excp):
    print(f'Failed to deliver message: {excp}')

server = 'localhost:9092'

producer = KafkaProducer(
    bootstrap_servers=[server],
    value_serializer=json_serializer,
    acks='all',  # Wait for all replicas
    retries=5,   # Retry sending on failure
    # Add these configurations for better debugging
    api_version_auto_timeout_ms=30000,
    request_timeout_ms=45000,
    metadata_max_age_ms=300000
)

t0 = time.time()
topic_name = 'events_test'

try:
    for i in range(10, 1000):
        message = {'test_data': i, 'event_timestamp': int(time.time() * 1000)}
        
        # Send with callbacks
        future = producer.send(topic_name, value=message)
        future.add_callback(on_success).add_errback(on_error)
        
        # print(f"Sending: {message}")
        time.sleep(0.05)
    
    # Wait for all messages to be sent
    producer.flush(timeout=60)

except Exception as e:
    print(f"An error occurred: {e}")

finally:
    # Close the producer
    producer.close()
    t1 = time.time()
    print(f'took {(t1 - t0):.2f} seconds')

