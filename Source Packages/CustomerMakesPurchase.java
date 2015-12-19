/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package router;

import Loyalty.domain.Transaction;
import SalesAggregation.domain.Sale;
import javax.jms.ConnectionFactory;
import javax.swing.JOptionPane;
import javax.swing.JPasswordField;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.model.dataformat.JsonLibrary;

/**
 *
 * @author Ryan Campion 2343075
 */
public class CustomerMakesPurchase {
    
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
            
            
            
         // Working - tested in Web Console   
         from("imaps://outlook.office365.com?username=camry134@student.otago.ac.nz"
        + "&password=" + getPassword("Enter your E-Mail password")
        + "&searchTerm.subject=Vend:SaleUpdate")
        .log("Found new E-Mail: ${body}")
        .multicast()
        .to("jms:queue:vend-email","jms:queue:vent-email-test","jms:queue:use-coupon-email-input");   
         
        
          // Working - tested in Web Console   
        from("jms:queue:vend-email")
        .setHeader("saleid").jsonpath("$.id")
        .setHeader("customerid").jsonpath("$.customer_id")
        .setHeader("totalprice").jsonpath("$.totals.total_price")
        .multicast()
        .to("jms:queue:new-sale", "jms:queue:new-sale-test", "jms:queue:new-transaction", "jms:queue:new-transaction-test"); 
        
        
        // Working - tested in Web Console 
        from("jms:queue:new-sale")
        .unmarshal().json(JsonLibrary.Gson, Sale.class)
                .multicast()
        .to("cxf:http://localhost:9001/salesAggregate?serviceClass=aggregationInterface.ISalesAggregation&defaultOperationName=newSale", "jms:queue:queue-to-rpc");
         
        
        
        
         // Working - tested in Web Console
        from("jms:queue:new-transaction")
        .setHeader("points").method(CustomerMakesPurchase.class, "pointsCalculator(${headers.totalprice})")
                .multicast()
        .to("jms:queue:calculated-points", "jms:queue:calculated-points-test");    
        
        
        
          // Working - tested in Web Console
        from("jms:queue:calculated-points")
        .bean(Transaction.class, "transactionMethodConstructor(${headers.saleid},${headers.points})")
                 .multicast()
        .to("jms:queue:send-transaction", "jms:queue:send-transaction-test");
        
        
        
        //not allowing a transaction to be made
        from("jms:queue:send-transaction")
        .setProperty("customerid").header("customerid")
        .removeHeaders("*") // remove headers to stop them being sent to the service
        .setHeader(Exchange.HTTP_METHOD, constant("POST"))
        .setHeader(Exchange.CONTENT_TYPE, constant("application/json")) 
        .marshal().json(JsonLibrary.Gson)
        .recipientList().simple("http4://localhost:8081/customers/${exchangeProperty.customerid}/transactions");
       
        
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
    
    
    
    
    
    // A method to promt the user for a password for accessing their email in the Route where
    //Email is used to be able to add an item to the shopping list
    public static String getPassword(String prompt) {
        JPasswordField txtPasswd = new JPasswordField();
        int resp = JOptionPane.showConfirmDialog(null, txtPasswd, prompt,
        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (resp == JOptionPane.OK_OPTION) {
                 String password = new String(txtPasswd.getPassword());
                    return password;
            }
            return null;
    }
    
    public Integer pointsCalculator(Double pointsInput){
        Integer points = pointsInput.intValue();
        points = points*5;
        return points;
   }
    
}
