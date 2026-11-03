package care.smith.top.backend.api.nlp;

public class InvalidDocumentSelectionException extends RuntimeException {
  public InvalidDocumentSelectionException(String message) {
    super(message);
  }
}
