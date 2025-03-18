import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

import com.launchdarkly.eventsource.ConnectStrategy;
import com.launchdarkly.eventsource.EventSource;
import com.launchdarkly.eventsource.background.BackgroundEventSource;
import com.launchdarkly.eventsource.background.BackgroundEventHandler;

import io.github.cdimascio.dotenv.Dotenv;

public class ProducerStreamAPIWikimedia {

    
    // private static final Logger log = LoggerFactory.getLogger(ProducerStreamAPIWikimedia.class.getSimpleName());
    public static void main(String[] args) throws InterruptedException {
        // Load .env file
        Dotenv dotenv = Dotenv.load();

        //create producer properties
        Properties properties = new Properties();

        //connect to localhost - Access environment variables
        properties.setProperty("bootstrap.servers", dotenv.get("KAFKA_BOOTSTRAP_SERVERS"));

        //set producer properties
        properties.setProperty("key.serializer", StringSerializer.class.getName());
        properties.setProperty("value.serializer", StringSerializer.class.getName());
        properties.setProperty(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        properties.setProperty(ProducerConfig.BATCH_SIZE_CONFIG, Integer.toString(32*1024));
        properties.setProperty(ProducerConfig.LINGER_MS_CONFIG, "20");

        //create producer
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);

        String topic = "wikimedia";

        //Create event for streaming data
        BackgroundEventHandler BackgroundEventHandler = new StreamAPIWikimediaHandler(producer, topic);
        
        String url = "https://stream.wikimedia.org/v2/stream/recentchange";

        EventSource.Builder builder = new EventSource.Builder(
            ConnectStrategy.http(URI.create(url))
            .connectTimeout(1, TimeUnit.MINUTES)
        );
        
        BackgroundEventSource eventSource = new BackgroundEventSource.Builder(BackgroundEventHandler, builder).build();
        // Start the producer in another thread
        eventSource.start();


        //to keep the stream open for x minutes
        TimeUnit.MINUTES.sleep(2);
    
    }
}
