package zdavep.forklift.consumers.hello;

import forklift.connectors.ForkliftMessage;
import forklift.decorators.Message;
import forklift.decorators.MultiThreaded;
import forklift.decorators.OnMessage;
import forklift.decorators.Queue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jms.TextMessage;
import javax.jms.BytesMessage;

/**
 * A simple "Hello, World" forklift consumer.
 */
@Queue("test")
@MultiThreaded(4)
public class HelloForkliftConsumer {

    private static final Logger log = LoggerFactory.getLogger(HelloForkliftConsumer.class);

    // The message received and set by forklift.
    @Message ForkliftMessage message;

    /**
     * Default constructor
     */
    public HelloForkliftConsumer() {
    }

    /**
     * Message handler function: called after the above message is set.
     */
    @OnMessage public void onMessage() {
        if (message == null) {
            log.warn("Message is null in onMessage!");
            return;
        }
        try {
            final javax.jms.Message jmsMsg = message.getJmsMsg();
            if (jmsMsg instanceof TextMessage) {
                handleTextMessage((TextMessage) jmsMsg);
            } else if (jmsMsg instanceof BytesMessage) {
                handleBytesMessage((BytesMessage) jmsMsg);
            } else {
                log.error("Error: unsupported message type");
            }
        } catch (Throwable t) {
            log.error("Error in onMessage: ", t);
        }
    }

    /**
     * Text message handling
     */
    private void handleTextMessage(final TextMessage textMessage) throws Exception {
        insertMessage("Got text message: " + textMessage.getText());
    }

    /**
     * Raw byte array message handling
     */
    private void handleBytesMessage(final BytesMessage bytesMessage) throws Exception {
        final long len = bytesMessage.getBodyLength();
        if (len > 0 && len < Long.valueOf(Integer.MAX_VALUE)) {
            final byte[] bytes = new byte[(int)len];
            bytesMessage.readBytes(bytes, (int)len);
            insertMessage("Got bytes message: " + new String(bytes));
        } else {
            log.error(len == 0
                ? "Error: message byte[] is empty"
                : "Error: message byte[] is too large to process");
        }
    }

    /**
     * Insert message into h2 db
     */
    private void insertMessage(String message) throws SQLException {
        Connection conn = getJDBConnnect();
        Statement stmt = null;
        try {
            log.debug("inserting " + message + " into db");
            stmt = conn.createStatement();
            stmt.execute("INSERT INTO MESSAGE(content) VALUES('" + message + "')");
        } catch (SQLException ex) {
            log.error("Error in insertMessage : ", ex);
        } finally {
            conn.close();
        }
    }

    /**
     * return connection to h2 memory db
     */
    private Connection getJDBConnnect() {
        Connection conn = null;

        try {
            Class.forName("org.h2.Driver");
            conn = DriverManager.getConnection("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1", "", "");
            return conn;
        } catch (SQLException ex) {
            log.error("Error in getJDBConnnect : ", ex);
        } catch (ClassNotFoundException nf) {
            log.error("Class not found : ", nf);
        }

        return conn;
    }

}
