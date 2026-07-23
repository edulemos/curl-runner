package com.curlrunner.repository;

import java.util.List;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import com.curlrunner.model.RequestResult;

public interface RequestResultRepository extends CrudRepository<RequestResult, Long> {

    List<RequestResult> findByRunId(Long runId);

    @Query("SELECT COUNT(*) FROM request_result WHERE run_id = :runId AND success = true")
    int countSuccessByRunId(@Param("runId") Long runId);

    @Query("SELECT COUNT(*) FROM request_result WHERE run_id = :runId AND success = false")
    int countFailuresByRunId(@Param("runId") Long runId);

    @Query("SELECT * FROM request_result WHERE run_id = :runId ORDER BY iteration_number ASC")
    List<RequestResult> findByRunIdOrderByIterationNumber(@Param("runId") Long runId);
}
