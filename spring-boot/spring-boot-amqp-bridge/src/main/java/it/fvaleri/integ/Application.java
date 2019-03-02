package it.fvaleri.integ;

import javax.jms.ConnectionFactory;

import org.apache.camel.component.amqp.AMQPComponent;
import org.apache.qpid.jms.JmsConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.connection.JmsTransactionManager;
import org.messaginghub.pooled.jms.JmsPoolConnectionFactory;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Value("${jmscomp.consumers}")
    Integer concurrentConsumers;

    @Value("${jmscomp.cache.level}")
    String cacheLevelName;

    @Bean
    public ConnectionFactory connectionFactory(@Value("${amqp.url}") String url,
            @Value("${amqp.username}") String username, @Value("${amqp.password}") String password) {
        return new JmsConnectionFactory(username, password, url);
    }

    @Bean
    public ConnectionFactory pooledConnectionFactory(@Value("${amqp.url}") String url,
            @Value("${amqp.username}") String username, @Value("${amqp.password}") String password) {
        // not using Spring CachingConnectionFactory, it's only useful to cache producers. If you have concCons > maxConcCons
        // you can end up with cached consumers which get messages where there is no listener (stuck messages)
        JmsPoolConnectionFactory jmsPoolConnectionFactory = new JmsPoolConnectionFactory();
        jmsPoolConnectionFactory.setConnectionFactory(new JmsConnectionFactory(username, password, url));
        // we only need one connection and we want to keep it opened
        jmsPoolConnectionFactory.setMaxConnections(1);
        jmsPoolConnectionFactory.setConnectionIdleTimeout(0);
        return jmsPoolConnectionFactory;
    }

    @Bean
    public JmsTransactionManager transactionManager(@Autowired ConnectionFactory pooledConnectionFactory) {
        JmsTransactionManager jmsTransactionManager = new JmsTransactionManager();
        jmsTransactionManager.setConnectionFactory(pooledConnectionFactory);
        return jmsTransactionManager;
    }

    @Bean(name = "amqp")
    public AMQPComponent getAMQPComponent(@Autowired ConnectionFactory pooledConnectionFactory,
            @Autowired JmsTransactionManager transactionManager) {
        AMQPComponent amqpComponent = new AMQPComponent(pooledConnectionFactory);
        // we need to enable transactions to avoid losing messages
        amqpComponent.setTransacted(true);
        amqpComponent.setTransactionManager(transactionManager);
        amqpComponent.setConcurrentConsumers(concurrentConsumers);
        amqpComponent.setMaxConcurrentConsumers(concurrentConsumers);
        amqpComponent.setCacheLevelName(cacheLevelName);
        return amqpComponent;
    }
}

