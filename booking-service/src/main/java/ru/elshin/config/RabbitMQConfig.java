package ru.elshin.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "appointment.notifications.queue";
    public static final String EXCHANGE_NAME = "appointment.exchange";

    // Routing keys для отправки точечных событий
    public static final String ROUTING_KEY_CREATED = "appointment.created";
    public static final String ROUTING_KEY_STATUS_CHANGED = "appointment.status.changed";

    @Bean
    public Queue queue() {
        return new Queue(QUEUE_NAME, true); // durable = true (очередь выдержит перезапуск брокера)
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        // "appointment.#" перехватывает все routing keys: appointment.created, appointment.status.changed, etc.
        return BindingBuilder.bind(queue).to(exchange).with("appointment.#");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
