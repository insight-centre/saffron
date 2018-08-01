package org.insightcentre.nlp.saffron.term;

/**
 * A lazy loaded variable 
 * @author John McCrae
 */
public abstract class Lazy<X> {

    private X x;

    public X get() {
        if (x == null) {
            x = init();
        }
        return x;
    }

    protected abstract X init();

}
