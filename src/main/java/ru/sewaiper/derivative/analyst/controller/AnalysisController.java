package ru.sewaiper.derivative.analyst.controller;

import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisRun;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisStep;
import ru.sewaiper.derivative.analyst.domain.analysis.LlmExchange;
import ru.sewaiper.derivative.analyst.domain.analysis.AnalysisStatus;
import ru.sewaiper.derivative.analyst.domain.analysis.StartAnalysisRequest;
import ru.sewaiper.derivative.analyst.domain.analysis.StartAnalysisResponse;
import ru.sewaiper.derivative.analyst.domain.market.enums.AssetType;
import ru.sewaiper.derivative.analyst.service.AnalysisRunService;

import java.net.URI;
import java.util.UUID;
import java.util.List;

@RestController
@RequestMapping("/api/v1/analyses")
public class AnalysisController {
    private final AnalysisRunService service;

    public AnalysisController(AnalysisRunService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<@NonNull StartAnalysisResponse> start(@RequestBody StartAnalysisRequest request) {
        if (request == null || request.providerId() == null || request.providerId().isBlank()
                || request.assetType() == null
                || !(request.assetType() == AssetType.FUTURE
                     || request.assetType() == AssetType.SHARE
                     || request.assetType() == AssetType.BOND)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "providerId and supported assetType are required");
        }
        UUID id = service.start(request);
        AnalysisStatus status = service.find(id).orElseThrow().status();
        return ResponseEntity.accepted()
                .location(URI.create("/api/v1/analyses/" + id))
                .body(new StartAnalysisResponse(id, status));
    }

    @GetMapping("/{id}")
    public AnalysisRun get(@PathVariable UUID id) {
        return service.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Analysis not found: " + id));
    }

    @GetMapping("/{id}/steps")
    public List<AnalysisStep> steps(@PathVariable UUID id) {
        service.find(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found: " + id));
        return service.steps(id);
    }

    @GetMapping("/{id}/forecast")
    public LlmExchange forecast(@PathVariable UUID id) {
        service.find(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found: " + id));
        return service.forecast(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Forecast not available: " + id));
    }
}
