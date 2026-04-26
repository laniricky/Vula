# Vula Scaling & Infrastructure Roadmap

This document outlines a strategic plan for scaling Vula's backend infrastructure beyond its initial MVP (Minimum Viable Product) stage on Firebase. Once the application achieves high user adoption and the current Firebase infrastructure becomes cost-prohibitive or performance-bottlenecked, this roadmap will guide the migration to a more robust, cost-effective, and highly scalable architecture.

## 1. Triggers for Migration
You should begin transitioning from Firebase to the technologies listed below when you hit one or more of the following triggers:
* **Storage Egress Costs:** When the cost of users downloading media (stories and posts) exceeds your operational budget. Firebase Storage egress fees can grow exponentially with viral content.
* **Firestore Read/Write Costs:** When the volume of chat messages, likes, and feed loads causes unmanageable spikes in database costs.
* **Complex Relational Queries:** If you introduce features requiring complex multi-table joins, full-text search, or advanced contact-matching logic that Firestore's NoSQL model cannot efficiently support.
* **Custom Backend Logic Limitations:** When Firebase Cloud Functions become too slow (cold starts) or too restricted for heavy real-time operations.

---

## 2. Phase 1: Storage Migration (The Highest Priority)
Media storage is usually the first massive cost center for a social application.

### Proposed Architecture:
* **Primary Storage:** **Cloudflare R2**
    * *Why:* Cloudflare R2 is fully S3-compatible but uniquely offers **zero egress fees**. This means you only pay for the storage space itself, completely eliminating the bandwidth penalty when thousands of users view the same viral story or video.
* **Content Delivery Network (CDN):** **Cloudflare**
    * *Why:* Cloudflare pairs seamlessly with R2, providing global edge-caching to deliver images and videos incredibly fast regardless of the user's location.
* **Migration Strategy:**
    1. Implement a backend service to issue signed URLs for direct client uploads to R2.
    2. Write a script to incrementally sync historical media from Firebase Storage to R2.
    3. Update the Android client to upload directly to R2 and read from the new CDN URLs.

---

## 3. Phase 2: Database Migration
Moving away from Firestore requires careful planning to ensure zero data loss.

### Proposed Architecture:
* **Primary Relational Database:** **Supabase (PostgreSQL)**
    * *Why:* Supabase is an open-source Firebase alternative built on PostgreSQL. It provides the same real-time subscription capabilities (which Vula relies on for chat) but with the immense power, complex querying, and predictable pricing of a standard SQL database.
* **High-Volume Time-Series Database (Optional, for Chat at Scale):** **ScyllaDB or Cassandra**
    * *Why:* If Vula reaches the scale of WhatsApp or Discord, PostgreSQL might struggle with the sheer volume of message writes. ScyllaDB is a highly scalable, ultra-fast NoSQL database specifically designed to handle infinite time-series data like chat logs without performance degradation.
* **Caching & Real-time Pub/Sub:** **Redis**
    * *Why:* Crucial for tracking user "online" status, typing indicators, and temporarily caching heavy queries (like the Global Feed) to prevent database overload.

### Migration Strategy:
1. Re-model Vula's NoSQL schema into a relational schema (Users, Posts, Comments, Likes, Messages).
2. Set up a dual-write system: The backend writes to both Firestore and Supabase simultaneously.
3. Perform a backfill of historical data from Firestore to Supabase.
4. Flip the read-path in the Android client to consume data from Supabase.
5. Decommission Firestore.

---

## 4. Phase 3: Dedicated Backend Microservices
Moving away from serverless Firebase Cloud Functions to a dedicated application server.

### Proposed Architecture:
* **Language/Framework:** **Go (Golang)** or **Kotlin (Ktor)**
    * *Why:* Both Go and Kotlin coroutines are exceptional at handling tens of thousands of concurrent connections on relatively cheap hardware. This is essential for maintaining persistent WebSocket connections for real-time messaging.
* **Hosting:** **AWS (EKS / EC2), Google Cloud (GKE), or DigitalOcean (Kubernetes)**
    * *Why:* Containerized microservices (Docker/Kubernetes) allow you to auto-scale your backend based on real-time traffic spikes.
* **Real-time Communication:** **WebSockets (or gRPC)**
    * *Why:* Replacing Firebase's proprietary real-time SDK with standard WebSockets allows for complete control over the network layer and significantly lowers latency.

---

## Summary of the Target "At-Scale" Stack:
* **Client:** Android Native (Kotlin/Compose)
* **Backend API:** Go or Kotlin Ktor
* **Real-time Engine:** WebSockets + Redis Pub/Sub
* **Primary Database:** Supabase (PostgreSQL)
* **Chat History Database:** ScyllaDB
* **Media Storage:** Cloudflare R2
* **CDN:** Cloudflare
