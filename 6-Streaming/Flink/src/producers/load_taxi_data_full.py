import csv
import json
from kafka import KafkaProducer
import time


def main():
    # Create a Kafka producer
    producer = KafkaProducer(
        bootstrap_servers='localhost:9092',
        value_serializer=lambda v: json.dumps(v).encode('utf-8')
    )
    t0 = time.time()
    csv_file = 'data/green_tripdata_2019-10.csv'  # change to your CSV file path if needed

    with open(csv_file, 'r', newline='', encoding='utf-8') as file:
        reader = csv.DictReader(file)

        for row in reader:
            # Send data to Kafka topic "green-trips-full"
            producer.send('green-trips-full', value=row)

    # Make sure any remaining messages are delivered
    producer.flush()
    producer.close()
    
    t1 = time.time()
    print(f'took {(t1 - t0):.2f} seconds')


if __name__ == "__main__":
    main()