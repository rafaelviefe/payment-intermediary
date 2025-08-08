# Rinha de Backend 2025 - Submission

## About the Project

I am Rafael Vieira Ferreira, an Information Systems student and intern, and I joined the Rinha de Backend 2025 mainly to learn and experiment with new technologies.

The main idea was to build a payment intermediary with a **custom load balancer** written in Go, designed to be extremely lightweight and focused on one thing: ensuring the highest possible *fire-and-forget* approach to reduce average response time.

In this second submission, I decided to use **only one processor**: the **default** one, which charges a lower fee. This simplified the architecture and removed the need for logic to distribute between multiple processors.

On the backend side, I used **reactive programming** and **Java 21 virtual threads** to optimize resource usage, as well as **GraalVM** to generate native binaries. **Redis** was used as both storage and messaging system, along with **Kryo** for serialization and **ShedLock** for scheduled task control.

---

## Technologies Used

* **Language:** Java 21 + Go
* **Framework:** Spring Boot + WebFlux
* **Load Balancer:** Go (developed by me)
* **Storage:** Redis
* **Messaging:** Redis
* **Serialization:** Kryo
* **Scheduling:** ShedLock
* **Native Build:** GraalVM

---

## Source Code Repository

[https://github.com/rafaelviefe/payment-intermediary](https://github.com/rafaelviefe/payment-intermediary)

---

## Running

The `docker-compose.yml` is set up to start:

* **Redis**
* **API** (default processor)
* **Load Balancer**

Just run:

```bash
docker compose up --build
```

The service will be available on port **9999**.

---

## Notes

The focus of this project was not just performance, but also learning: from writing a load balancer from scratch to using virtual threads and native binaries with GraalVM. It was a great exercise in integrating technologies and optimizing resources.
