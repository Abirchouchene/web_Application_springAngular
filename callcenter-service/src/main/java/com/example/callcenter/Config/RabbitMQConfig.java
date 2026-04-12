package com.example.callcenter.Config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Exchange
    public static final String EXCHANGE = "callcenter.events";

    // Queues
    public static final String REPORT_APPROVED_QUEUE = "callcenter.report.approved";
    public static final String REPORT_GENERATED_QUEUE = "callcenter.report.generated";
    public static final String NOTIFICATION_QUEUE = "callcenter.notification";

    // Routing keys
    public static final String REPORT_APPROVED_KEY = "report.approved";
    public static final String REPORT_GENERATED_KEY = "report.generated";
    public static final String NOTIFICATION_KEY = "notification.send";

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Queue reportApprovedQueue() {
        return QueueBuilder.durable(REPORT_APPROVED_QUEUE).build();
    }

    @Bean
    public Queue reportGeneratedQueue() {
        return QueueBuilder.durable(REPORT_GENERATED_QUEUE).build();
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE).build();
    }

    @Bean
    public Binding reportApprovedBinding(Queue reportApprovedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(reportApprovedQueue).to(exchange).with(REPORT_APPROVED_KEY);
    }

    @Bean
    public Binding reportGeneratedBinding(Queue reportGeneratedQueue, TopicExchange exchange) {
        return BindingBuilder.bind(reportGeneratedQueue).to(exchange).with(REPORT_GENERATED_KEY);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange exchange) {
        return BindingBuilder.bind(notificationQueue).to(exchange).with(NOTIFICATION_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
}
