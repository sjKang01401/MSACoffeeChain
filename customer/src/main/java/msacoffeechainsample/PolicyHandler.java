package msacoffeechainsample;

import msacoffeechainsample.config.kafka.KafkaProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){

    }

    @Autowired
    CustomerInfoRepository customerInfoRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverStatusUpdated_(@Payload StatusUpdated statusUpdated){

        if(statusUpdated.isMe()){

            System.out.println("##### listener  : " + statusUpdated.toJson());

            CustomerInfo customerInfo = customerInfoRepository.findById(statusUpdated.getCustomerId()).get();

            customerInfo.setScore( customerInfo.getScore() + statusUpdated.getQty() );

            customerInfoRepository.save(customerInfo);
        }
    }

}
