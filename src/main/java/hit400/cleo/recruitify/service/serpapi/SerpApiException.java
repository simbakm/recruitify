package hit400.cleo.recruitify.service.serpapi;

import org.springframework.http.HttpStatusCode;

public class SerpApiException extends RuntimeException {
    private final HttpStatusCode status;
    private final String responseBody;

    public SerpApiException(HttpStatusCode status, String responseBody) {
        super("SerpAPI request failed with status " + status + (responseBody == null || responseBody.isBlank() ? "" : (": " + responseBody)));
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

