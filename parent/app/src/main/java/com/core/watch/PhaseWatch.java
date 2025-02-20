package com.core.watch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@Builder
public class PhaseWatch {

    private String name;
    private StopWatch watch;
    private List<PhaseWatch> phases;
    private PhaseWatch nextPhase;

    public PhaseWatch(String name) {

        this.name = name;
        phases = new ArrayList<>();
        watch = new StopWatch();
    }

    private PhaseWatch(String name, StopWatch watch) {
        this.name = name;
        phases = new ArrayList<>();
        this.watch = watch;
    }

    public void start() {

        watch.start(name);
    }

    public void stop() {

        watch.stop();
    }

    public PhaseWatch split(String name) {

        PhaseWatch split = new PhaseWatch(name, watch);
        phases.add(split);
        return split;
    }

    public PhaseWatch next(String name) {

        nextPhase = new PhaseWatch(name);
        nextPhase.start();
        return nextPhase;
    }

    public long getTimeInMilis() {

        return watch.getLastTaskInfo().getTimeMillis();
    }

}
