package zdavep.forklift.consumers.hello;

import forklift.connectors.ForkliftMessage;
import forklift.decorators.Message;
import forklift.decorators.MultiThreaded;
import forklift.decorators.OnMessage;
import forklift.decorators.Queue;

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

    // We simply log messages in this example.
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
        log.info("Got text message: " + textMessage.getText());
    }

    /**
     * Raw byte array message handling
     */
    private void handleBytesMessage(final BytesMessage bytesMessage) throws Exception {
        final long len = bytesMessage.getBodyLength();
        if (len > 0 && len < Long.valueOf(Integer.MAX_VALUE)) {
            final byte[] bytes = new byte[(int)len];
            bytesMessage.readBytes(bytes, (int)len);
            log.info("Got bytes message: " + new String(bytes));
        } else {
            log.error(len == 0
                ? "Error: message byte[] is empty"
                : "Error: message byte[] is too large to process");
        }
    }

}
