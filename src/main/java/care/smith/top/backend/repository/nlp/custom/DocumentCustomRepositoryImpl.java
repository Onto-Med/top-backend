package care.smith.top.backend.repository.nlp.custom;

import care.smith.top.backend.model.nlp.DocumentEntity;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.SearchHit;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class DocumentCustomRepositoryImpl implements DocumentCustomRepository{

    private final ElasticsearchOperations operations;

    public DocumentCustomRepositoryImpl(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public List<DocumentEntity> getDocumentsByTerms(String[] terms, String[] fields) {
        String queryString = String.join(" ", terms);
        String fieldsString = Arrays
                .stream(fields)
                .map(field -> "\"" + field + "\"")
                .collect(Collectors.joining(", "));

        Query searchQuery = new StringQuery(
                "{" +
                        "\"query_string\": {" +
                        "\"fields\": [" + fieldsString + "]," +
                        "\"query\": \"" + queryString + "\"" + "}}"
        );

        return operations
                .search(searchQuery, DocumentEntity.class)
                .stream()
                .map(SearchHit::getContent)
                .collect(Collectors.toList());
    }
}
