# TypoScanning 2.0
A Hybrid System for Domain Squatting Detection Using Heuristic Analysis and Large Language Models.

## Description
This project is a demonstrative version of a system developed as part of a master's thesis. The main objective of the system is to automatically detect domain-squatting domains and assess the level of risk associated with them. The proposed solution uses a hybrid approach, combining heuristic analysis based on predefined rules with an artificial intelligence module to interpret the results and determine potential malicious intent.

## Key Features
* **Automated Variant Generation:** Generates potential squatting variants for an input domain, including wrong TLDs, combosquatting, and typos.
* **Parallel Infrastructure Scanning:** Collects technical information asynchronously by checking DNS records (A, AAAA, MX), HTTP/HTTPS reachability, redirects, and TLS certificates.
* **Heuristic Scoring Engine:** Assigns a numerical risk score based on extracted lexical and infrastructural features, categorizing domains into LOW, MEDIUM, or HIGH risk levels.
* **LLM Integration (Gemini 2.5 Flash):** Analyzes the collected technical context to assess the likely intent of the domain, classify the threat (e.g., `PHISHING_LIKE`), and generate an analytical justification with recommended actions.

## Tech Stack
* **Frontend:** Next.js, React, TypeScript
* **Backend:** Java Spring Boot
* **Database:** PostgreSQL

## Future Roadmap
Planned features for future development include:
* **User Accounts & Monitoring:** Registration, personalized analysis history, and continuous/periodic domain monitoring.
* **Advanced Web Analysis:** Integration with an automated browser environment (Playwright) to detect hidden phishing content and automatically capture screenshots.
* **Extended Variant Generation:** Support for soundsquatting, bitsquatting, and IDN homographs.
* **System Scaling:** Implementation of a message broker (RabbitMQ/Kafka) for stable background task queuing and processing.
* **Advanced Reporting:** PDF report generation and data export in Threat Intelligence formats (MISP, STIX/TAXII).

---
**Author**: Julia Chilczuk (Warsaw University of Technology)