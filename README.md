# TOP Backend

Spring Boot based backend of the TOP framework. Please see [top-deployment](https://onto-med.github.io/top-deployment)
for additional documentation.

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
    * `QUERY_RESULT_DIR`: location where query results are stored to, defaults to `config/query_results`
    * `QUERY_RESULT_DOWNLOAD_ENABLED`: whether users with write permission for a repository can download query results
      or not, defaults to true
    * `TERMINOLOGY_SERVICE_ENDPOINT`: endpoint of the Ontology Lookup Service to be used for code search, default
      to http://localhost:9000/api (OLS3 is currently supported)

   Document related:
    * `DB_NEO4J_HOST`: host running the neo4j database server, defaults to neo4j
    * `DB_NEO4J_PORT`: port of the neo4j database, defaults to 7687
    * `DB_NEO4J_USER`: username for neo4j database, defaults to neo4j
    * `DB_NEO4J_PASS`: password for neo4j database

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
4. Start the OAuth2 server ([see dockerhub](https://hub.docker.com/r/bitnami/keycloak)).

If you run the TOP Framework with an OAuth2 server, the first user that is created will have the admin role.

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

## License

The code in this repository and the package `care.smith.top:top-backend` are licensed under [MIT](LICENSE).
