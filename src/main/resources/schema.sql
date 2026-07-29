CREATE TABLE execution_run (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    curl_command TEXT NOT NULL,
    total_iterations INT NOT NULL,
    delay_ms INT DEFAULT 0,
    parallel BOOLEAN DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    success_count INT DEFAULT 0,
    failure_count INT DEFAULT 0,
    avg_response_time_ms BIGINT DEFAULT 0
);

CREATE TABLE request_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    run_id BIGINT NOT NULL,
    iteration_number INT NOT NULL,
    status_code INT,
    response_time_ms BIGINT NOT NULL,
    success BOOLEAN NOT NULL,
    error_message TEXT,
    response_size_bytes INT,
    executed_at TIMESTAMP NOT NULL,
    response_body CLOB,
    FOREIGN KEY (run_id) REFERENCES execution_run(id)
);
