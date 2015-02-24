# Forklift server Dockerfile
# 2015-02-24

# Pull base image
FROM zdavep/forklift-server:latest

# Main developer
MAINTAINER Dave Pederson <dave.pederson@gmail.com>

# Add hello forklift JAR
ADD ./target/hello-forklift-0.1.jar $FORKLIFT_CONSUMER_HOME/hello-forklift-0.1.jar

# Add boot script
ADD boot.sh /opt/boot.sh
RUN chmod +x /opt/boot.sh

# Boot forklift on startup
CMD /opt/boot.sh