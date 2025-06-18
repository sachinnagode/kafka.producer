package com.example.kafka.producer;

import com.github.javafaker.Faker;
import com.sachin.kafka.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class AvroProducer {

    private static final String TOPIC = "test-avro-topic";
    private final Faker faker = new Faker();
    @Autowired
    private KafkaTemplate<String, User> kafkaTemplate;
    public void sendRandomUsers(int count) {
        for (int i = 0; i < count; i++) {
            try {
                Thread.sleep(100);
                User user = new User(faker.name().firstName(), faker.number().numberBetween(18, 60));
                kafkaTemplate.send(TOPIC, user);
                System.out.println("Sent user: " + user);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
