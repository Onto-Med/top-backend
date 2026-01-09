# TOP Backend

Spring Boot based backend of the TOP framework. Please see [top-deployment](https://onto-med.github.io/top-deployment)
for additional documentation.

[![Lint and Test](https://github.com/Onto-Med/top-backend/actions/workflows/lint-and-test.yml/badge.svg)](https://github.com/Onto-Med/top-backend/actions/workflows/lint-and-test.yml)

## Running the Spring Server

Because some Maven dependencies are hosted at [GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry),
you need to make some modifications to your Maven installation in order to run the TOP Backend server. Please follow the
[Authenticating to GitHub Packages](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry#authenticating-to-github-packages)
instructions.

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
    * `TERMINOLOGY_SERVICE_ENDPOINT`: endpoint of the Ontology Lookup Service to be used for code search, defaults
      to https://www.ebi.ac.uk/ols4/api (OLS4 is currently supported)
    * `IMPORT_DEMO_DATA`: whether a demo organisation with a small BMI phenotype model should be imported on startup,
      defaults to false
    * `MAX_BATCH_SIZE`: max number of entities that can be uploaded to the server in one batch

   Document related:
    * `DB_NEO4J_HOST`: Neo4j database server name, defaults to `localhost`
    * `DB_NEO4J_PORT`: Neo4J database port, defaults to 7687
    * `DB_NEO4J_USER`: username for Neo4j database, defaults to `neo4j`
    * `DB_NEO4J_PASS`: password for Neo4j database (should be declared here and not written into an adapter)
    * `DB_NEO4J_CONNECTION_TIMEOUT`: timeout in seconds for Neo4j requests, defaults to 30
    * `CONCEPT_GRAPHS_API_ENDPOINT`: API endpoint of the concept-graphs service, defaults to `http://localhost:9007`
    * `CONCEPT_GRAPHS_API_ENABLED`: Whether the concept-graphs-api should be available.
    * `MAX_COMBINED_DOCUMENTS_UPLOAD`: restricts the maximum combined upload size of a document batch (defaults to 2MB); should be on par with the respective value in the frontend

   OAuth2 related:
    * `OAUTH2_ENABLED`: enable or disable oauth2, defaults to `false`
    * `OAUTH2_URL`: base URL of the OAuth2 server, defaults to `http://127.0.0.1:8081`
    * `OAUTH2_DOCKER_URL`: This URL defaults to `OAUTH2_URL`. In a docker setup, you can use it to configure the Docker network internal URL to access the OAuth2 server. In this case, JWTs are verified agains `OAUTH2_DOCKER_URL`, whereas `OAUTH2_URL` is used to verify the issuer of JWTs.
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
4. Start a default document index service (Elasticsearch) on the address specified in the adapter (if no adapter file is found defaults to `localhost:9008`).

5. Start the [concept graphs service](https://github.com/Onto-Med/concept-graphs) on the address specified in the adapter (if no adapter file is found defaults to `localhost:9007`).

6. Start the OAuth2 server ([see dockerhub](https://hub.docker.com/r/bitnami/keycloak)).

If you run the TOP Framework with an OAuth2 server, the first user that is created will have the admin role.

## NLP/Document related configuration

To utilize the document search of the framework, one needs three different services running:
1. [concept graphs service](https://github.com/Onto-Med/concept-graphs) (default: `http://localhost:9007`)
2. Neo4j cluster (default: `bolt://localhost:7687`)
3. Document servers (e.g. Elasticsearch)

1 and 2 are configurable via environment variables.

The document search is adapter centric and one needs a working configuration file (yml) that specifies the address of
a document server in the folder declared with the environment variable `DOCUMENT_DATA_SOURCE_CONFIG_DIR`.
You can find more information about the adapter specification under [top-document-query](https://github.com/Onto-Med/top-document-query).  

The [concept graphs service](https://github.com/Onto-Med/concept-graphs) is responsible for generating graphs of related
phrases from a document source (either via upload or an external data/document server, such as Elasticsearch).
These graphs in turn are then represented as `concept nodes`, `phrase nodes` and `document nodes` on a Neo4j cluster
where they serve as a way to search/explore documents.  

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

If your IDE does not support file formatting, you can get a JAR release of [google-java-format](https://github.com/google/google-java-format) and run the
following command from the root of this repository:  
```sh
java -jar google-java-format-CURRENT_VERSION-all-deps.jar --skip-reflowing-long-strings --replace $(git ls-files *.java)
```

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

To build a Docker image, you need to provide your GitHub username and [personal access token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens)
as [Docker secrets](https://docs.docker.com/engine/swarm/secrets/). Assuming they are available
as environment variables:

```sh
docker build --secret id=GH_MAVEN_PKG_USER --secret id=GH_MAVEN_PKG_AUTH_TOKEN -t top-backend .
```

## License

The code in this repository and the package `care.smith.top:top-backend` are licensed under [MIT](LICENSE).
