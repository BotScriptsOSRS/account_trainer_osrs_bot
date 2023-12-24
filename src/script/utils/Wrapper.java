package script.utils;

public class Wrapper<k> {
    private k item;

    public Wrapper() { this(null); }

    public Wrapper(k item) {
        this.item = item;
    }

    public k get() { return item; }

    public void set(k item) { this.item = item; }

    public boolean isSet() { return this.item != null; }
}
