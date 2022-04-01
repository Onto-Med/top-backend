# TOP Backend

Spring Boot based backend of the TOP framework

## Running the Spring Server

1. Set up environment variables:
* `APP_PORT`: the port where the spring application will run on, e.g. 8080
* `APP_PATH`: the context path, e.g. "/" for root
* `DB_HOST`: host running the neo4j database server
* `DB_PORT`: port of the database host
* `DB_USER`: username for connecting to the database
* `DB_PASS`: password for connecting to the database

2. Start the Neo4j database ([see dockerhub](https://hub.docker.com/_/neo4j)). For production use, you should map a volume for the container folder `/data`.
```sh
docker run -p 7687:7687 --env NEO4J_AUTH=neo4j/<password> neo4j
```

3. Execute the spring-boot plugin of the submodule [resource-server](resource-server) via `mvn spring-boot:run`.
