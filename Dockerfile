FROM jboss/keycloak-adapter-wildfly
RUN /opt/wildfly/bin/add-user.sh admin hunter2 --silent

#USER root
#ADD . /build
#RUN curl https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein > bin/lein && \
#    chmod +x /bin/lein && \
#    chown -R wildfly /build && \
#		yum install -y zip
#USER wildfly
#ENV HOME /build
#WORKDIR /build
#RUN lein immutant war /opt/wildfly/standalone/deployments/ && \
#    mv /opt/wildfly/standalone/deployments/learning-dev.war /opt/wildfly/standalone/deployments/ROOT.war && \
#    zip /opt/wildfly/standalone/deployments/ROOT.war WEB-INF/*


ADD target/base+system+user+dev/consumer.war /opt/wildfly/standalone/deployments/ROOT.war
