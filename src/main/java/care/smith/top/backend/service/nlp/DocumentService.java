package care.smith.top.backend.service.nlp;

import care.smith.top.backend.model.nlp.DocumentEntity;
import care.smith.top.backend.model.nlp.DocumentNodeEntity;
import care.smith.top.backend.repository.nlp.DocumentNodeRepository;
import care.smith.top.backend.repository.nlp.DocumentRepository;
import care.smith.top.backend.service.ContentService;
import care.smith.top.model.Document;
import org.neo4j.cypherdsl.core.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DocumentService implements ContentService {

    private final DocumentRepository documentRepository;
    private final DocumentNodeRepository documentNodeRepository;

    @Autowired
    public DocumentService(DocumentRepository documentRepository, DocumentNodeRepository documentNodeRepository) {
        this.documentRepository = documentRepository;
        this.documentNodeRepository = documentNodeRepository;
    }

    @Override
    @Cacheable("documentCount")
    public long count() { return documentNodeRepository.count(); }


    public Document getDocumentById(String documentId) {
        DocumentEntity document = documentRepository.findById(documentId).orElse(null);
        if (document != null) {
            return documentEntityMapper.apply(document);
        } else {
            return new Document().id("No Id").text("No text").name("No Name");
        }
    }

    public Document getDocumentByName(String documentName) {
        return documentEntityMapper.apply(
                documentRepository.findDocumentEntityByDocumentName(documentName)
        );
    }

    public List<Document> getDocumentsByTerms(String[] terms, String[] fields) {
        return documentRepository
                .getDocumentsByTerms(terms, fields)
                .stream()
                .map(documentEntityMapper)
                .collect(Collectors.toList());
    }

    public List<Document> getDocumentsByTermsBoolean(String[] mustTerms, String[] shouldTerms, String[] notTerms, String[] fields) {
        List<String> mustTermsList = new ArrayList<>();
        for (String s: mustTerms ) {
            mustTermsList.add(String.format("(%s)", s));
        }
        String mustString = (mustTermsList != null) ? String.format("+( %s )",
                String.join(" ", mustTermsList)
        ) : "";
        String shouldString = (shouldTerms != null) ? String.join(" ", shouldTerms) : "";
        String notString = (notTerms != null) ? String.format("-( %s )", String.join(" ", notTerms)) : "";
        return getDocumentsByTerms(new String[]{
                shouldString,
                !Objects.equals(mustString, "+(  )") ? mustString : null,
                !Objects.equals(notString, "-(  )") ? notString : null
        }, fields);
    }

    public List<Document> getDocumentsForConcepts(Set<String> conceptIds, Boolean exemplarOnly) {
        if (conceptIds.size() == 0) {
            return List.of();
        }
//        HashMap<String, Document> docMap = documentNodeRepository
//                .findAll(documentsForConceptsUnion(conceptIds, exemplarOnly))
//                .stream()
//                .map(documentNodeEntityMapper)
//                .collect(Collectors.toMap(Document::getId, Function.identity(), (prev, next) -> next, HashMap::new));
//
//        return new ArrayList<>(docMap.values());
        return documentNodeRepository.getDocumentsForConcepts(List.copyOf(conceptIds), exemplarOnly)
                .stream()
                .map(documentNodeEntityMapper)
                .collect(Collectors.toList());
        // I added DISTINCT to neo4j query, so this shouldn't be necessary anymore
//                .collect(Collectors.toMap(Document::getId, Function.identity(), (prev, next) -> next, HashMap::new));
//        return new ArrayList<>(docMap.values());
    }

    private String parentheses(String s) {
        return String.format("(%s)", s);
    }

    static Statement documentsForConceptsUnion(Set<String> conceptIds, Boolean exemplarOnly) {
        Node document = Cypher.node("Document").named("document");
        Node phrase;
        if (exemplarOnly) {
            phrase = Cypher.node("Phrase").withProperties(Map.of("exemplar", true)).named("phrase");
        } else {
            phrase = Cypher.node("Phrase").named("phrase");
        }
        Node concept = Cypher.node("Concept").named("concept");

        return Cypher
                .match(
                        concept.relationshipFrom(phrase, "IN_CONCEPT"),
                        document.relationshipTo(phrase, "HAS_PHRASE")
                )
                .where(concept.property("conceptId").in(someIdList(conceptIds)))
                .returning(document)
                .build();
    }

    static Expression someIdList(Set<String> someIds) {
        return Cypher.listOf(someIds
                .stream()
                .map(Cypher::literalOf)
                .collect(Collectors.toList())
        );
    }

    private final Function<DocumentEntity, Document> documentEntityMapper = documentEntity -> new Document()
            .id(documentEntity.getId())
            .name(documentEntity.getDocumentName())
            .text(documentEntity.getDocumentText())
            .highlightedText(documentEntity
                    .getHighlights()
                    .values()
                    .stream()
                    .flatMap(Collection::stream)
                    .collect(Collectors.joining())
            );

    private final Function<DocumentNodeEntity, Document> documentNodeEntityMapper = documentNodeEntity -> new Document()
            .id(documentNodeEntity.documentId())
            .name(documentNodeEntity.documentName());
}
