# Curl Runner

A desktop application to execute cURL commands repeatedly with control over iterations, delay, and parallelism. Similar to Postman's Collection Runner and Bruno's Runner.

## What is it for?

Curl Runner lets you stress-test or batch-test HTTP endpoints by executing the same cURL command multiple times, with control over:

- **Number of iterations** — how many times the request will be executed (1 to 10,000)
- **Delay between requests** — interval in milliseconds between each execution (0 to 60,000ms)
- **Parallel or sequential mode** — execute one at a time or all simultaneously

After execution, the application displays a full summary with performance metrics: average response time, percentiles (P50, P95, P99), requests per second, status code distribution, and success/failure counts.

## How to use

1. **Paste your cURL command** in the text area
2. **Configure the execution**:
   - Set the number of iterations
   - Set the delay between requests (0 for no pause)
   - Check "Parallel" for simultaneous execution
3. **Click "Run"**
4. **Monitor progress** via the progress bar
5. **Analyze results** in the summary panel and results table
6. **Click on a row** to view the full response body (formatted as JSON when applicable)
7. **Click "Export results"** to save the completed execution as a minified JSON file. For every iteration, it contains the request actually sent (method, URL, headers, and body), response, HTTP status, and elapsed time. JSON request and response bodies are exported as JSON objects rather than escaped text.

To cancel a running execution, click "Cancel".

## Data-Driven Execution (JSON Variables)

You can load a JSON file with a list of data objects and use `{{variable}}` placeholders in the cURL command. Each item in the list generates one execution with the corresponding values substituted.

### Example

cURL command:
```
curl --request POST --url https://api.example.com/users --header 'Content-Type: application/json' --data '{"name": "{{name}}", "email": "{{email}}"}'
```

JSON file (`data.json`):
```json
[
  {"name": "Carlos", "email": "carlos@email.com"},
  {"name": "Daniel", "email": "daniel@email.com"},
  {"name": "Maria", "email": "maria@email.com"}
]
```

**Result:** 3 executions, each replacing `{{name}}` and `{{email}}` with the corresponding values from each object.

### How to use data-driven mode

1. Write the cURL command using `{{variable}}` placeholders where you want dynamic data
2. Click **"Load JSON"** and select your data file
3. The number of iterations will be automatically set to the list size
4. Click **"Run"**

To go back to manual iteration mode, click **"Clear"**.

### JSON file format

The file must contain a JSON array of objects:

```json
[
  {"field1": "value1", "field2": 123},
  {"field1": "value2", "field2": 456}
]
```

Variables can be used in:
- URL (`--url https://api.example.com/{{path}}`)
- Request body (`--data '{"key": "{{value}}"}'`)
- Headers (`--header 'Authorization: Bearer {{token}}'`)
- Basic auth (`-u {{user}}:{{pass}}`)

## Supported cURL flags

- `-X` / `--request` — HTTP method (GET, POST, PUT, DELETE, PATCH)
- `--url` — target URL
- `-H` / `--header` — request headers
- `-d` / `--data` / `--data-raw` — request body
- `-u` / `--user` — basic authentication (user:password)
- Bare URLs (passed directly after `curl`)

Commands copied from browsers or tools with `\` line continuations are also supported.

## Running the application

Requirements: Java 17 or higher.

```bash
mvn spring-boot:run
```

Or build the JAR and run it:

```bash
mvn clean package -DskipTests
java -jar target/curl-runner-0.1.0.jar
```

## Notes

- Execution data is stored in-memory only. When the application is closed, all history is lost.
- Commands containing shell injection characters (pipes, redirections, backticks) are rejected for security.
- In parallel mode, the thread pool is limited to the number of available processors.
- The response body is stored for each iteration and can be viewed by selecting a row in the results table.
