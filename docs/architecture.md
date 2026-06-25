# YearnDeployerAlerterBot — Architecture

> A Spring Boot bot that monitors Yearn Finance deployer activity on Etherscan and auto-posts alerts to Twitter. It scrapes Etherscan's API at a fixed interval, detects contract creation and token transfer events, and tweets them with deduplication via a circular FIFO queue.

| | |
|---|---|
| **Language** | Java 8 |
| **Framework** | Spring Boot 2.1.4 · Spring Data JPA · Spring Scheduling |
| **Scraping** | Jsoup + RestTemplate (Etherscan API) |
| **Concurrency** | ForkJoinPool for crawling · ThreadPoolTaskExecutor for async |
| **Twitter** | OAuth 1.0a (HMAC-SHA1 signing via TwitterOauthHeaderGenerator) |
| **Deduplication** | CircularFifoQueue (100-entry window) |
| **Database** | H2 (runtime, dev) · PostgreSQL (production) |
| **AOP** | ApplicationLoggerAspect — logs controllers and crawler service |

---

## High-Level Architecture

```mermaid
flowchart TB
  subgraph SpringBoot["Spring Boot Application"]
    MAIN["YearnDeployerAlerter<br/>@SpringBootApplication<br/>@EnableScheduling"]
    SERVICE["XSource_CrawlerService<br/>@Scheduled fixedDelay=30s"]
    CRAWLER["WebCrawler<br/>ForkJoinPool"]
    PROCESSOR["XSource_LinkProcessor<br/>RecursiveTask"]
    SENDER["EmailSender<br/>Runnable"]
    TWITTER["TwitterOauthHeaderGenerator<br/>HMAC-SHA1"]
    FIFO["CircularFifoQueue<br/>100 entries"]
  end

  subgraph External["External Services"]
    ETHERSCAN["Etherscan API<br/>api.etherscan.io"]
    TWITTER_API["Twitter API<br/>statuses/update.json"]
  end

  subgraph Async["Async Configuration"]
    FORK_EXEC["forkTaskExecutor<br/>ForkJoinPool"]
    DEFAULT_EXEC["taskExecutor<br/>ThreadPoolTaskExecutor"]
  end

  MAIN --> SERVICE
  SERVICE --> FORK_EXEC
  FORK_EXEC --> CRAWLER
  CRAWLER --> PROCESSOR
  PROCESSOR --> ETHERSCAN
  PROCESSOR --> SENDER
  SENDER --> FIFO
  SENDER --> TWITTER
  TWITTER --> TWITTER_API
  SERVICE --> DEFAULT_EXEC
```

---

## Scheduled Crawl Cycle

```mermaid
sequenceDiagram
  participant Scheduler as Spring @Scheduled
  participant Service as XSource_CrawlerService
  participant ForkExec as forkTaskExecutor
  participant Crawler as WebCrawler
  participant ForkJoin as ForkJoinPool
  participant Processor as XSource_LinkProcessor
  participant Etherscan as Etherscan API
  participant Sender as EmailSender

  Scheduler->>Service: fetchData() every 30s
  Service->>ForkExec: CompletableFuture.runAsync(startCrawler)
  ForkExec->>Crawler: WebCrawler.startScrapping()
  Crawler->>ForkJoin: invoke(RecursiveTask)
  ForkJoin->>Processor: compute()
  Processor->>Etherscan: GET api.etherscan.io/api<br/>module=account and action=txlist<br/>page=1 and offset=10 and sort=desc
  Etherscan-->>Processor: XSource_Content (transaction list)
  Processor-->>Crawler: Collection of XSource_Content
  Crawler-->>Service: transaction data
  Service->>Sender: new EmailSender(data, fifo)
  Sender->>Sender: run() — process each transaction
```

---

## Alert Decision Flow

For each transaction from Etherscan, the bot decides whether to tweet. Deduplication uses a `CircularFifoQueue` of 100 transaction hashes.

