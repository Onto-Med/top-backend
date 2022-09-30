package care.smith.top.backend.api;

import care.smith.top.backend.service.DocumentService;
import care.smith.top.model.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class DocumentApiDelegateImpl implements DocumentApiDelegate {

    @Autowired
    DocumentService documentService;

    @Override
    public ResponseEntity<List<Document>> getDocuments(List<String> include, String id) {
//        return new ResponseEntity<>(Collections.singletonList(new Document().id("0").text("Test")), HttpStatus.OK);
        return new ResponseEntity<>(documentService.getDocuments(), HttpStatus.OK);
    }

//    @Override
//    public ResponseEntity<List<Document>> getDocuments() {
//        return new ResponseEntity<>(documentService.getDocuments(), HttpStatus.OK);
//    }
}
