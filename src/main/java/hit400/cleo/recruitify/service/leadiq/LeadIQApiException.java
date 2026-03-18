package hit400.cleo.recruitify.service.leadiq;
 
import org.springframework.http.HttpStatusCode;
 
public class LeadIQApiException extends RuntimeException {
    private final HttpStatusCode status;
    private final String responseBody;
 
    public LeadIQApiException(HttpStatusCode status, String responseBody) {
        super("LeadIQ API request failed with status " + status + (responseBody == null || responseBody.isBlank() ? "" : (": " + responseBody)));
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
