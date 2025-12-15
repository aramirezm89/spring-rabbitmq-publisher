package com.aramirezm.rabbitpublisher;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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
     * CONFIGURACIÓN PARA PRODUCCIÓN:
     * - durable=true: El exchange sobrevive a reinicios del broker RabbitMQ
     * - autoDelete=false: El exchange no se elimina automáticamente cuando no hay bindings
     * Esto garantiza que la infraestructura de mensajería sea persistente
     * 
     * @return TopicExchange configurado con el nombre definido
     */
    @Bean
    public TopicExchange topicExchange(){
        return new TopicExchange(EXCHANGE_NAME, true, false);
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
     * CONFIGURACIONES PARA ALTO VOLUMEN Y RESILIENCIA:
     * 1. MessageConverter: Serializa objetos Java a JSON automáticamente
     * 2. Mandatory: Si true, el mensaje retorna al publisher si no puede ser enrutado a ninguna cola
     * 3. ConfirmCallback: Recibe confirmaciones del broker cuando el mensaje fue recibido
     * 4. ReturnsCallback: Recibe mensajes que no pudieron ser enrutados (cuando mandatory=true)
     * 
     * El ConfirmCallback permite implementar lógica de reintento o logging cuando un mensaje falla.
     * El ReturnsCallback alerta cuando un mensaje no llegó a ninguna cola (routing key incorrecto, etc.)
     * 
     * @param connectionFactory Factory de conexiones proporcionado por Spring Boot
     * @return AmqpTemplate configurado con conversión JSON para envío de mensajes
     */
    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory){
        final var template = new RabbitTemplate(connectionFactory);
        
        // Asigna el convertidor JSON para serializar automáticamente los objetos
        template.setMessageConverter(jsonMessageConverter());
        
        // MANDATORY: Si el mensaje no puede ser enrutado a ninguna cola, se devuelve al publisher
        // Esto es crítico para detectar problemas de configuración o routing keys incorrectos
        template.setMandatory(true);
        
        // CONFIRM CALLBACK: Se ejecuta cuando RabbitMQ confirma que recibió el mensaje
        // ack=true: mensaje recibido exitosamente
        // ack=false: mensaje rechazado (problema en el broker)
        // En producción, aquí implementarías lógica de reintento o persistencia de mensajes fallidos
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (ack) {
                log.debug("Mensaje confirmado por RabbitMQ: {}", correlationData);
            } else {
                log.error("Mensaje rechazado por RabbitMQ. CorrelationData: {}, Causa: {}", 
                         correlationData, cause);
                // TODO: Implementar lógica de reintento o almacenamiento en DLQ (Dead Letter Queue)
            }
        });
        
        // RETURNS CALLBACK: Se ejecuta cuando un mensaje no pudo ser enrutado a ninguna cola
        // Esto ocurre cuando el routing key no coincide con ningún binding o la cola no existe
        // Es fundamental para detectar errores de configuración
        template.setReturnsCallback(returned -> {
            log.error("Mensaje no pudo ser enrutado. Exchange: {}, RoutingKey: {}, ReplyCode: {}, ReplyText: {}",
                     returned.getExchange(),
                     returned.getRoutingKey(),
                     returned.getReplyCode(),
                     returned.getReplyText());
            // TODO: Implementar lógica de manejo de mensajes no enrutados
            // Opciones: reintentar con otro routing key, almacenar en base de datos, alertar, etc.
        });
        
       return template;
    }
}
