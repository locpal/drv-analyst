package ru.sewaiper.derivative.analyst.domain.feature;

public class FeatureContext {

    private Meta meta;
    private Instrument instrument;

    public Meta getMeta() {
        return meta;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public Instrument getInstrument() {
        return instrument;
    }

    public void setInstrument(Instrument instrument) {
        this.instrument = instrument;
    }
}
