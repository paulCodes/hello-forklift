package zdavep.forklift.test;

import org.apache.activemq.broker.BrokerService;
import org.apache.commons.io.FileUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import forklift.Forklift;
import forklift.connectors.ActiveMQConnector;

/**
 * Contains all of the necessary connection management code that tests
 * need in order to run against the activemq broker, in memory h2 database and forklift. This 
 * manager assumes that only a single broker is needed for testing. 
 * @author mconroy, pparker
 *
 */
public class TestServiceManager {

	private static final Logger log = LoggerFactory.getLogger(TestServiceManager.class);

	private static final Object lock = new Object();
	private static final String brokerUrl = "tcp://localhost:61617";
	private static Integer count = 0;
	private static BrokerService activemq;
	private static ActiveMQConnector connector;
	private static Forklift forklift;
	
	public static void start() {
		synchronized (lock) {
			if (forklift != null && forklift.isRunning())
				return;

			Connection conn = getJDBConnnect();

			try {
				activemq = new BrokerService();
				activemq.addConnector(brokerUrl);
				activemq.start();
				
				// Verify that we can get an activemq connection to the broker.
	            connector = new ActiveMQConnector(brokerUrl);
	            connector.start();
	            
	            forklift = new Forklift();
	            forklift.start(connector);

	            conn.createStatement().execute("CREATE TABLE MESSAGE(content TEXT)");
	            conn.close();

	            count++;
			} catch (Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	public static void stop() {
		synchronized (lock) {
			// Don't shutdown the connector if there are still classes using it. 
			count--;
			if (count > 0)
				return;
			log.debug("Shutting down all the things");
			Connection conn = getJDBConnnect();

			// Kill forklift and broker. Cleanup the testing data.
			try {
				forklift.shutdown();
				activemq.stop();
				FileUtils.deleteDirectory(activemq.getDataDirectoryFile());
				conn.createStatement().execute("DROP TABLE MESSAGE");
				conn.close();
			} catch (Throwable e) {
				e.printStackTrace();
			} 
		}
	}
	
	public static ActiveMQConnector getConnector() {
		synchronized (lock) {
			return connector;
		}
	}
	
	public static Forklift getForklift() {
		synchronized (lock) {
			return forklift;
		}
	}

	public static Connection getJDBConnnect() {
        Connection conn = null;

        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "", "");
            return conn;
        } catch (SQLException ex) {
            log.error("Error in getJDBConnnect : ", ex);
        } catch (ClassNotFoundException nf) {
        	log.error("Class not found in getJDBConnnect : ", nf);
        }

        return conn;
    } 
}
