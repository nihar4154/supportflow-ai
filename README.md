# SupportFlow AI

AI-assisted customer support for payment and refund queries, built with Java, Spring Boot, and Spring AI.

SupportFlow AI is designed to read support emails, investigate customer issues using business data, and prepare clear, accurate replies. Actions such as refunds follow business rules and approval requirements, with a record of what was decided and done.

> **In development:** the architecture and roadmap are defined. Application implementation is the next step.

## What it will do

The first release will focus on three common requests:

| Request | Response |
| --- | --- |
| “I was charged twice.” | Check payment records, identify duplicate charges, and propose an eligible refund. |
| “Where is my refund?” | Look up the refund status and explain its progress. |
| “I want a refund.” | Review the applicable policy, check eligibility, and request approval when needed. |

When information is missing or a case needs human judgment, the agent will ask for clarification or escalate it.

## How it works

1. **Receive** a customer email.
2. **Investigate** using customer, order, and payment records.
3. **Consult** relevant support policies and include supporting references.
4. **Resolve** the request or propose an action for approval.
5. **Reply** to the customer and record the outcome.

The planned architecture separates the AI workflow from business operations:

```mermaid
flowchart LR
    Inbox[Support inbox] --> Agent[Support agent]
    Agent <--> LLM[Language model]
    Agent <-->|MCP| Server[Support MCP server]
    Server <--> DB[(Business database)]
    Agent --> Reply[Customer reply]
```

**`support-agent`** will handle email processing, model interaction, policy retrieval, and reply delivery.

**`support-mcp-server`** will expose business operations through the Model Context Protocol (MCP). Its services will enforce access controls, refund rules, and duplicate-action checks.

The model will propose actions; application code will determine whether they are allowed. Saved workflow progress will let failed steps resume without repeating completed refunds.

## Technology

| Area | Planned stack |
| --- | --- |
| Application | Java, Spring Boot, Gradle |
| AI integration | Spring AI, MCP, retrieval-augmented generation (RAG) |
| Data | MySQL schema and sample data; Spring Data JPA and migrations planned |
| Local environment | Docker Compose, Mailpit |
| Testing and monitoring | JUnit, Testcontainers, Spring Boot Actuator, Micrometer |

## Project structure

The applications currently have separate Gradle builds and will run independently:

```text
supportflow-ai/
├── support-agent/        # Email processing and AI workflow
├── support-mcp-server/   # Business services and MCP tools
├── demo/                 # Sample emails and repeatable scenarios
├── docs/                 # Architecture and setup guides
├── support-agent/build.gradle.kts
└── support-mcp-server/compose.yaml  # Local MySQL
```

Java packages will use the root `io.github.nihar4154.supportflow`, with `agent` and `mcp` branches. Within each application, code will be grouped by feature, such as `email`, `payment`, and `refund`.

## Database relationships

The [schema](support-mcp-server/db/init/01-schema.sql) and [sample data](support-mcp-server/db/init/02-seed.sql) define the `supportflow` MySQL database. The sample records cover repeat product issues and a duplicate charge. These scripts run in filename order when MySQL initializes a **new** data volume; they do not rerun on every application start.

To start the database locally, run `docker compose -f support-mcp-server/compose.yaml up -d` from the repository root. MySQL is exposed on `localhost:3306`. The application does not connect to these tables yet; data access is part of the next implementation milestone.

```mermaid
erDiagram
    customers ||--o{ orders : places
    customers ||--o{ support_tickets : opens
    orders ||--o{ order_items : contains
    products ||--o{ order_items : appears_in
    orders ||--o{ payments : has
    orders ||--o{ refunds : has
    payments o|--o{ refunds : linked_to
    orders o|--o{ support_tickets : concerns
    products o|--o{ support_tickets : concerns
```

Each order belongs to one customer. `order_items` links orders to products and records the quantity and price paid. An order can have several payment attempts or captured charges, which lets the demo represent a duplicate charge. Each refund belongs to an order and may reference a specific payment. Every support ticket belongs to a customer; its order and product references are optional.

Foreign keys ensure referenced rows exist. Business rules still need application checks: a refund's payment must belong to the same order, the refundable amount must be valid, and a ticket's order must belong to its customer. These checks are not implemented yet.

## Roadmap

- [x] Define the project scope and architecture.
- [ ] Set up both applications, the database, and local email delivery.
- [ ] Complete an email-to-reply workflow using read-only MCP tools.
- [ ] Add policy retrieval with supporting references.
- [ ] Introduce refund approvals and simulated refund execution.
- [ ] Add persistent workflows, retries, and protection against duplicate actions.
- [ ] Publish automated evaluations, monitoring, and a reproducible demo.

## Demo and evaluation

The demo will use synthetic customer data, Mailpit for email, and simulated payments. It will cover successful resolutions, missing information, unauthorized requests, and recovery after a failure.

Evaluation will measure whether decisions are correct, replies are supported by evidence, and actions respect permissions. Latency and model usage will also be recorded.

Setup instructions will be added with the first runnable version.
