package care.smith.top.backend.repository.nlp.custom;

import care.smith.top.backend.model.nlp.DocumentEntity;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;

import java.util.*;
import java.util.stream.Collectors;

public class DocumentCustomRepositoryImpl implements DocumentCustomRepository{

    private final ElasticsearchOperations operations;

    public DocumentCustomRepositoryImpl(ElasticsearchOperations operations) {
        this.operations = operations;
    }

    @Override
    public List<DocumentEntity> getDocumentsByTerms(String[] terms, String[] fields) {
        ArrayList<DocumentEntity> documentEntities = new ArrayList<>();
        getSearchHitsByTerms(terms, fields)
            .stream()
            .forEach(sh -> {
                DocumentEntity de = sh.getContent();
                de.setHighlights(sh.getHighlightFields());
                documentEntities.add(de);
            });
        return documentEntities;
    }

    private SearchHits<DocumentEntity> getSearchHitsByTerms(String[] terms, String[] fields) {
        String queryString = Arrays.stream(terms)
                .filter(Objects::nonNull)
                .filter(t -> t.length() > 0)
                .collect(Collectors.joining(" "));
        String fieldsString = Arrays.stream(fields)
                .map(field -> "\"" + field + "\"")
                .collect(Collectors.joining(", "));

        Query searchQuery = new StringQuery(
                "{" +
                            "\"query_string\": {" +
                                "\"fields\": [" + fieldsString + "]," +
                                "\"query\": \"" + queryString + "\"" +
                            "}" +
                        "}"
        );

        Highlight highlight = new Highlight(
                Arrays.stream(fields).map(HighlightField::new).collect(Collectors.toList())
        );
        searchQuery.setHighlightQuery(new HighlightQuery(highlight, DocumentEntity.class));
        return operations.search(searchQuery, DocumentEntity.class);
    }
}
