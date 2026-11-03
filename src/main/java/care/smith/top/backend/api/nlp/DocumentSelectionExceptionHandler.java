package care.smith.top.backend.api.nlp;

import care.smith.top.backend.api.DocumentApiController;
import care.smith.top.model.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice(assignableTypes = DocumentApiController.class)
public class DocumentSelectionExceptionHandler {
  @ExceptionHandler(InvalidDocumentSelectionException.class)
  public ResponseEntity<ErrorResponse> invalidSelection(
      InvalidDocumentSelectionException exception) {
    return ResponseEntity.badRequest().body(new ErrorResponse(400, exception.getMessage()));
  }

  @ExceptionHandler({ConstraintViolationException.class, HandlerMethodValidationException.class})
  public ResponseEntity<ErrorResponse> invalidRequest() {
    return ResponseEntity.badRequest().body(new ErrorResponse(400, "Invalid document request"));
  }

  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> missingParameter(
      MissingServletRequestParameterException exception)
      throws MissingServletRequestParameterException {
    if (!"index".equals(exception.getParameterName())) throw exception;
    return ResponseEntity.badRequest()
        .body(new ErrorResponse(400, "An explicit document index is required"));
  }
}
