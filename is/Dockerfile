FROM node:10.19.0
COPY node_modules /home/node_modules
COPY dist /home/dist
COPY static /home/static
COPY config-production.yml /home/config.yml
RUN date > /home/timestamp.txt
EXPOSE 8004
WORKDIR /home
CMD ["node", "dist/server.js"]
