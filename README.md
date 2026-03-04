# 🚀 Fetch My Offer - Autonomous AI Job Hunter

**Fetch My Offer** is a fully autonomous, RAG-driven microservice architecture that acts as a personal AI recruiter. It dynamically generates search queries based on your embedded resume, commands a fleet of web scrapers, evaluates job descriptions using LLMs, and sends highly curated job matches directly to your phone.

## 🧠 System Architecture

This project is built using a distributed microservice architecture:
1. **The Brain (Spring Boot + Spring AI):** Orchestrates the pipeline, generates dynamic search queries based on the candidate's skills, and evaluates job matches.
2. **The Memory (PostgreSQL + pgvector):** A cloud-hosted vector database that stores the candidate's resume as mathematical embeddings and maintains a "Seen Jobs" ledger for deduplication.
3. **The Worker (Python + FastAPI):** A separate Dockerized web-scraping microservice using headless Playwright to extract dynamic DOM content.
4. **The Messenger (Telegram API):** Pushes formatted, actionable job alerts to the user's smartphone.

## ✨ Key Features

* **Autonomous Scheduling:** A CRON-based orchestrator wakes up daily, requires zero manual input, and hunts for jobs automatically.
* **Dynamic Query Generation:** Uses Groq (Llama 3) to analyze your resume and generate the optimal search keywords (e.g., "Java Backend Intern", "Full Stack Engineer") for that specific day.
* **Retrieval-Augmented Generation (RAG):** Uses Google Gemini to convert your resume into vector embeddings, allowing the system to mathematically compare your skills against job requirements.
* **Zero-Shot AI Evaluation:** Bypasses basic keyword matching. The AI reads the job description and explicitly reasons why you are or are not a fit before sending an alert.
* **Smart Deduplication:** Remembers every job URL it has processed in the PostgreSQL database, saving API limits and preventing notification spam.

## 🛠️ Tech Stack

* **Backend:** Java 21, Spring Boot 3.4, Spring AI, Spring Data JPA
* **AI/LLMs:** Groq (Llama 3.3 70B), Google Gemini (Text Embeddings)
* **Database:** Supabase (PostgreSQL with `pgvector`)
* **Cloud Deployment:** Render (Dockerized)
* **Integrations:** Telegram Bot API

## 🚀 Getting Started

### Prerequisites
* Java 21 & Maven
* A PostgreSQL database with the `pgvector` extension enabled
* API Keys for Groq, Google Gemini, and a Telegram Bot

### Environment Variables
Create an `.env` file in the root directory (or configure in your cloud provider) with the following:

```env
DB_URL=jdbc:postgresql://your_db_host:5432/postgres?sslmode=require
DB_USERNAME=postgres
DB_PASSWORD=your_password
GROQ_API_KEY=your_groq_key
GEMINI_API_KEY=your_gemini_key
TELEGRAM_BOT_TOKEN=your_telegram_bot_token
TELEGRAM_CHAT_ID=your_chat_id
WEBHOOK_URL=http://localhost:8080/api/v1/webhooks/scrape-results
