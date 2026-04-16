package hit400.cleo.recruitify.service.snovio;

import org.springframework.http.HttpStatusCode;

public class SnovIoException extends RuntimeException {
    private final HttpStatusCode status;
    private final String responseBody;

    public SnovIoException(HttpStatusCode status, String responseBody) {
        super("Snov.io request failed with status " + status + (responseBody == null || responseBody.isBlank() ? "" : (": " + responseBody)));
        this.status = status;
        this.responseBody = responseBody;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getResponseBody() {
        return responseBody;
    }
}

