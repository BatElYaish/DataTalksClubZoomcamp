import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Properties;
// import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.google.gson.JsonParser;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;

import io.github.cdimascio.dotenv.Dotenv;

public class ConsumerStreamAPIWikimedia {

    
    private static final Logger log = LoggerFactory.getLogger(ConsumerStreamAPIWikimedia.class.getSimpleName());

    private static String extractJsonId(String json){
        return JsonParser.parseString(json)
                        .getAsJsonObject()
                        .get("meta")
                        .getAsJsonObject()
                        .get("id")
                        .getAsString();
    }
    

    public static void main(String[] args) throws InterruptedException {
        // Load .env file
        Dotenv dotenv = Dotenv.load();
        // Load the GCS credentials path from an environment variable
        String gcsCreds = dotenv.get("GCP_CRED_PATH");
        String gcsBucket = dotenv.get("GSC_BUCKET"); 
        // Load the GCS credentials from the JSON file
        Credentials credentials;
        try (FileInputStream fis = new FileInputStream(gcsCreds)) {            
            credentials = GoogleCredentials.fromStream(fis);        
        } catch (IOException e) {            
            e.printStackTrace();            
            return;        
        }
        // Create a GCS storage client
        Storage storage = StorageOptions.newBuilder()                
                            .setCredentials(credentials)                
                            .build()                
                            .getService();

        //create consumer properties
        Properties properties = new Properties();

        //connect to localhost - Access environment variables
        properties.setProperty("bootstrap.servers", dotenv.get("KAFKA_BOOTSTRAP_SERVERS"));

        //set consumer properties
        properties.setProperty("key.deserializer", StringDeserializer.class.getName());
        properties.setProperty("value.deserializer", StringDeserializer.class.getName());

        //set the consumer group
        properties.setProperty("group.id", "wikimedia_group");
        //create consumer
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);

        String topic = "wikimedia";


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

                // Process the messages and write them to GCS
                for (ConsumerRecord<String, String> record : records) {

                    String message = record.value();
                    String recordId = extractJsonId(message);
                    String folderPath = topic+"/"+"Landed/"+LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));                   
                    BlobId blobId = BlobId.of(gcsBucket, folderPath + "/" + recordId+".json");
                    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
                    Blob blob = storage.create(blobInfo, message.getBytes());
                    log.info("Wrote message to GCS: " + blob.getName());
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
