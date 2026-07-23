package com.curlrunner.repository;

import org.springframework.data.repository.CrudRepository;
import com.curlrunner.model.ExecutionRun;

public interface ExecutionRunRepository extends CrudRepository<ExecutionRun, Long> {
}
