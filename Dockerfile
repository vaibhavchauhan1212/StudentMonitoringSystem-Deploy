FROM tomcat:11.0

RUN rm -rf /usr/local/tomcat/webapps/*

COPY StudentMonitoringSystem_Deploy.war /usr/local/tomcat/webapps/ROOT.war

EXPOSE 8080

CMD ["catalina.sh", "run"]