import java.io.FileInputStream;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;

import io.github.cdimascio.dotenv.Dotenv;

public class ConsumerStreamAPIWikimediaBulk {

    
    private static final Logger log = LoggerFactory.getLogger(ConsumerStreamAPIWikimediaBulk.class.getSimpleName());

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
            consumer.subscribe(Arrays.asList(topic));
            
            while (true) {
                log.info("Polling");
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(3000));
                if (!records.isEmpty()) {
                    // Prepare to collect JSON objects
                    List<Object> jsonObjectList = new ArrayList<>();
                    ObjectMapper objectMapper = new ObjectMapper();
                    
                    for (ConsumerRecord<String, String> record : records) {
                        try {
                            // Parse the JSON value and add it to the list
                            Object jsonObject = objectMapper.readValue(record.value(), Object.class);
                            jsonObjectList.add(jsonObject);
                        } catch (Exception e) {
                            log.error("Error parsing JSON: " + record.value(), e);
                        }
                    }

                    // Only write if there are JSON objects
                    if (!jsonObjectList.isEmpty()) {
                        // Convert the list of JSON objects to a JSON array string
                        String jsonArray = objectMapper.writeValueAsString(jsonObjectList);

                        // Wait for a specified time before writing the records to GCS
                        TimeUnit.SECONDS.sleep(5);

                        // Write the JSON array string to a single blob
                        String fileName = String.format("records_%s.json", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss-SSS")));
                        String folderPath = topic + "/Landed_Bulk/" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                        BlobId blobId = BlobId.of(gcsBucket, folderPath + "/" + fileName);
                        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                                                    .setContentType("application/json")
                                                    .build();

                        Blob blob = storage.create(blobInfo, jsonArray.getBytes());
                        log.info("Wrote JSON array to GCS: " + blob.getName());
                    }
                }
            }
        } catch (WakeupException e) {
            log.info("Consumer is starting to shut down");
        } catch (Exception e) {
            log.error("Unexpected exception", e);
        } finally {
            consumer.close();
            log.info("The consumer is now closed.");
        }
    
    }
}
