#Comandos para iniciar:
#   -Construir imagen: $ docker build -t datagrid:1.0
#   -Construir contenedores: $ docker run -d -p 4848:4848 -p 8080:8080 --name payara-node-1 --net red_payara datagrid:1.0
#                            $ docker run -d -p 4849:4848 -p 8081:8080 --name payara-node-2 --net red_payara datagrid:1.0
#                            $ docker run -d -p 4850:4848 -p 8082:8080 --name payara-node-3 --net red_payara datagrid:1.0
#Obtener imagen de debian
FROM debian:stretch

#Instalar JDK y componentes necesarios
RUN apt-get update && apt-get install -y openjdk-8-jre openjdk-8-jdk unzip wget openssh-client

RUN mkdir ~/certificadosRadius

RUN wget -P ~/certificadosRadius -O ca.pem https://raw.githubusercontent.com/Ludhe/pdc135asignacion4/master/resources/ca.pem 
RUN wget -P ~/certificadosRadius -O cakey.pem https://raw.githubusercontent.com/Ludhe/pdc135asignacion4/master/resources/cakey.pem


#Descargar Payara
RUN wget -O payara https://search.maven.org/remotecontent?filepath=fish/payara/distributions/payara/5.201/payara-5.201.zip

#Descomprimir Payara
RUN unzip payara

#Variables de entorno
ENV PASSWORD_FILE="password" \
PAYARA_DIR="payara5" \
ADMIN_USER="admin" \
ADMIN_PASSWORD="admin"

#Exponer puertos necesarios
EXPOSE 8080
EXPOSE 4848

#Descargar el war de la aplicación
RUN wget https://raw.githubusercontent.com/Ludhe/pdc135asignacion4/master/resources/login.war

#Crear usuario y contraseña para consola de administración
RUN printf "%b""AS_ADMIN_PASSWORD=$ADMIN_PASSWORD" >> $PASSWORD_FILE
RUN printf "%b""AS_ADMIN_PASSWORD=\n AS_ADMIN_NEWPASSWORD=$ADMIN_PASSWORD" >> tmpfile

#Iniciar el dominio, cambiar contraseña de administración, enable-secure-admin, configuración data grid y deploy de aplicación
RUN ${PAYARA_DIR}/bin/asadmin start-domain --interactive=false && \
$PAYARA_DIR/bin/asadmin --user $ADMIN_USER --passwordfile=tmpfile --interactive=false change-admin-password && \
$PAYARA_DIR/bin/asadmin --user $ADMIN_USER --passwordfile=${PASSWORD_FILE} --interactive=false enable-secure-admin  && \
$PAYARA_DIR/bin/asadmin --user $ADMIN_USER --passwordfile=${PASSWORD_FILE} set-hazelcast-configuration --clusterMode=multicast --multicastGroup=224.2.2.3 --multicastPort=54327 --dynamic=true  && \
$PAYARA_DIR/bin/asadmin --user $ADMIN_USER --passwordfile=${PASSWORD_FILE} --interactive=false deploy --availabilityenabled=true /login.war && \
${PAYARA_DIR}/bin/asadmin stop-domain

#Iniciar Payara
ENTRYPOINT $PAYARA_DIR/bin/asadmin --interactive=false start-domain --verbose
