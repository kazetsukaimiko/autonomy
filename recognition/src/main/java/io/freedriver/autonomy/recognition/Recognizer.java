package main.java.io.freedriver.autonomy;

public interface Recognizer<I, O extends AbstractConcept<I>, C extends ConcreteInstance<I, O>> {
	C recognize(O concept);
}
