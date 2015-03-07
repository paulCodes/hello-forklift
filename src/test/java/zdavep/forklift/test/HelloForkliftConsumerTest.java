package zdavep.forklift.test;

import java.util.concurrent.atomic.AtomicInteger;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.apache.activemq.command.ActiveMQTextMessage;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import forklift.connectors.ConnectorException;
import forklift.connectors.ForkliftConnectorI;
import forklift.connectors.ForkliftMessage;
import forklift.consumer.Consumer;
import forklift.decorators.OnMessage;
import forklift.decorators.Queue;

import zdavep.forklift.consumers.hello.HelloForkliftConsumer;

@Queue("test")
public class HelloForkliftConsumerTest {

	private static final Logger log = LoggerFactory.getLogger(HelloForkliftConsumerTest.class);
	private static AtomicInteger called = new AtomicInteger(0);
	private static boolean ordered = true;
	
	@Before
	public void before() {
		TestServiceManager.start();
	}

	@After
	public void after() {
		TestServiceManager.stop();
	}
	

    @Test
    public void test() throws JMSException, ConnectorException, SQLException {
    	int msgCount = 100;
    	
    	final ForkliftConnectorI connector = TestServiceManager.getForklift().getConnector();
    	final MessageProducer producer = connector.getProducer("test");
        for (int i = 0; i < msgCount; i++) {
        	final Message m = new ActiveMQTextMessage();
        	m.setJMSCorrelationID("" + i);
        	producer.send(m);
        }
        producer.close();

    	final HelloForkliftConsumer consumer = new HelloForkliftConsumer();
        final Consumer c = new Consumer(consumer.getClass(), TestServiceManager.getConnector());
        
        // Shutdown the consumer after all the messages have been processed.
        c.setOutOfMessages((listener) -> {
            listener.shutdown();

            Connection conn = TestServiceManager.getJDBConnnect();
            
	        try {
	        	Statement stmt = conn.createStatement();
	        	ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS messageCount FROM MESSAGE");
	        	rs.next();
	        	called.set(rs.getInt("messageCount"));
	        	conn.close();
	        } catch (SQLException ex) {
	        	log.error("Error in setOutOfMessages ", ex);
	        	Assert.fail("Failed while testing db message count");
	        }

            Assert.assertTrue("called("+called.get()+" was not == " + msgCount, called.get() == msgCount);
        });

        // Start the consumer.
        c.listen();
        
        Assert.assertTrue(called.get() > 0);
    }
}