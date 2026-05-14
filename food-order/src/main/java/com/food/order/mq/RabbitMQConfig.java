package com.food.order.mq;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_COUPON_CLAIM = "coupon.claim.queue";
    public static final String EXCHANGE_COUPON = "coupon.exchange";
    public static final String ROUTING_KEY_CLAIM = "coupon.claim";

    @Bean
    public Queue couponClaimQueue() {
        return QueueBuilder.durable(QUEUE_COUPON_CLAIM)
                .withArgument("x-dead-letter-exchange", "coupon.dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "coupon.dlx.claim")
                .build();
    }

    @Bean
    public DirectExchange couponExchange() {
        return new DirectExchange(EXCHANGE_COUPON);
    }

    @Bean
    public Binding claimBinding(Queue couponClaimQueue, DirectExchange couponExchange) {
        return BindingBuilder.bind(couponClaimQueue).to(couponExchange).with(ROUTING_KEY_CLAIM);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("coupon.dlx.exchange");
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable("coupon.dlx.queue").build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("coupon.dlx.claim");
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
