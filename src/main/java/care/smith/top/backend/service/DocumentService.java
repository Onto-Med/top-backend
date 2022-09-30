package care.smith.top.backend.service;

import care.smith.top.model.Document;
import care.smith.top.backend.repository.DocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class DocumentService {

    @Autowired DocumentRepository documentRepository;

    public List<Document> getDocuments() {
        return documentRepository
                .findAll()
                .stream()
                .map(documentEntity -> new Document()
                        .id(documentEntity.documentId())
                        .text(documentEntity.documentText())
                )
                .collect(Collectors.toList());
    }
}
