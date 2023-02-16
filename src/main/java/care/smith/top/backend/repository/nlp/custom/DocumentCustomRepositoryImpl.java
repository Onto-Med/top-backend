package care.smith.top.backend.repository.nlp.custom;

import care.smith.top.backend.model.nlp.DocumentEntity;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;

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

//        String spanTag = "<span style=\"border-width:3px; border-style:solid; border-color:#FF0000; padding:1px\">";
        String spanTag = "<span style=\"background-color:#FF000080; border-radius:3px; padding:2px\">";
        Highlight highlight = new Highlight(
                Arrays.stream(fields)
                        .map(s -> new HighlightField(s,
                                // makes it so, that not only fragments (which contain the phrase) are returned but the whole field
                                HighlightFieldParameters.builder()
                                        .withNumberOfFragments(0)
                                        .withPreTags(new String[]{spanTag})
                                        .withPostTags(new String[]{"</span>"})
                                        .build())
                        ).collect(Collectors.toList())
        );
        searchQuery.setHighlightQuery(new HighlightQuery(highlight, DocumentEntity.class));
        return operations.search(searchQuery, DocumentEntity.class);
    }
}
