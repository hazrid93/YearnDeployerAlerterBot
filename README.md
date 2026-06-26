# YearnDeployerAlerterBor
Bot to alert of yearn deployer activity on etherscan to twitter at: https://twitter.com/yearndeployera1

📎 **[Architecture & Sequence Diagrams](docs/architecture.md)**

📖 **[DeepWiki](https://app.devin.ai/org/isaiya-9bf81eafd4d3/wiki/hazrid93/YearnDeployerAlerterBot?branch=main)**


[![zread](https://img.shields.io/badge/Ask_Zread-_.svg?style=for-the-badge&color=00b0aa&labelColor=000000&logo=data%3Aimage%2Fsvg%2Bxml%3Bbase64%2CPHN2ZyB3aWR0aD0iMTYiIGhlaWdodD0iMTYiIHZpZXdCb3g9IjAgMCAxNiAxNiIgZmlsbD0ibm9uZSIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj4KPHBhdGggZD0iTTQuOTYxNTYgMS42MDAxSDIuMjQxNTZDMS44ODgxIDEuNjAwMSAxLjYwMTU2IDEuODg2NjQgMS42MDE1NiAyLjI0MDFWNC45NjAxQzEuNjAxNTYgNS4zMTM1NiAxLjg4ODEgNS42MDAxIDIuMjQxNTYgNS42MDAxSDQuOTYxNTZDNS4zMTUwMiA1LjYwMDEgNS42MDE1NiA1LjMxMzU2IDUuNjAxNTYgNC45NjAxVjIuMjQwMUM1LjYwMTU2IDEuODg2NjQgNS4zMTUwMiAxLjYwMDEgNC45NjE1NiAxLjYwMDFaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00Ljk2MTU2IDEwLjM5OTlIMi4yNDE1NkMxLjg4ODEgMTAuMzk5OSAxLjYwMTU2IDEwLjY4NjQgMS42MDE1NiAxMS4wMzk5VjEzLjc1OTlDMS42MDE1NiAxNC4xMTM0IDEuODg4MSAxNC4zOTk5IDIuMjQxNTYgMTQuMzk5OUg0Ljk2MTU2QzUuMzE1MDIgMTQuMzk5OSA1LjYwMTU2IDE0LjExMzQgNS42MDE1NiAxMy43NTk5VjExLjAzOTlDNS42MDE1NiAxMC42ODY0IDUuMzE1MDIgMTAuMzk5OSA0Ljk2MTU2IDEwLjM5OTlaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik0xMy43NTg0IDEuNjAwMUgxMS4wMzg0QzEwLjY4NSAxLjYwMDEgMTAuMzk4NCAxLjg4NjY0IDEwLjM5ODQgMi4yNDAxVjQuOTYwMUMxMC4zOTg0IDUuMzEzNTYgMTAuNjg1IDUuNjAwMSAxMS4wMzg0IDUuNjAwMUgxMy43NTg0QzE0LjExMTkgNS42MDAxIDE0LjM5ODQgNS4zMTM1NiAxNC4zOTg0IDQuOTYwMVYyLjI0MDFDMTQuMzk4NCAxLjg4NjY0IDE0LjExMTkgMS42MDAxIDEzLjc1ODQgMS42MDAxWiIgZmlsbD0iI2ZmZiIvPgo8cGF0aCBkPSJNNCAxMkwxMiA0TDQgMTJaIiBmaWxsPSIjZmZmIi8%2BCjxwYXRoIGQ9Ik00IDEyTDEyIDQiIHN0cm9rZT0iI2ZmZiIgc3Ryb2tlLXdpZHRoPSIxLjUiIHN0cm9rZS1saW5lY2FwPSJyb3VuZCIvPgo8L3N2Zz4K&logoColor=ffffff)](https://zread.ai/hazrid93/YearnDeployerAlerterBot)

[![Codacy Badge](https://app.codacy.com/project/badge/Grade/6c34db8061d24427831d6d7a72b654dd)](https://www.codacy.com/gh/hazrid93/YearnDeployerAlerterBot/dashboard?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=hazrid93/YearnDeployerAlerterBot&amp;utm_campaign=Badge_Grade)


---

## Key Features & Highlights

A Spring Boot 2.1 bot (Java 8) that polls the Etherscan API for transactions originating from a specific Yearn deployer address, classifies each transaction, and auto-posts alerts to Twitter with hand-rolled OAuth 1.0a signing — all scheduled and concurrent via a ForkJoinPool-backed executor. Below are the notable engineering details read directly from the source.

- **Etherscan account-transaction polling with deserialized model**
  The crawler queries Etherscan's `module=account&action=txlist` endpoint, built in `XSource_CrawlerService.startCrawler()` as `XSource_Constants.XSource_SEARCH_URI + "&address=" + deployer + "&apikey=" + apikey`, where `XSource_SEARCH_URI = "https://api.etherscan.io/api?module=account&action=txlist"`. The response is deserialized via Spring's `RestTemplate.getForEntity()` into `XSource_Content`, a `@JsonIgnoreProperties(ignoreUnknown = true)` POJO whose `result` is a `List<Map<String,String>>` — a flexible schema that tolerates Etherscan's loosely-typed JSON without a brittle per-field binding.

- **ForkJoinPool-based recursive scraping**
  `WebCrawler` constructs a `ForkJoinPool(threadCount)` (default 10, from `application.async.fork-thread-size`) and invokes a `XSource_LinkProcessor extends RecursiveTask<XSource_Content>`. The processor appends pagination params (`&page=1&offset=10&sort=desc`) and fetches via `RestTemplate`. Notable: the `RecursiveTask` machinery is in place for fork/join fan-out (the loop over `XSource_MAX_SCRAP_PAGE` pages is commented out but the architecture supports parallel multi-page scraping without code redesign).

- **Fixed-delay Spring scheduling**
  Scrape cycles are driven by `@Scheduled(fixedDelay = XSource_Constants.XSource_MAX_SCRAP_DELAY)` where `XSource_MAX_SCRAP_DELAY = 30000` ms. The whole job is wrapped in `CompletableFuture.runAsync(..., forkExecutor).exceptionally(e -> { log.error(...); return null; })`, so each poll runs off the scheduler thread and is fault-isolated — a thrown exception kills that future, logs, and returns `null` rather than crashing the scheduled loop. This is a clean, non-blocking scheduling pattern.

- **Circular-FIFO deduplication of alerts**
  `XSource_CrawlerService` holds `private Queue<String> fifo = new CircularFifoQueue<String>(100)` (Apache Commons Collections 4). In `EmailSender.run()`, before sending, it checks `!fifo.contains(data.get("hash"))`; on a successful tweet it `fifo.add(data.get("hash"))`. The bounded ring buffer (`CircularFifoQueue`) silently evicts the oldest entry once 100 hashes are stored, so dedup memory is O(1) and self-rotating — no manual cleanup or unbounded growth, a genuinely elegant dedup choice for a polling bot.

- **Transaction classification: contract-creation vs. token-transfer**
  Two deciders in `EmailSender` implement real heuristics. `decidingLogicContractCreation()` returns true iff `data.get("from").equalsIgnoreCase("0x2d407ddb06311396fe14d4b49da5f0471447d45c") && !data.get("contractAddress").isEmpty() && data.get("to").isEmpty()` — matching the Yearn deployer address hardcoded in both code and `application-dev.properties` (`application.etherscan.deployer=0x2d407ddb06311396fe14d4b49da5f0471447d45c`) and detecting contract deployments (non-empty `contractAddress`, empty `to`). `decidingLogicOther()` flags non-contract-creation transfers (`contractAddress` empty AND `to` non-empty). The deployer address is thus config-driven and also defensively hardcompared in the decider.

- **Hand-rolled Twitter OAuth 1.0a signing (no third-party Twitter SDK)**
  Rather than pulling in twitter4j or similar, the bot implements the full OAuth 1.0a signing chain itself in `TwitterOauthHeaderGenerator`. `generateSignatureBaseString()` assembles the percent-encoded, alphabetically-sorted parameter string (collected into a `LinkedHashMap` via `Map.Entry.comparingByKey()`), concatenates `HTTP_METHOD & encoded(url) & encoded(base)`, then `encryptUsingHmacSHA1()` computes the HMAC-SHA1 MAC with the `consumerSecret & tokenSecret` composite key and Base64-encodes the result. The `encode()` method is a careful RFC 3986 implementation that re-maps `*`→`%2A`, `+`→`%20`, and `%7E`→`~`. A cryptographically-random 10-char nonce is generated and a rounded epoch-seconds timestamp rounds out the header. This is a from-scratch, spec-correct OAuth implementation — impressive dependency discipline.

- **Direct Twitter statuses/update POST with multipart form body**
  `EmailSender.sendTweet()` posts to `https://api.twitter.com/1.1/statuses/update.json` via `RestTemplate.postForEntity()`, attaching the generated `Authorization: OAuth ...` header and a `MULTIPART_FORM_DATA` body containing a single `status` field carrying the alert text. The tweet body is composed inline in `run()`: `"$YFI #yearnfinance #YFI @iearnfinance \n ### Possible contract creation event by Yearn deployer at: https://etherscan.io/tx/" + data.get("hash")` (contract case) or `"### Token transferred event to/from Yearn deployer at: https://etherscan.io/tx/" + hash` (transfer case) — each tweet carries DeFi-relevant cashtags/hashtags and a deep-link back to the Etherscan transaction.

- **Dual notification channel architecture (Twitter + email)**
  `EmailSender` is aptly named but actually does double duty as the Twitter poster, with a dormant `sendEmail()` path using `javax.mail` (mail 1.4). The email util builds a `MimeMessage` over SMTP (outlook host `smtp-mail.outlook.com:587`, STARTTLS enabled, `mail.smtp.auth=true`) with `Authenticator` password authentication and subject `"[IMPORTANT] Yearn Deployer Notification"`. It's commented out of the main `run()` flow with a note that enabling it would "require a separate fifo handling" — a thoughtful acknowledgement that each channel deserves its own dedup queue. The infrastructure is live and pluggable.

- **Tiered async executor configuration with separate ForkJoinPool bean**
  `AsyncConfiguration.@EnableAsync` defines two executor beans: a default `ThreadPoolTaskExecutor` (`taskExecutor`) wired from `application.async.core-pool-size`/`max-pool-size`/`queue-capacity`, and a dedicated `forkTaskExecutor` constructed explicitly as `new ForkJoinPool(asyncProperties.getForkThreadSize())`. The crawler runs on `forkExecutor` while the `EmailSender` is dispatched on `defaultExecutor.execute(...)`, cleanly separating the fetch stage from the notify stage across disjoint pools — preventing one stage's backpressure from starving the other.

- **Graceful shutdown with retry-bounded executor drain**
  `ContextClosedHandler` listens for `ContextClosedEvent` and walks every `ThreadPoolTaskExecutor` bean: for up to 50 iterations it `Thread.sleep(1000)`s while `executor.getActiveCount() > 0`, logging each poll, then calls `executor.shutdown()`; if the retry count is exceeded it logs "will be killed immediately" and force-shuts. Task schedulers are drained via `awaitTermination(20000, TimeUnit.MILLISECONDS)` with a fallback `shutdownNow()`. This gives in-flight tweet/fetch work a bounded window to complete on app shutdown (relevant for the polling loop) rather than hard-killing mid-HTTP-call.

- **AOP logging aspect around scheduler and controllers**
  `ApplicationLoggerAspect` (Spring AOP / AspectJ, `@Aspect @Component`) declares two pointcuts: `execution(* com.azad.yearn.deployer.controllers.*.*(..))` and `execution(* com.azad.yearn.deployer.services.*XSource_CrawlerService.*(..))`, with `@Around` advice that logs method signatures + arguments before and after `jp.proceed()`. It's a non-invasive cross-cutting concern that keeps the crawler service's business logic free of boilerplate tracing — observability without clutter.
