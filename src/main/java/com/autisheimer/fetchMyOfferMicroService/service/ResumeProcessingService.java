package com.autisheimer.fetchMyOfferMicroService.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ResumeProcessingService {

    private final VectorStore vectorStore;

    // Spring AI automatically wires up PgVectorStore here
    public ResumeProcessingService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void processAndStoreResume(MultipartFile file) throws IOException {
        // 1. Convert the uploaded file into a Spring Resource
        Resource pdfResource = new InputStreamResource(file.getInputStream());

        // 2. Extract raw text from the PDF using Apache Tika
        TikaDocumentReader documentReader = new TikaDocumentReader(pdfResource);
        List<Document> rawDocuments = documentReader.get();

        // 3. Chunk the text so the LLM doesn't lose context
        TokenTextSplitter splitter = new TokenTextSplitter(800, 400, 5, 10000, true);
        List<Document> chunkedDocuments = splitter.apply(rawDocuments);

        // 4. Attach metadata so we can filter searches later
        for (Document doc : chunkedDocuments) {
            doc.getMetadata().put("doc_type", "candidate_resume");
        }

        // 5. Save to Vector Store (Spring AI handles the Gemini embedding generation automatically here!)
        vectorStore.add(chunkedDocuments);
    }
}