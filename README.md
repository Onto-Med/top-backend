# TOP Backend

Spring Boot based backend of the TOP framework

## Running the Spring Server

1. Set up environment variables:
* `APP_PORT`: the port where the spring application will run on, e.g. 8080
* `APP_PATH`: the context path, e.g. "/" for root
* `DB_HOST`: host running the database server with the [generic-ontology-db](https://github.com/Onto-Med/generic-ontology-db)
* `DB_PORT`: port of the database host
* `DB_DATABASE`: name of the database
* `DB_USER`: username for connecting to the database
* `DB_PASS`: password for connecting to the database

2. Start the PostgreSQL database, containing the [generic-ontology-db](https://github.com/Onto-Med/generic-ontology-db). For production use, you should map a volume for the container folder `/var/lib/postgresql/data`.
```sh
docker run -p 5432:5432 ghcr.io/onto-med/generic-ontology-db:latest
```
3. Execute the spring-boot plugin of the sub module [resource-server](resource-server) via `mvn spring-boot:run`.
