# TOP Backend

Spring Boot based backend of the TOP framework. Please see [top-deployment](https://onto-med.github.io/top-deployment)
for additional documentation.

[![Lint and Test](https://github.com/Onto-Med/top-backend/actions/workflows/lint-and-test.yml/badge.svg)](https://github.com/Onto-Med/top-backend/actions/workflows/lint-and-test.yml)

## Running the Spring Server

1. Set up environment variables:
    * `APP_PORT`: the port where the spring application will run on, e.g. 8080
    * `APP_PATH`: the context path, e.g. "/" for root
    * `DB_TYPE`: type of the DB to be used, defaults to postgresql
    * `DB_HOST`: host running the database server, defaults to localhost
    * `DB_NAME`: name of the database, defaults to postgres
    * `DB_PORT`: port of the database host, defaults to 5432
    * `DB_USER`: username for connecting to the database, defaults to postgres
    * `DB_PASS`: password for connecting to the database, required
    * `DATA_SOURCE_CONFIG_DIR`: location of data source configuration files, defaults to `config/data_sources`
    * `DOCUMENT_DATA_SOURCE_CONFIG_DIR`: location of document data source configuration files, defaults to `config/data_sources/nlp`
    * `QUERY_RESULT_DIR`: location where query results are stored to, defaults to `config/query_results`
    * `QUERY_RESULT_DOWNLOAD_ENABLED`: whether users with write permission for a repository can download query results
      or not, defaults to true
    * `TERMINOLOGY_SERVICE_ENDPOINT`: endpoint of the Ontology Lookup Service to be used for code search, default
      to http://localhost:9000/api (OLS3 is currently supported)  

   Document related:  
   *(The following variables will be overwritten by their respective adapter values if specified)*  
    * `DB_NEO4J_USER`: username for neo4j database, defaults to `neo4j`
    * `DB_NEO4J_PASS`: password for neo4j database (should be declared here and not written into an adapter)
    * `DB_ELASTIC_USER`: username for elasticsearch database, defaults to `elastic`
    * `DB_ELASTIC_PASS`: password for elasticsearch database (should be declared here and not written into an adapter)

   *(These are general configuration variables for the database that won't be declared in an adapter)*  
    * `DB_ELASTIC_CONNECTION_TIMEOUT`: timeout in seconds, defaults to `1s`
    * `DB_ELASTIC_SOCKET_TIMEOUT`: timeout in seconds, defaults to `30s`
    * `DB_NEO4J_CONNECTION_TIMEOUT`: timeout in seconds, defaults to `30s`  

   OAuth2 related:
    * `OAUTH2_ENABLED`: enable or disable oauth2, defaults to `false`
    * `OAUTH2_URL`: base URL of the OAuth2 server, defaults to `http://127.0.0.1:8081`
    * `OAUTH2_REALM`: name of the OAuth2 realm to be used for authentication
2. Start the PostgreSQL database ([see dockerhub](https://hub.docker.com/_/postgres)). Please review the documentation
   for production use.
    ```sh
    docker run --rm -p 5432:5432 -e POSTGRES_PASSWORD=password postgres
    ```
3. Start the Neo4J database ([see dockerhub](https://hub.docker.com/_/neo4j)). Please review the documentation for
   production use.
    ```sh
    docker run --rm -p 7687:7687 -e NEO4J_AUTH=neo4j/password neo4j
    ```
4. Start a default document index service (Elasticsearch) on the address specified in the adapter (if no adapter file is found defaults to `localhost:9200`).

5. Start the [concept graphs service](https://github.com/Onto-Med/concept-graphs) on the address specified in the adapter (if no adapter file is found defaults to `localhost:9007`).

6. Start the OAuth2 server ([see dockerhub](https://hub.docker.com/r/bitnami/keycloak)).

If you run the TOP Framework with an OAuth2 server, the first user that is created will have the admin role.

## NLP/Document related configuration

To utilize the document search of the framework, one needs three different services running: 
1. Elasticsearch or something similar
2. A Neo4j cluster
3. And the [concept graphs service](https://github.com/Onto-Med/concept-graphs)

The document search is adapter centric and one needs a working configuration file (yml) that specifies the addresses of said services
under the folder declared with the environment variable `DOCUMENT_DATA_SOURCE_CONFIG_DIR`.
If no `DOCUMENT_DEFAULT_ADAPTER` is specified, the first adapter found in the folder is used for setup.  

## Plugins

Any plugin you want to provide must be a member of the package `care.smith.top`.
It is sufficient to build JAR files and place them in the classpath of this application.

Please make sure to set all TOP Framework dependencies in your plugins to `provided`! e.g.:

```xml
<dependencies>
    <dependency>
        <groupId>care.smith.top</groupId>
        <artifactId>top-api</artifactId>
        <version>${version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Currently supported plugin types:

* **phenotype importer:** implement `care.smith.top.top_phenotypic_query.converter.PhenotypeImporter`
* **phenotype exporter:** implement `care.smith.top.top_phenotypic_query.converter.PhenotypeExporter`

## Development

### Coding Standard

The code in this repository, and in contributions provided via pull requests, should conform
to [Google Java Style](https://google.github.io/styleguide/javaguide.html).

We use the flag `--skip-reflowing-long-strings` for [google-java-format](https://github.com/google/google-java-format),
as it is currently not supported by all IDEs.

### Database Migrations

The application uses [Liquibase](https://www.liquibase.org) in combination with
[liquibase-maven-plugin](https://docs.liquibase.com/tools-integrations/maven/home.html) to manage migrations (changelog
files).

This section describes how to generate new changelogs based on modifications applied to JPA entities.
To generate new changelogs, a local HSQL database is used to reflect the state prior to changes.

1. Run `mvn liquibase:update` to apply all changelogs to the local HSQL database.
2. Make desired modifications to the JPA entities in `care.smith.top.backend.model`.
3. Recompile all JPA entities with `mvn compile`.
4. Run the following command to generate new changelogs:
   ```sh
   mvn liquibase:diff \
     -Dliquibase.diffChangeLogFile=src/main/resources/db/changelog/changesets/<timestamp>-<changelog name>.yaml
   ```
   You can call `set user.name=<change author>` before above command to modify the changelog author name.
5. Review the generated changelog file!

*There is a bug in `liquibase-maven-plugin` that results in recreation of some constraints and of the hibernate
sequence.
You should manually remove these changes from the generated changelog file.*

### NLP related Tests

On newer JDK versions, you might need the following arguments to run Neo4j tests:  
`--add-opens java.base/java.nio=ALL-UNNAMED`
`--add-opens java.base/java.lang=ALL-UNNAMED`

### Docker

Before building the Docker image, copy `.env.dist` to `.env` and fill in your GitHub username and Maven package registry authentication token.

## License

The code in this repository and the package `care.smith.top:top-backend` are licensed under [MIT](LICENSE).
