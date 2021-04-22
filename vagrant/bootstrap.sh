#!/usr/bin/env bash

#
# Basic development RPMs
#
sudo apt-get -y update
sudo apt-get -y install build-essential emacs zip
sudo apt-get -y install git apt-transport-https
sudo apt-get -y install curl tcpdump

#
# NFS Server
#
if [[ ! -s /etc/exports ]]; then
	echo "Setting up NFS server"
 	sudo apt-get -y install nfs-kernel-server
 	vagrantuser=`id -u vagrant`
  	vagrantgroup=`id -g vagrant`
  	sudo echo "/home/vagrant *(rw,sync,all_squash,no_subtree_check,anonuid=$vagrantuser,anongid=$vagrantgroup)" >> /etc/exports
  	systemctl restart nfs-kernel-server 
  	echo "NFS server setup complete"
fi

# 
# Maven in support of Drop wizard (w/ Java11 runtime)
#
sudo apt-get -y install maven

#
# Mongodb
#
wget -qO - https://www.mongodb.org/static/pgp/server-4.2.asc | sudo apt-key add -
echo "deb http://repo.mongodb.org/apt/debian buster/mongodb-org/4.2 main" | sudo tee /etc/apt/sources.list.d/mongodb-org-4.2.list
sudo apt-get -y update
sudo apt-get install -y mongodb-org
sudo systemctl enable mongod
sudo systemctl start mongod

#
# Manual steps to complete setup and finish
#    
echo ""
echo "======================================================================="
echo "Things you must do to complete setup:"
echo " Install node:"
echo "   wget -qO- https://raw.githubusercontent.com/creationix/nvm/v0.33.11/install.sh | bash"
echo "   nvm install 10.19.0"
echo "   curl -L https://www.npmjs.com/install.sh | sh"
echo ""
echo "======================================================================="
echo ""
