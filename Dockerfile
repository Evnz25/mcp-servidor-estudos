FROM maven:3.10.0-eclipse-temurin-25 AS compilacao
WORKDIR /projeto
COPY pom.xml .
COPY src/main src/main
RUN mvn -B -Pserver -DskipTests package

FROM node:22-bookworm-slim
COPY --from=eclipse-temurin:25-jre-jammy /opt/java/openjdk /opt/java/openjdk
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH="${JAVA_HOME}/bin:${PATH}"
ENV NODE_ENV=production \
    HOME=/home/node \
    HOST=0.0.0.0 \
    DANGEROUSLY_BIND_ALL_INTERFACES=true \
    CLIENT_PORT=6274 \
    MCP_AUTO_OPEN_ENABLED=false \
    MCP_INSPECTOR_SECRET_STORE=memory

WORKDIR /home/node
RUN npm install --global @modelcontextprotocol/inspector@2.15.0
COPY --from=compilacao /projeto/server/ /opt/mcp/server/
USER node
EXPOSE 6274
ENTRYPOINT ["mcp-inspector"]
CMD ["--web"]
