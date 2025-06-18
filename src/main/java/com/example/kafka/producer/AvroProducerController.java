package com.example.kafka.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/send")
public class AvroProducerController {

    @Autowired
    private AvroProducer producer;

    @GetMapping("/{count}")
    public String send(@PathVariable int count) {
        producer.sendRandomUsers(count);
        return "Sent " + count + " fake users to Kafka!";
    }
}
