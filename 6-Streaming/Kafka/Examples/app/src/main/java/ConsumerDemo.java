import java.time.Duration;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.CooperativeStickyAssignor;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.cdimascio.dotenv.Dotenv;

public class ConsumerDemo {

    private static final Logger log = LoggerFactory.getLogger(ConsumerDemo.class.getSimpleName());
    public static void main(String[] args) {
        // Load .env file
        Dotenv dotenv = Dotenv.load();

        log.info("I am a kafka consumer");

        String groupId = "my-java-app";
        String topic = "demo_java";

        //create consumer properties
        Properties properties = new Properties();

        //connect to localhost - Access environment variables
        properties.setProperty("bootstrap.servers", dotenv.get("KAFKA_BOOTSTRAP_SERVERS"));

        //set consumer properties
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        //set the consumer group
        properties.setProperty("group.id", groupId);
        // Partition assignment strategy
        properties.setProperty("partition.assignment.strategy", CooperativeStickyAssignor.class.getName());
        //static assignment
        // properties.setProperty("group.instance.id", "<>");
        


        //resetting offset properties
        // none - if we don't have existing consumer group then we fail
        //earliest - read from the beginning of the topic , same as CLI --from-beginning
        //latest - only read new messages 
        properties.setProperty("auto.offset.reset", "earliest");

        //create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        //create a shutdown hook
        //get reference to the main thread
        final Thread mainThread = Thread.currentThread();
        //adding shutdown hook
        Runtime.getRuntime().addShutdownHook((new Thread(){
            public void run() {
                log.info("Detected a shutdown, let's exit by calling consumer.wakeup()...");
                //the next time we'll do a consumer poll in the code it'll throw a wakeup exception, so it'll stop the loop and continue with the code
                consumer.wakeup();

                // we want to make sure that the shutdown hook is waiting for the main program to finish
                //join the main thread to allow the execution of the code in the main thread
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }));

        
        try {
            //subscribe to a topic
            consumer.subscribe(Arrays.asList(topic));
            
            //poll the data
            while (true) {
                log.info("Polling");

                //how long we are willing to wait te receive data, 
                //if there is data it will poll immediately
                //if there is no data we are willing to wait 1 second to receive data from kafka , this meant to not overload kafka
                ConsumerRecords<String,String> records = 
                    consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String,String> record: records) {
                    log.info("key" + record.key() + ", value: "+ record.value() + ", Partition: "+ record.partition() + ", offset: "+ record.offset());
                }

            }
        } catch (WakeupException e) {
            log.info("consumer is starting to shut down");
        } catch (Exception e) {
            log.error("Unexpected exception", e);
        } finally {
            consumer.close(); //this will also commit the offsets
            log.info("The consumer is now closed.");
        }
        

        
    
    }
}
