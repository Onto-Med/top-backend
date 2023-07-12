package care.smith.top.backend.repository.nlp.custom;

import care.smith.top.backend.model.nlp.DocumentEntity;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.StringQuery;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightFieldParameters;

public class DocumentCustomRepositoryImpl implements DocumentCustomRepository {

  private final ElasticsearchOperations operations;

  public DocumentCustomRepositoryImpl(ElasticsearchOperations operations) {
    this.operations = operations;
  }

  @Override
  public List<DocumentEntity> getESDocumentsByTerms(String[] terms, String[] fields) {
    // ToDo: "hover" argument not hard coded
    ArrayList<DocumentEntity> documentEntities = new ArrayList<>();
    getSearchHitsByTerms(terms, fields, "Searched Term").stream()
        .forEach(
            sh -> {
              DocumentEntity de = sh.getContent();
              de.setHighlights(sh.getHighlightFields());
              documentEntities.add(de);
            });
    return documentEntities;
  }

  @Override
  public List<DocumentEntity> getESDocumentsByTermsBoolean(
      String[] shouldTerms, String[] mustTerms, String[] notTerms, String[] fields) {
    String mustString =
        (mustTerms != null) ? String.format("+( %s )", String.join(" ", mustTerms)) : "";
    String shouldString = (shouldTerms != null) ? String.join(" ", shouldTerms) : "";
    String notString =
        (notTerms != null) ? String.format("-( %s )", String.join(" ", notTerms)) : "";

    return getESDocumentsByTerms(
        new String[] {
          shouldString,
          !Objects.equals(mustString, "+(  )") ? mustString : null,
          !Objects.equals(notString, "-(  )") ? notString : null
        },
        fields);
  }

  @Override
  public List<DocumentEntity> getESDocumentsByPhrases(String[] phrases, String[] fields) {
    // ToDo: "hover" argument not hard coded
    ArrayList<DocumentEntity> documentEntities = new ArrayList<>();
    getSearchHitsByPhrases(phrases, fields[0], "Searched Phrase").stream()
        .forEach(
            sh -> {
              DocumentEntity de = sh.getContent();
              de.setHighlights(sh.getHighlightFields());
              documentEntities.add(de);
            });
    return documentEntities;
  }

  @Override
  public List<DocumentEntity> getESDocumentsByPhrasesBoolean(
      String[] shouldPhrases, String[] mustPhrases, String[] notPhrases, String[] fields) {
    ArrayList<DocumentEntity> documentEntities = new ArrayList<>();
    getSearchHitsByPhrasesBoolean(
            shouldPhrases, mustPhrases, notPhrases, fields[0], "Searched Phrase")
        .stream()
        .forEach(
            sh -> {
              DocumentEntity de = sh.getContent();
              de.setHighlights(sh.getHighlightFields());
              documentEntities.add(de);
            });
    return documentEntities;
  }

  private SearchHits<DocumentEntity> getSearchHitsByPhrases(
      String[] phrases, String field, String hover) {
    // ToDo: no hardcoded "slop"
    Query searchQuery =
        new StringQuery(
            String.format("{\"bool\":\n { %s } \n}", boolQueryPart("should", phrases, field, 2)));
    return getSearchHitsByQuery(searchQuery, new String[] {field}, hover);
  }

  private SearchHits<DocumentEntity> getSearchHitsByPhrasesBoolean(
      String[] shouldPhrases,
      String[] mustPhrases,
      String[] notPhrases,
      String field,
      String hover) {
    Query searchQuery =
        new StringQuery(
            String.format(
                "{\"bool\":\n { %s, %s, %s } \n}",
                boolQueryPart("should", shouldPhrases, field, 2),
                boolQueryPart("must", mustPhrases, field, 2),
                boolQueryPart("must_not", notPhrases, field, 2)));
    return getSearchHitsByQuery(searchQuery, new String[] {field}, hover);
  }

  private SearchHits<DocumentEntity> getSearchHitsByTerms(
      String[] terms, String[] fields, String hover) {
    String queryString =
        Arrays.stream(terms)
            .filter(Objects::nonNull)
            .filter(t -> t.length() > 0)
            .collect(Collectors.joining(" "));
    String fieldsString =
        Arrays.stream(fields).map(field -> "\"" + field + "\"").collect(Collectors.joining(", "));

    //        String finalQueryString = queryString.substring(Math.max(0,
    // queryString.indexOf("("))).trim();
    Query searchQuery =
        new StringQuery(
            "{"
                + "\"query_string\": {"
                + "\"fields\": ["
                + fieldsString
                + "],"
                + "\"query\": \""
                + queryString
                + "\""
                + "}"
                + "}");
    return getSearchHitsByQuery(searchQuery, fields, hover);
  }

  private SearchHits<DocumentEntity> getSearchHitsByQuery(
      Query query, String[] fields, String hover) {
    String addString = (hover != null) ? String.format("title=\"%s\"", hover) : "";
    String spanTagStart =
        "<span" + " style=\"background-color:#FF000080; border-radius:3px; padding:2px\"" + " %s>";

    Highlight highlight =
        new Highlight(
            Arrays.stream(fields)
                .map(
                    s ->
                        new HighlightField(
                            s,
                            // makes it so, that not only fragments (which contain the phrase) are
                            // returned but the whole field
                            HighlightFieldParameters.builder()
                                .withNumberOfFragments(0)
                                .withPreTags(new String[] {String.format(spanTagStart, addString)})
                                .withPostTags(new String[] {"</span>"})
                                .build()))
                .collect(Collectors.toList()));
    query.setHighlightQuery(new HighlightQuery(highlight, DocumentEntity.class));
    return operations.search(query, DocumentEntity.class);
  }

  private String boolQueryPart(String bool, String[] phrases, String field, int slop) {
    if (phrases == null) {
      return "\n\"" + bool + "\": [ \n]";
    }
    String matchQuery = "\n{\"match_phrase\": { \"%s\": { \"query\": \"%s\", \"slop\": %s } } }";
    return Arrays.stream(phrases)
        .map(s -> String.format(matchQuery, field, s, slop))
        .collect(Collectors.joining(",", "\n\"" + bool + "\": [", "\n]"));
  }
  ;
}
