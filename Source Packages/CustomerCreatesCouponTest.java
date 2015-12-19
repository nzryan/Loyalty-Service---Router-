/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package router;

import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.apache.http.auth.UsernamePasswordCredentials;

import org.apache.http.impl.auth.BasicScheme;

/**
 *
 * @author Ryan Campion 2343075
 */
public class CustomerCreatesCouponTest {
    
    public static void main(String[] args) throws Exception {
        
         /* Basic camel setup */  
        // create default context  
        CamelContext camel = new DefaultCamelContext();  
  
        // register ActiveMQ as the JMS handler  
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");  
        camel.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));  
  
        /* End basic setup */
        
        
        
        
        
        
        
        
        /* Create the routes */  
        camel.addRoutes(new RouteBuilder() {  
  
        @Override  
        public void configure() { 
            
          from("websocket://localhost:9091/email") 
          .log("received message: ${body}")
          .setProperty("email").body()
          .to("jms:queue:email-from-AJAX");
          
          from("websocket://localhost:9091/product") 
          .log("received message: ${body}")
          .to("jms:queue:product-from-AJAX");
                    
            
        }
        
        });
        
        
        
        
        
        
        
        
        
        /* Start the router */  
  
        // turn exchange tracing on or off (false is off)  
        camel.setTracing(false);  
  
        // start routing  
        System.out.println("Starting router...");  
  
        camel.start();  
  
        System.out.println("... ready."); 
        
        
        
        
    }
    
}
