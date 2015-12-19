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
public class CustomerCreatesCoupon {
    
    public static void main(String[] args) throws Exception {
        
         /* Basic camel setup */  
        // create default context  
        CamelContext camel = new DefaultCamelContext();  
  
        // register ActiveMQ as the JMS handler  
        ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");  
        camel.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));  
  
        /* End basic setup */
        
        
        
        
        String vendUsername = "camry134";  
        String vendPassword = "keenwhale39";  
  
        UsernamePasswordCredentials vendCreds = new UsernamePasswordCredentials(vendUsername, vendPassword); 


        // create the Base 64 encoded Basic Access header  
        final String basicAuthHeader  = BasicScheme.authenticate(vendCreds, "US-ASCII", false).getValue();  
        
        
        
        
        
        /* Create the routes */  
        camel.addRoutes(new RouteBuilder() {  
  
        @Override  
        public void configure() { 
            
            
            
            
            
            
            
            
            from("websocket://localhost:9091/email")
                    
                //save the email in a property do dont loose when removeing headers
                .setProperty("email").body() //email header not being set!
                .removeHeaders("*")
                    
                /**Vend necessities*/
                .setHeader("Authorization", constant(basicAuthHeader))
                .setBody(constant(null))
                .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                .to("http4://info-nts-12.otago.ac.nz:8086/vend-oauth/restricted/token")
                .convertBodyTo(String.class)
                .setHeader("Authorization").simple("Bearer ${body.trim()}")
//                
//            .multicast()
//            .to("jms:queue:AJAX-email-ready-for-vend","jms:queue:AJAX-email-ready-for-vend-test");
//            
//            
//            
//            
//            
//            
//            
//            
//            from("jms:queue:AJAX-email-ready-for-vend")
//                    
                    .removeHeaders("*", "Authorization")
                    .marshal().json(JsonLibrary.Gson)
                    .setHeader(Exchange.CONTENT_TYPE).constant("application/json")
                    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                    .recipientList().simple("https://info323.vendhq.com/api/customers?email=${exchangeProperty.email}")
             
            .multicast()
            .to("jms:queue:returned-customer-from-vend", "jms:queue:returned-customer-from-vend-test");
                    
            
            
            
            
            
            
            
            from("jms:queue:returned-customer-from-vend")
                    
                        .convertBodyTo(String.class)
                    
            .to("websocket://localhost:9091/email?sendToAll=true");
            
            
            
            
            
            
            
            
            from("websocket://localhost:9091/product") 
                    .log("${body}")
                    .setProperty("coupon").simple("${body}")
                    .setBody(constant(null))
                    .setHeader("Authorization", constant(basicAuthHeader))
                    .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                    .to("http4://info-nts-12.otago.ac.nz:8086/vend-oauth/restricted/token")
                    .convertBodyTo(String.class) 
                    .setHeader("Authorization").simple("Bearer ${body.trim()}")
                    .setBody(constant(null))
                    .setBody().header("coupon")
                    .removeHeaders("*", "Authorization")
                    .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                    .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                    .to("https://info323.vendhq.com/api/products")
                    
            .multicast()
            .to("jms:queue:product-back-from-vend", "jms:queue:product-back-from-vend-test");
            
            
            
            
            
            
            
            
            from("jms:queue:product-back-from-vend")
                    
                    .convertBodyTo(String.class)
                    
            .to("websocket://localhost:9091/product?sendToAll=true");
                     
            
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
