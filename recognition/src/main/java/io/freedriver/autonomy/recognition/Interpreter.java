package main.java.io.freedriver.autonomy;

import java.io.IOException;
import java.util.List;
import java.util.function.Function;

/**
 * For Some kind of
 * @param <I> Input
 *           We want to Interpret that input as some kind of
 * @param <O> AbstractConcept
 *
 */
public interface Interpreter<I, O extends AbstractConcept> extends Function<I, O> {
	List<O> interpret() throws IOException, BadInputException;
}
