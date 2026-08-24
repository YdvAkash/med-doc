package med.com.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.Block;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextRequest;
import software.amazon.awssdk.services.textract.model.DetectDocumentTextResponse;
import software.amazon.awssdk.services.textract.model.Document;
import software.amazon.awssdk.services.textract.model.S3Object;
import software.amazon.awssdk.services.textract.model.TextractException;

@Service
@RequiredArgsConstructor
@Slf4j
public class TextractService {

    private final TextractClient textractClient;

    /**
     * Extracts raw text from an image or single-page PDF stored in S3.
     */
    public String extractTextFromS3(String bucketName, String s3Key) {
        log.info("Starting text extraction for s3://{}/{}", bucketName, s3Key);

        try {
            S3Object s3Object = S3Object.builder()
                    .bucket(bucketName)
                    .name(s3Key)
                    .build();

            Document document = Document.builder()
                    .s3Object(s3Object)
                    .build();

            DetectDocumentTextRequest request = DetectDocumentTextRequest.builder()
                    .document(document)
                    .build();

            DetectDocumentTextResponse response = textractClient.detectDocumentText(request);

            StringBuilder extractedText = new StringBuilder();
            for (Block block : response.blocks()) {
                if (block.blockTypeAsString().equals("LINE")) {
                    extractedText.append(block.text()).append("\n");
                }
            }

            log.info("Successfully extracted text from s3://{}/{}", bucketName, s3Key);
            return extractedText.toString();

        } catch (TextractException e) {
            log.error("Error extracting text from document: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to extract text using AWS Textract", e);
        }
    }
}
