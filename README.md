# checkout-demo (Maven)

A realistic “legacy” Java checkout service used to demonstrate a Codex enablement workflow:
**Understand → Risk-map → Safe refactor → Generate tests**.

## Quickstart
Requires Java 17.

Run tests:
```bash
mvn test

mvn -q -DskipTests package
java -jar target/checkout-demo-1.0.0.jar
