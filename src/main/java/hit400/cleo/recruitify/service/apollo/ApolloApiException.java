package hit400.cleo.recruitify.service.apollo;

import org.springframework.http.HttpStatusCode;

public class ApolloApiException extends RuntimeException {
    private final HttpStatusCode status;
    private final String responseBody;

    public ApolloApiException(HttpStatusCode status, String responseBody) {
        super("Apollo API request failed with status " + status + (responseBody == null || responseBody.isBlank() ? "" : (": " + responseBody)));
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

