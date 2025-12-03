package com.aramirezm.rabbitpublisher;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/publish")
@RequiredArgsConstructor
public class PublisherController {

    private final AmqpTemplate rabbitTemplate;

    @PostMapping("/color")
    public String publishMessageColor(@RequestBody Models.Color color){
      this.rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_Color, color);
      return "Message Color published successfully";
    }

    @PostMapping("/shape")
    public ResponseEntity<Map<String, String>> publishMessageShape(@RequestBody Models.Shape shape){
      this.rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME, RabbitMQConfig.ROUTING_KEY_Shape, shape);
      return ResponseEntity.ok(Map.of("message", "Message Shape published successfully"));
    }
}
