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
                .getESDocumentsByTerms(terms, fields)
                .stream()
                .map(documentEntityMapper)
                .collect(Collectors.toList());
    }

    public List<Document> getDocumentsByTermsBoolean(String[] mustTerms, String[] shouldTerms, String[] notTerms, String[] fields) {
        return documentRepository
                .getESDocumentsByTermsBoolean(shouldTerms, mustTerms, notTerms, fields)
                .stream()
                .map(documentEntityMapper)
                .collect(Collectors.toList());
    }

    public List<Document> getDocumentsByPhrases(String[] phrases, String[] fields) {
        return documentRepository
                .getESDocumentsByPhrases(phrases, fields)
                .stream()
                .map(documentEntityMapper)
                .collect(Collectors.toList());
    }

    public List<Document> getDocumentsByPhrasesBoolean(String[] mustPhrases, String[] shouldPhrases, String[] notPhrases, String[] fields) {
        return documentRepository
                .getESDocumentsByPhrasesBoolean(shouldPhrases, mustPhrases, notPhrases, fields)
                .stream()
                .map(documentEntityMapper)
                .collect(Collectors.toList());
    }

    public List<Document> getDocumentsForConcepts(Set<String> conceptIds, Boolean exemplarOnly) {
        if (conceptIds.size() == 0) {
            return List.of();
        }
        return documentNodeRepository.getDocumentsForConcepts(List.copyOf(conceptIds), exemplarOnly)
                .stream()
                .map(documentNodeEntityMapper)
                .collect(Collectors.toList());
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
