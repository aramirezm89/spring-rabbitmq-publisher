package com.aramirezm.rabbitpublisher;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clase de configuración de RabbitMQ para Spring Boot.
 * Define los componentes necesarios para publicar mensajes a RabbitMQ:
 * - Exchange: Punto de entrada de mensajes
 * - MessageConverter: Convierte objetos Java a JSON
 * - RabbitTemplate: Cliente para enviar mensajes
 */
@Configuration
public class RabbitMQConfig {

    // Nombre del exchange de tipo Topic donde se publicarán los mensajes
    public static final String EXCHANGE_NAME = "exchange";
    
    // Routing key para enrutar mensajes relacionados con colores a su cola específica
    public static final String ROUTING_KEY_Color = "color_routing_key";
    
    // Routing key para enrutar mensajes relacionados con formas a su cola específica
    public static final String ROUTING_KEY_Shape = "shape_routing_key";

    /**
     * Crea un Exchange de tipo Topic.
     * Un Topic Exchange permite enrutar mensajes a diferentes colas basándose en
     * patrones de routing keys. Los consumidores se suscriben con patrones que
     * coincidan con las routing keys de los mensajes.
     * 
     * @return TopicExchange configurado con el nombre definido
     */
    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(EXCHANGE_NAME);
    }


    /**
     * Configura el convertidor de mensajes para serialización JSON.
     * Utiliza Jackson para convertir automáticamente objetos Java a formato JSON
     * antes de enviarlos a RabbitMQ, y viceversa al recibirlos.
     * 
     * @return MessageConverter que usa Jackson para serialización/deserialización JSON
     */
    @Bean
    public MessageConverter jsonMessageConverter(){
       return new JacksonJsonMessageConverter();
    }

    /**
     * Crea y configura el RabbitTemplate, que es el cliente principal para interactuar con RabbitMQ.
     * Este template se usa para enviar mensajes al broker. Se inyecta automáticamente el
     * ConnectionFactory que Spring Boot configura desde application.properties.
     * 
     * @param connectionFactory Factory de conexiones proporcionado por Spring Boot
     * @return AmqpTemplate configurado con conversión JSON para envío de mensajes
     */
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory){
        final var template = new RabbitTemplate(connectionFactory);
        // Asigna el convertidor JSON para serializar automáticamente los objetos
        template.setMessageConverter(jsonMessageConverter());
       return template;
    }
}
