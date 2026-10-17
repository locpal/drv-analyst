package ru.sewaiper.derivative.analyst.service;

import org.springframework.stereotype.Service;
import ru.sewaiper.derivative.analyst.dao.AnalysisStepDao;

import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class AnalysisStepJournal {
    private final AnalysisStepDao dao;

    public AnalysisStepJournal(AnalysisStepDao dao) {
        this.dao = dao;
    }

    public <T> T run(UUID analysisId,
                     String name,
                     Object input,
                     Supplier<T> action,
                     Function<T, Object> output) {
        long stepId = dao.start(analysisId, name, input);
        try {
            T result = action.get();
            dao.complete(stepId, output.apply(result));
            return result;
        } catch (RuntimeException e) {
            dao.fail(stepId, e);
            throw e;
        }
    }

    public void run(UUID analysisId, String name, Object input, Runnable action, Object output) {
        run(analysisId, name, input, () -> {
            action.run();
            return output;
        }, Function.identity());
    }
}