```mermaid
flowchart TD
  TX["Transaction from Etherscan<br/>txlist response"] --> DEDUP{"Transaction hash<br/>in FIFO queue?"}
  DEDUP -- yes --> SKIP["Skip — already alerted"]
  DEDUP -- no --> CLASSIFY["Classify transaction type"]

  CLASSIFY --> CONTRACT{"Contract creation?<br/>(to field is empty or<br/>creates contract)"}
  CLASSIFY --> OTHER{"Other notable activity?<br/>(token transfer, large value)"}

  CONTRACT -- yes --> CREATE_TWEET["Build tweet:<br/>$YFI #yearnfinance #YFI<br/>Possible contract creation<br/>etherscan.io/tx/hash"]
  CONTRACT -- no --> OTHER

  OTHER -- yes --> OTHER_TWEET["Build tweet:<br/>$YFI #yearnfinance #YFI<br/>Token transfer or activity<br/>etherscan.io/tx/hash"]
  OTHER -- no --> SKIP

  CREATE_TWEET --> FIFO_ADD["Add hash to FIFO"]
  OTHER_TWEET --> FIFO_ADD
  FIFO_ADD --> TWEET["Send tweet via Twitter API"]
  TWEET --> DONE["Alert posted"]
  SKIP --> DONE
```

---

## Twitter OAuth 1.0a Signing

The bot uses manual OAuth 1.0a header generation with HMAC-SHA1 signing — no Twitter SDK dependency.

```mermaid
sequenceDiagram
  participant Sender as EmailSender
  participant OAuth as TwitterOauthHeaderGenerator
  participant Twitter as Twitter API

  Sender->>OAuth: generateHeader(POST, twitter_url, params)
  OAuth->>OAuth: generate nonce (random)
  OAuth->>OAuth: generate timestamp (unix)
  OAuth->>OAuth: build signature base string<br/>method + url + sorted params
  OAuth->>OAuth: sign with HMAC-SHA1<br/>key = consumerSecret + and + tokenSecret
  OAuth->>OAuth: base64 encode signature
  OAuth-->>Sender: Authorization header<br/>OAuth oauth_consumer_key, oauth_token,<br/>oauth_signature_method, oauth_timestamp,<br/>oauth_nonce, oauth_version, oauth_signature

  Sender->>Twitter: POST statuses/update.json<br/>Authorization: OAuth header<br/>status=tweet text
  Twitter-->>Sender: 200 OK (tweet posted)
```

---

## Async Thread Pool Architecture

```mermaid
flowchart TB
  subgraph AsyncConfig["AsyncConfiguration  (@EnableAsync)"]
    DEFAULT["taskExecutor<br/>ThreadPoolTaskExecutor<br/>corePoolSize from config"]
    FORK["forkTaskExecutor<br/>ForkJoinPool<br/>forkThreadSize=10 (default)"]
  end

  subgraph Usage
    SCHEDULER["@Scheduled fetchData<br/>runs on scheduler thread"]
    CRAWL_TASK["CompletableFuture.runAsync<br/>uses forkTaskExecutor"]
    CRAWL["WebCrawler.startScrapping<br/>uses ForkJoinPool"]
  end

  SCHEDULER --> CRAWL_TASK
  CRAWL_TASK --> FORK
  FORK --> CRAWL

  subgraph Shutdown["Graceful Shutdown  (ContextClosedHandler)"]
    HANDLER["ContextClosedHandler<br/>ApplicationListener"]
    HANDLER --> WAIT["Wait up to 20s for<br/>ThreadPoolTaskScheduler shutdown"]
    HANDLER --> WAIT_EXEC["Wait up to 50 retries for<br/>ThreadPoolTaskExecutor active=0"]
    WAIT --> FORCE["shutdownNow if not terminated"]
    WAIT_EXEC --> FORCE
  end
```

---

## AOP Logging

```mermaid
flowchart LR
  subgraph Aspect["ApplicationLoggerAspect  (@Aspect)"]
    P1["@Pointcut: controllers.*.*"]
    P2["@Pointcut: services.XSource_CrawlerService.*"]
    A1["@Around: logAround<br/>log before + after method execution"]
    A2["@Around: logAroundConfig<br/>log before + after crawler service"]
  end

  P1 --> A1
  P2 --> A2
  A1 --> LOG["SLF4J Logger<br/>debug level"]
  A2 --> LOG
```

The aspect intercepts all controller methods (`com.azad.yearn.deployer.controllers.*`) and the crawler service (`XSource_CrawlerService`), logging method entry, arguments, and exit at debug level.
