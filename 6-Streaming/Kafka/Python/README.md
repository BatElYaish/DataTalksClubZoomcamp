# Running Examples

1. Run producer.py - `py producer.py`  
2. You can go to <http://localhost:9021> and see the confluent UI  
3. Run consumer.py - `py consumer.py`  
    the consumer keeps listening to the kafka topic - line `self.consumer.subscribe(topics)` so it won't finish executing.  
    to stop it run `ctrl+c`
