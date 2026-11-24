# Mendel Challenge – Transactions Service

RESTful service built with Spring Boot (Java 17) that stores transactions in memory and exposes information about them.

## Endpoints

| Method | Path | Description |
|---|---|---|
| `PUT` | `/transactions/{transaction_id}` | Creates or replaces a transaction. Body: `{ "amount": double, "type": string, "parent_id": long (optional) }` |
| `GET` | `/transactions/types/{type}` | Returns the ids of all transactions of the given type. |
| `GET` | `/transactions/sum/{transaction_id}` | Returns the sum of the amounts of the transaction and all its transitive descendants. |

> Work in progress.
