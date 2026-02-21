package hit400.cleo.recruitify.dto;

import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.multipart.MultipartFile;

public record CvUploadRequest(FilePart cvFile) {
}
