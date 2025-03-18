import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;

public class ProducerDemo {

    private static final Logger log = LoggerFactory.getLogger(ProducerDemo.class.getSimpleName());
    public static void main(String[] args) {
        // Load .env file
        Dotenv dotenv = Dotenv.load();

        log.info("I am a kafka producer");

        //create producer properties
        Properties properties = new Properties();

        //connect to localhost - Access environment variables
        properties.setProperty("bootstrap.servers", dotenv.get("KAFKA_BOOTSTRAP_SERVERS"));

        //set producer properties
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());

        //create producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        // //create a producer record
        // ProducerRecord<String, String> producerRecord = 
        //     new ProducerRecord<String,String>("demo_java", "Hello world!");

        //*********Simple**********/
        //send data --- asynchronous operation
        // producer.send(producerRecord);

        //*********callback**********/
        //send data with callback - this will be executed every time a record is successfully sent or exception is thrown 
        // for (int i=0 ; i < 10; i++) {
        //     //create a producer record
        //     ProducerRecord<String, String> producerRecord = 
        //     new ProducerRecord<>("demo_java", "Hello world! "+i);

        //     producer.send(producerRecord,  new Callback() {
        //         @Override
        //         public void onCompletion(RecordMetadata metadata, Exception e) {
        //             if (e== null) { //record was successfully sent
        //                 // the record was successfully sent
        //                 log.info("Received new metadata \n" +
        //                         "Topic: " + metadata. topic() + "\n" +
        //                         "Partition: " + metadata.partition() + "\n" +
        //                         "Offset: " + metadata.offset() + "\n" +
        //                         "Timestamp: " + metadata.timestamp());
        //             } else {
        //                 log.error("Error while producing ", e);
        //             }
        //         }
        //     });
        // }
        
        //*********key**********/
        // send value with key - same key = send to the same partition
        for (int j=0; j<3 ; j++) {
            for (int i=0 ; i < 5; i++) {
                String topic = "demo_java";
                String key = "id_" + i;
                String value = "Hello world! "+i;
    
                //create a producer record
                ProducerRecord<String, String> producerRecord = 
                    new ProducerRecord<>(topic, key,value);
    
                producer.send(producerRecord,  new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception e) {
                        if (e== null) { //record was successfully sent
                            // the record was successfully sent
                            log.info("key: " + key + " | Partition: " + metadata.partition());
                        } else {
                            log.error("Error while producing ", e);
                        }
                    }
                });
            }
        }

        // flush producer - tells the producer to send all data and block until done - synchronous operation
        // producer.flush();

        //close producer, does flush+close
        producer.close();

        System.out.println("Hi!");
    
    }
}
