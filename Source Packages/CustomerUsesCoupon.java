/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package router;

import java.util.Map;
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
public class CustomerUsesCoupon {
    
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
            
            //from the multicast of incoming email from vend from router 1 and make into a map
            from("jms:queue:use-coupon-email-input")
                        .setHeader("customer").jsonpath("$.customer_id")
                        .unmarshal().json(JsonLibrary.Gson, Map.class)  
            .to("jms:queue:products");
            
            
            
            
            
            
            // split map into single products
                from("jms:queue:products")
                        .split()
                        .simple("${body[register_sale_products]}")
            .to("jms:queue:split-products");
                
                
                
                
                
                
                
                
                // marshals to json, takes the prod id from the json and makes it a header  
                from("jms:queue:split-products")
                        .marshal().json(JsonLibrary.Gson) 
                        .setHeader("productId").jsonpath("$.product_id")
                        .unmarshal().json(JsonLibrary.Gson, Map.class)
                .to("jms:queue:products-to-get-vend-auth");
                
            
                
                
                
                
                
                //request the product from vend
                 from("jms:queue:products-to-get-vend-auth")
                        .setProperty("productId").header("productId")
                        .setProperty("orignalBody").body()//so it doesnt get lost
                        .removeHeaders("*", "customerId", "productId")                     
                        .setHeader("Authorization", constant(basicAuthHeader))
                        .setBody(constant(null))//Get method doesnt use body
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        .to("http4://info-nts-12.otago.ac.nz:8086/vend-oauth/restricted/token")
                        .convertBodyTo(String.class) 
                        .setHeader("Authorization").simple("Bearer ${body.trim()}") 
                        .setBody(constant(null))
                        .setBody().header("originalBody")
                        .removeProperty("originalBody")
                        .removeHeaders("*", "Authorization", "productId", "customerId")
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .recipientList().simple("http://localhost:8081/customers/products/${exchangeProperty.productid}")
                .to("jms:queue:product-jason-from-vend");
                
                 
                 
                 
                 
                 
                 
                 
                 //once received from Vend. If the product was a coupon carry on to following routers, otherwise dump in a queue bin
                 from("jms:queue:product-jason-from-vend")
                        .unmarshal().json(JsonLibrary.Gson, Map.class)
                        .split().simple("${body[products]}")
                        .choice()
                        .when().simple("${body[handle]} == 'Coupon'")
                        .to("jms:queue:was-a-coupon")
                        .otherwise()
                        .to("jms:queue:was-not-a-coupon-do-nothing");
                 
                 
                 
                 
                 
                 
                 
                 
                 
                 
                 from("jms:queue:was-a-coupon")
                         
                         //get the customer from vend we can extract the firstname for the Social Integration
                        .marshal().json(JsonLibrary.Gson)
                        .setProperty("productid").header("productId")
                        .setProperty("customerid").header("customerId")                        
                        .setProperty("couponid").jsonpath("$.source_id")
                        .setProperty("productPrice").jsonpath("$.price")
                        .setProperty("orignalbody").body()
                        .unmarshal().json(JsonLibrary.Gson, Map.class)
                        .removeHeaders("*", "productId", "customerId", "couponId", "productPrice")
                        .setHeader("Authorization", constant(basicAuthHeader))
                        .setBody(constant(null))
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        .to("http4://info-nts-12.otago.ac.nz:8086/vend-oauth/restricted/token")
                        .convertBodyTo(String.class)
                        .setHeader("Authorization").simple("Bearer ${body.trim()}")
                        .setBody(constant(null))
                        .setBody().header("originalbody")
                        .removeProperty("originalbody").removeHeaders("*", "Authorization", "productId", "customerId", "couponId", "productPrice")
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .recipientList().simple("http4://localhost:9080/api/customers?id={exchangeProperty.customerid}")
                         
                         //Post the advertisment on the Social Service
                        .removeHeaders("*")
                        .unmarshal().json(JsonLibrary.Gson, Map.class)
                        .split().simple("${body[customers]}")  
                        .marshal().json(JsonLibrary.Gson)
                        .setProperty("first").jsonpath("$.first_name")
                        .setBody().simple("${exchangeProperty.first} just saved ${exchangeProperty.productPrice}")
                        .convertBodyTo(String.class)
                        .to("rmi://localhost:1099/social?remoteInterfaces=social.ISocialIntegrationService&method=postNotification")
                         
                         //update the coupon resource to used
                        .setProperty("couponId").header("couponId")
                        .removeHeaders("*", "Authorization", "productId", "customerId", "couponId", "productPrice" ) 
                        .setBody(constant(null))
                        .setHeader(Exchange.HTTP_METHOD, constant("PUT"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .recipientList().simple("http4://localhost:8081/customers/${exchangeProperty.customerid}/coupons/${exchangeProperty.couponid}")
                         
                         //Delete the poduct on Vend
                        .setProperty("couponid").header("id")
                        .unmarshal().json(JsonLibrary.Gson, Map.class)
                        .removeHeaders("*", "Authorization", "productId", "customerId", "couponId", "productPrice")
                        .setHeader("Authorization", constant(basicAuthHeader))
                        .setBody(constant(null))
                        .setHeader(Exchange.HTTP_METHOD, constant("GET"))
                        .to("http4://info-nts-12.otago.ac.nz:8086/vend-oauth/restricted/token")
                        .convertBodyTo(String.class)
                        .setHeader("Authorization").simple("Bearer ${body.trim()}") // trim is necessary!
                        .setBody(constant(null))
                        .setHeader(Exchange.HTTP_METHOD, constant("DELETE"))
                        .setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
                        .recipientList().simple("http4://localhost:8081/api/products/${exchangeProperty.couponId}");
                         
                
                
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
