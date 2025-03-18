import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.launchdarkly.eventsource.MessageEvent;
import com.launchdarkly.eventsource.background.BackgroundEventHandler;

public class StreamAPIWikimediaHandler implements BackgroundEventHandler{

    KafkaProducer<String, String> kafkaProducer;
    String topic;

    private final Logger log = LoggerFactory.getLogger(StreamAPIWikimediaHandler.class.getName());
    
    public StreamAPIWikimediaHandler(KafkaProducer<String,String> kafkaProducer, String topic){
        this.kafkaProducer = kafkaProducer;
        this.topic = topic;
    }


    @Override
    public void onOpen(){
        // pass
    }

    @Override
    public void onClosed(){
        kafkaProducer.close();;
    }

    @Override
    public void onMessage(String event, MessageEvent messageEvent){
        // log.info(messageEvent.getData());
        //asynchronous
        kafkaProducer.send(new ProducerRecord<>(topic, messageEvent.getData()));
    }

    @Override
    public void onComment(String comment){
        // pass
        
    }

    @Override
    public void onError(Throwable t) {
        log.error("Error in stream read: ",t);
    }
    
}
