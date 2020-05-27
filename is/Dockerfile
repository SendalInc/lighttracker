FROM node:10.19.0
COPY . /home
COPY config-production.yml /home/config.yml
RUN date > /home/timestamp.txt
EXPOSE 8004
WORKDIR /home
CMD ["node", "dist/server.js"]
