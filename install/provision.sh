#!/bin/bash

set -e -x

add-apt-repository ppa:webupd8team/java -y
apt-get update

apt-get install -y curl git gcc make python-dev vim-nox jq cgroup-lite silversearcher-ag

echo "LC_ALL=en_US.UTF-8" >> /etc/environment
echo "LC_CTYPE=en_US.UTF-8" >> /etc/environment
echo "LANG=en_US.UTF-8" >> /etc/environment
echo "LANGUAGE=en_US.UTF-8" >> /etc/environment
source /etc/environment

#mysql
echo "mysql-server mysql-server/root_password password password" | sudo debconf-set-selections
echo "mysql-server mysql-server/root_password_again password password" | sudo debconf-set-selections
apt-get install -y mysql-server

tee -a /etc/mysql/my.cnf <<EOF

[client]
default-character-set = utf8mb4

[mysql]
default-character-set = utf8mb4

[mysqld]
character-set-client-handshake = FALSE
character-set-server = utf8mb4
collation-server = utf8mb4_unicode_ci
EOF

service mysql stop
service mysql start

#java
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 seen true" | debconf-set-selections
apt-get install oracle-java8-installer -y

#maven
apt-get install -y maven

#noi project
git clone https://github.com/pppepito86/sandbox.git /vagrant/sandbox
git clone https://github.com/pppepito86/grader.git /vagrant/grader
git clone https://github.com/MarinShalamanov/worker-manager.git /vagrant/worker-manager
git clone https://github.com/pppepito86/noi.git /vagrant/noi
mvn install -f /vagrant/sandbox/pom.xml
mvn install -f /vagrant/grader/pom.xml
mvn install -f /vagrant/worker-manager/pom.xml
mvn install -f /vagrant/noi/pom.xml

#c++ documentation
mkdir -p /vagrant/noi/src/main/resources/static/docs
wget https://github.com/PeterFeicht/cppreference-doc/releases/download/v20180923/html-book-20180923.zip -O /vagrant/noi/src/main/resources/static/docs/html_book_20180311.zip
unzip /vagrant/noi/src/main/resources/static/docs/html_book_20180311.zip -d /vagrant/noi/src/main/resources/static/docs/
mv /vagrant/noi/src/main/resources/static/docs/reference/* /vagrant/noi/src/main/resources/static/docs

mvn spring-boot:run -f /vagrant/noi/pom.xml >/vagrant/noi/stdout 2> /vagrant/noi/stderr &
