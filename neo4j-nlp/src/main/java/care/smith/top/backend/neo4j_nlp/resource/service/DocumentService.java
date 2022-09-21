package care.smith.top.backend.neo4j_nlp.resource.service;

import care.smith.top.backend.model.Organisation;
import care.smith.top.backend.neo4j_nlp.neo4_access.model.Document;
import care.smith.top.backend.neo4j_ontology_access.repository.DirectoryRepository;
import org.neo4j.cypherdsl.core.Cypher;
import org.neo4j.cypherdsl.core.Node;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.neo4j.repository.support.CypherdslConditionExecutor;
import org.springframework.data.neo4j.repository.support.CypherdslStatementExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired
    DirectoryRepository directoryRepository;

    public List<Organisation> getOrganisations(String name, Integer page, List<String> include) {
        return directoryRepository
                .findAllByTypeAndNameAndDescription("Organisation", name, null)
                .stream()
                .map(this::directoryToOrganisation)
                .collect(Collectors.toList());
    }
}
