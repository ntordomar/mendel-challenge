# Mendel Challenge – Transactions Service

In-memory RESTful service that stores transactions and answers two questions about them: which ids share a type, and how much a transaction plus its descendants adds up to.

Java 17, Spring Boot, Maven. No database.

## Run

The Maven wrapper is included. From the project directory:

```bash
./mvnw spring-boot:run
```

The service listens on port 8080. Run the tests with:

```bash
./mvnw test
```

Docker builds the jar and runs it on the same port:

```bash
docker compose up --build
```

Or without Compose:

```bash
docker build -t transactions .
docker run --rm -p 8080:8080 transactions
```

State lives in the process. Restarting the service, or running a second instance, starts from an empty store.

## API

| Method | Path | Success |
|---|---|---|
| `PUT` | `/transactions/{id}` | `201` the first time that id is stored, `200` when it replaces an existing one. Body is always `{"status":"ok"}`. |
| `GET` | `/transactions/types/{type}` | `200` and a JSON array of ids. An unknown type is `200` and `[]`. Order is not defined. |
| `GET` | `/transactions/sum/{id}` | `200` and `{"sum": <number>}`. The sum is the transaction plus every descendant. Ancestors are not included. |

`PUT` body:

```json
{ "amount": 5000, "type": "cars", "parent_id": 10 }
```

`amount` and `type` are required. `type` cannot be blank. `parent_id` is optional. Omitting it on a replace clears the previous parent. The id in the URL is chosen by the client.

`amount` is stored as a `BigDecimal`, so `0.1 + 0.2` is `0.3`. Zero and negative amounts are accepted. `5000` and `5000.0` are the same number; the JSON may keep the scale of the value that was stored.

## Example

```bash
curl -i -X PUT http://localhost:8080/transactions/10 \
  -H 'Content-Type: application/json' \
  -d '{"amount": 5000, "type": "cars"}'

curl -i -X PUT http://localhost:8080/transactions/11 \
  -H 'Content-Type: application/json' \
  -d '{"amount": 10000, "type": "shopping", "parent_id": 10}'

curl -i -X PUT http://localhost:8080/transactions/12 \
  -H 'Content-Type: application/json' \
  -d '{"amount": 5000, "type": "shopping", "parent_id": 11}'

curl http://localhost:8080/transactions/types/cars
curl http://localhost:8080/transactions/sum/10
curl http://localhost:8080/transactions/sum/11
```

`types/cars` returns `[10]`. `sum/10` returns `{"sum":20000}`. `sum/11` returns `{"sum":15000}`.

## Errors

Failed requests use the same JSON shape:

```json
{
  "timestamp": "2026-11-25T22:21:39.846Z",
  "status": 422,
  "error": "Unprocessable Content",
  "path": "/transactions/11",
  "message": "Parent transaction not found: 99"
}
```

| Status | When | `message` |
|---|---|---|
| `400` | `amount` is missing, or `type` is missing or blank | the field and why, for example `amount must not be null` |
| `404` | `GET /transactions/sum/{id}` and that id does not exist | `Transaction not found: {id}` |
| `422` | Business rules error: `parent_id` does not exist, the transaction is its own parent, or the new parent is one of its descendants | `Parent transaction not found: {id}` or `Transaction {id} would create a cycle` |

A rejected `PUT` does not change what was already stored. The parent must exist before the child is saved. Sending the child first returns `422`.
