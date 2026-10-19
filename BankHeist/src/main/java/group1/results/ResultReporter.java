package group1.results;

@FunctionalInterface
public interface ResultReporter {
    ResultReporter NONE = result -> { };
    void submit(HeistResult result);
}
