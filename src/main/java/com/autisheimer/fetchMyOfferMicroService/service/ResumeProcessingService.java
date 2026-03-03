package com.autisheimer.fetchMyOfferMicroService.service;
//
//
//import org.springframework.ai.document.Document;
//import org.springframework.ai.embedding.EmbeddingModel;
//import org.springframework.ai.reader.tika.TikaDocumentReader;
//import org.springframework.ai.transformer.splitter.TokenTextSplitter;
//import org.springframework.ai.vectorstore.VectorStore;
//import org.springframework.core.io.InputStreamResource;
//import org.springframework.core.io.Resource;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.List;
//
//@Service
//public class ResumeProcessingService {
//
//    private final VectorStore vectorStore;
//
//    // Spring AI automatically provides the PgVectorStore implementation here
//    public ResumeProcessingService(VectorStore vectorStore) {
//        this.vectorStore = vectorStore;
//    }
//    public ResumeProcessingService(VectorStore vectorStore, EmbeddingModel embeddingModel)   {
//        this.vectorStore = vectorStore;
//    }
//
//    public void processAndStoreResume(MultipartFile file) throws IOException {
//        System.out.println("📄 Starting PDF parsing and vectorization...");
//
//        // 1. Convert the uploaded file into a Spring Resource
//        Resource pdfResource = new InputStreamResource(file.getInputStream());
//
//        // 2. Extract raw text from the PDF using Apache Tika
//        TikaDocumentReader documentReader = new TikaDocumentReader(pdfResource);
//        List<Document> rawDocuments = documentReader.get();
//        System.out.println("✅ Extracted raw text from PDF.");
//
//        // 3. Chunk the text so the LLM doesn't lose context
//        // This splits your resume into overlapping tokens
//        TokenTextSplitter splitter = new TokenTextSplitter(800, 400, 5, 10000, true);
//        List<Document> chunkedDocuments = splitter.apply(rawDocuments);
//
//        // 4. Attach metadata so we can filter searches later
//        for (Document doc : chunkedDocuments) {
//            doc.getMetadata().put("doc_type", "candidate_resume");
//        }
//
////        System.out.println("🧩 Split resume into " + chunkedDocuments.size() + " vector chunks.");
////
////        // 5. Generate embeddings locally and save to PostgreSQL (pgvector)
////        System.out.println("🧠 Generating embeddings and saving to pgvector...");
////        vectorStore.add(chunkedDocuments);
////
////        System.out.println("🎉 Resume successfully embedded into system memory!");
//        // ... existing extraction and splitting code ...
//        System.out.println("🧩 Split resume into " + chunkedDocuments.size() + " vector chunks.");
//
//        // 🛑 NEW: Manually generate and print the embeddings BEFORE saving to the database
//        System.out.println("🔍 Testing Gemini Vector Generation...");
//        for (int i = 0; i < chunkedDocuments.size(); i++) {
//            Document doc = chunkedDocuments.get(i);
//            // Call Gemini to embed this specific chunk of text
//            float[] vector = embeddingModel.embed(doc);
//
//            System.out.println("Chunk " + (i+1) + " Vector Size: " + vector.length + " dimensions");
//            System.out.print("Chunk " + (i+1) + " Preview: [");
//            // Print just the first 5 numbers of the 768 so we don't flood the console
//            for(int j = 0; j < 5; j++) {
//                System.out.print(vector[j] + ", ");
//            }
//            System.out.println("...]");
//        }
//
//        System.out.println("🧠 Attempting to save to pgvector database...");
//        // 3. Save to Vector Store
//        vectorStore.add(chunkedDocuments);
//
//
//    }
//}


//package com.autisheimer.fetchMyOfferMicroService.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
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
    private final EmbeddingModel embeddingModel; // <-- FIXED: Added class-level variable

    // FIXED: Single constructor injecting both beans
    public ResumeProcessingService(VectorStore vectorStore, EmbeddingModel embeddingModel) {
        this.vectorStore = vectorStore;
        this.embeddingModel = embeddingModel; // <-- FIXED: Assigned the variable
    }

    public void processAndStoreResume(MultipartFile file) throws IOException {
        System.out.println("📄 Starting PDF parsing and vectorization...");

        // 1. Convert the uploaded file into a Spring Resource
        Resource pdfResource = new InputStreamResource(file.getInputStream());

        // 2. Extract raw text from the PDF using Apache Tika
        TikaDocumentReader documentReader = new TikaDocumentReader(pdfResource);
        List<Document> rawDocuments = documentReader.get();
        System.out.println("✅ Extracted raw text from PDF.");

        // 3. Chunk the text so the LLM doesn't lose context
        TokenTextSplitter splitter = new TokenTextSplitter(800, 400, 5, 10000, true);
        List<Document> chunkedDocuments = splitter.apply(rawDocuments);

        // 4. Attach metadata so we can filter searches later
        for (Document doc : chunkedDocuments) {
            doc.getMetadata().put("doc_type", "candidate_resume");
        }

        System.out.println("🧩 Split resume into " + chunkedDocuments.size() + " vector chunks.");

        // 🛑 Manually generate and print the embeddings BEFORE saving to the database
        System.out.println("🔍 Testing Gemini Vector Generation...");
        for (int i = 0; i < chunkedDocuments.size(); i++) {
            Document doc = chunkedDocuments.get(i);

            // Call Gemini to embed this specific chunk of text
            float[] vector = embeddingModel.embed(doc);

            System.out.println("Chunk " + (i+1) + " Vector Size: " + vector.length + " dimensions");
            System.out.print("Chunk " + (i+1) + " Preview: [");
            // Print just the first 5 numbers of the 768 so we don't flood the console
            for(int j = 0; j < 5; j++) {
                System.out.print(vector[j] + ", ");
            }
            System.out.println("...]");
        }

        System.out.println("🧠 Attempting to save to pgvector database...");
        // 5. Save to Vector Store
        vectorStore.add(chunkedDocuments);
        System.out.println("🎉 Resume successfully embedded into system memory!");
    }
}