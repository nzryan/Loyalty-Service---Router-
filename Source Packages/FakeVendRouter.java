
import javax.jms.ConnectionFactory;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;

public class FakeVendRouter {

	public static void main(String[] args) throws Exception {

		/*
		 * Basic camel setup
		 */

		// create default context  
		CamelContext camel = new DefaultCamelContext();

		// register ActiveMQ as the JMS handler  
		ConnectionFactory connectionFactory = new ActiveMQConnectionFactory("tcp://localhost:61616");
		camel.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));

		/*
		 * End basic setup
		 */

		/*
		 * Create the routes
		 */
		camel.addRoutes(new RouteBuilder() {

			@Override
			public void configure() {

				from("jetty:http://localhost:9080/api/customers?httpMethodRestrict=GET")
						.log("HTTP Method: ${headers.CamelHttpMethod}")
						.log("Query parameters: ${headers.CamelHttpQuery}")
						.log("Authorization header: ${headers.Authorization}")
						.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
						.setBody(constant("{ \"customers\" : [ { \"id\": \"bc305bf5-da76-11e4-f3a2-b723349226ba\", \"name\": \"Jimbob Jenkins\", \"customer_code\": \"Jimbob-2567\", \"customer_group_id\": \"bc305bf5-da20-11e4-f3a2-b575f2b60965\", \"customer_group_name\": \"All Customers\", \"first_name\": \"Jimbob\", \"last_name\": \"Jenkins\", \"company_name\": \"\", \"phone\": \"021555555\", \"mobile\": \"\", \"fax\": \"\", \"email\": \"jim@somewhere.com\", \"twitter\": \"\", \"website\" : \"\", \"physical_address1\": \"\", \"physical_address2\": \"\", \"physical_suburb\": \"\", \"physical_city\": \"\", \"physical_postcode\": \"\", \"physical_state\": \"\", \"physical_country_id\": \"NZ\", \"postal_address1\": \"\", \"postal_address2\": \"\", \"postal_suburb\": \"\", \"postal_city\": \"\", \"postal_postcode\": \"\", \"postal_state\": \"\", \"postal_country_id\" : \"NZ\", \"updated_at\": \"2015-05-01 13:38:18\", \"deleted_at\": \"\", \"balance\": \"-17.950\", \"year_to_date\": \"143.60001\", \"date_of_birth\": \"\", \"sex\": \"M\", \"custom_field_1\": \"\", \"custom_field_2\": \"\", \"custom_field_3\": \"\", \"custom_field_4\": \"\", \"note\": \"\", \"contact\": { \"company_name\": \"\", \"phone\": \"021555555\", \"email\": \"jim@somewhere.com\" } } ] }"));

				from("jetty:http://localhost:9080/api/products?httpMethodRestrict=POST")
						.convertBodyTo(String.class)
						.setProperty("incoming").body()
						.log("HTTP Method: ${headers.CamelHttpMethod}")
						.log("Authorization header: ${headers.Authorization}")
						.log("Body: ${body}")
						.setBody().simple("{ \"id\": \"bc305bf5-da20-11e4-f3a2-b575f2cb0159\",${exchangeProperty.incoming.substring(1)}");

				from("jetty:http://localhost:9080/api/products/?matchOnUriPrefix=true&httpMethodRestrict=GET,DELETE")
						.log("HTTP Method: ${headers.CamelHttpMethod}")
						.log("Authorization header: ${headers.Authorization}")
						.log("${body}")
						.setHeader(Exchange.CONTENT_TYPE, constant("application/json"))
						.setBody(constant("{ \"products\": [ { \"id\": \"bc305bf5-da20-11e4-f3a2-b575f2cb0159\", \"source_id\": \"\", \"variant_source_id\": \"\", \"handle\": \"coffee\", \"type\": \"\", \"has_variants\": false, \"variant_parent_id\": \"\", \"variant_option_one_name\": \"\", \"variant_option_one_value\": \"\", \"variant_option_two_name\": \"\", \"variant_option_two_value\": \"\", \"variant_option_three_name\": \"\", \"variant_option_three_value\": \"\", \"active\": true, \"name\": \"Coffee (Demo)\", \"base_name\": \"Coffee (Demo)\", \"description\": \"<p>A cup of Coffee. You can delete this once you have some other products setup.<\\/p>\", \"image\": \"https:\\/\\/info323.vendhq.com\\/images\\/placeholder\\/product\\/no-image-white-thumb.png\", \"image_large\": \"https:\\/\\/info323.vendhq.com\\/images\\/placeholder\\/product\\/no-image-white-original.png\", \"sku\": \"coffee-hot\", \"tags\": \"General\", \"brand_id\": \"bc305bf5-da20-11e4-f3a2-b575f2d3d435\", \"brand_name\": \"Generic Brand\", \"supplier_name\": \"INFO323\", \"supplier_code\": \"\", \"supply_price\": \"0.00\", \"account_code_purchase\": \"\", \"account_code_sales\": \"\", \"price\": 2.6087, \"tax\": 0.39131, \"tax_id\": \"bc305bf5-da20-11e4-f3a2-b575f2bd6715\", \"tax_rate\": 0.15, \"tax_name\": \"GST\", \"display_retail_price_tax_inclusive\": 1, \"updated_at\": \"2015-04-27 00:46:12\", \"deleted_at\": \"\" } ] }"));
			}
		});

		/*
		 * Start the router
		 */

		// turn exchange tracing on or off (false is off)  
		camel.setTracing(false);

		// start routing  Vend router...");

		camel.start();

		System.out.println("... ready.");


	}

}
