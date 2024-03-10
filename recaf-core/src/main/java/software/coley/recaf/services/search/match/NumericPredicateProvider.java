package software.coley.recaf.services.search.match;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static software.coley.recaf.util.NumberUtil.cmp;

/**
 * Provider of {@link NumberPredicate} instances.
 *
 * @author Matt Coley
 */
@ApplicationScoped
public class NumericPredicateProvider {
	private final Map<String, BiNumberMatcher> biNumberMatchers = new ConcurrentHashMap<>();
	private final Map<String, RangeNumberMatcher> rangeNumberMatchers = new ConcurrentHashMap<>();
	private final Map<String, MultiNumberMatcher> multiNumberMatchers = new ConcurrentHashMap<>();

	@Inject
	public NumericPredicateProvider() {
		registerBiMatcher("equal", (key, value) -> cmp(key, value) == 0);
		registerBiMatcher("not", (key, value) -> cmp(key, value) != 0);
		registerBiMatcher("gt", (key, value) -> cmp(key, value) < 0);
		registerBiMatcher("gte", (key, value) -> cmp(key, value) <= 0);
		registerBiMatcher("lt", (key, value) -> cmp(key, value) > 0);
		registerBiMatcher("lte", (key, value) -> cmp(key, value) >= 0);
		registerRangeMatcher("gt-lt", (lower, upper, value) -> cmp(lower, value) < 0 && cmp(upper, value) > 0);
		registerRangeMatcher("gte-lt", (lower, upper, value) -> cmp(lower, value) <= 0 && cmp(upper, value) > 0);
		registerRangeMatcher("gt-lte", (lower, upper, value) -> cmp(lower, value) < 0 && cmp(upper, value) >= 0);
		registerRangeMatcher("gte-lte", (lower, upper, value) -> cmp(lower, value) <= 0 && cmp(upper, value) >= 0);
		registerMultiMatcher("any-of", (keys, value) -> {
			for (Number key : keys)
				if (cmp(key, value) == 0)
					return true;
			return false;
		});
	}

	/**
	 * @param id
	 * 		Unique ID to register with.
	 * @param matcher
	 * 		Matcher implementation.
	 *
	 * @return {@code true} on success. {@code false} if the ID is already in-use.
	 */
	public boolean registerBiMatcher(@Nonnull String id, @Nonnull BiNumberMatcher matcher) {
		return biNumberMatchers.putIfAbsent(id, matcher) == null;
	}

	/**
	 * @param id
	 * 		Unique ID to register with.
	 * @param matcher
	 * 		Matcher implementation.
	 *
	 * @return {@code true} on success. {@code false} if the ID is already in-use.
	 */
	public boolean registerMultiMatcher(@Nonnull String id, @Nonnull MultiNumberMatcher matcher) {
		return multiNumberMatchers.putIfAbsent(id, matcher) == null;
	}

	/**
	 * @param id
	 * 		Unique ID to register with.
	 * @param matcher
	 * 		Matcher implementation.
	 *
	 * @return {@code true} on success. {@code false} if the ID is already in-use.
	 */
	public boolean registerRangeMatcher(@Nonnull String id, @Nonnull RangeNumberMatcher matcher) {
		return rangeNumberMatchers.putIfAbsent(id, matcher) == null;
	}

	/**
	 * @param numbers
	 * 		Array of numbers to match.
	 *
	 * @return Predicate to target the given numbers.
	 */
	@Nonnull
	public NumberPredicate newAnyOfPredicate(@Nonnull Number... numbers) {
		List<Number> numbersList = List.of(numbers);
		return newAnyOfPredicate(numbersList);
	}

	/**
	 * @param numbers
	 * 		Collection of numbers to match.
	 *
	 * @return Predicate to target the given numbers.
	 */
	@Nonnull
	public NumberPredicate newAnyOfPredicate(@Nonnull Collection<Number> numbers) {
		return Objects.requireNonNull(newMultiNumberPredicate("any-of", numbers));
	}

	/**
	 * @param lower
	 * 		Lower inclusive bound to match.
	 * @param upper
	 * 		Upper inclusive bound to match.
	 *
	 * @return Predicate to target the numbers in the given range.
	 */
	@Nonnull
	public NumberPredicate newRangePredicate(@Nonnull Number lower, @Nonnull Number upper) {
		return newRangePredicate(lower, upper, true, true);
	}

	/**
	 * @param lower
	 * 		Lower bound to match.
	 * @param upper
	 * 		Upper bound to match.
	 * @param inclusiveLower
	 *        {@code true} to make the lower bound inclusive.
	 * @param inclusiveUpper
	 *        {@code true} to make the upper bound inclusive.
	 *
	 * @return Predicate to target the numbers in the given range.
	 */
	@Nonnull
	public NumberPredicate newRangePredicate(@Nonnull Number lower, @Nonnull Number upper,
											 boolean inclusiveLower, boolean inclusiveUpper) {
		String id = (inclusiveLower ? "gte" : "gt") + "-" + (inclusiveUpper ? "lte" : "lt");
		return Objects.requireNonNull(newRangeNumberPredicate(id, lower, upper));
	}

	/**
	 * @param key
	 * 		Number to match against.
	 *
	 * @return Predicate to target the given number.
	 */
	@Nonnull
	public NumberPredicate newEqualsPredicate(@Nonnull Number key) {
		return Objects.requireNonNull(newBiNumberPredicate("equal", key));
	}

	/**
	 * @param key
	 * 		Number to match against.
	 *
	 * @return Predicate to target anything but the given number.
	 */
	@Nonnull
	public NumberPredicate newNotEqualsPredicate(@Nonnull Number key) {
		return Objects.requireNonNull(newBiNumberPredicate("not", key));
	}

	/**
	 * @param key
	 * 		Number to match against.
	 *
	 * @return Predicate to target any number greater than the given number.
	 */
	@Nonnull
	public NumberPredicate newGreaterThanPredicate(@Nonnull Number key) {
		return Objects.requireNonNull(newBiNumberPredicate("gt", key));
	}

	/**
	 * @param key
	 * 		Number to match against.
	 *
	 * @return Predicate to target any number greater than or equal to the given number.
	 */
	@Nonnull
	public NumberPredicate newGreaterThanOrEqualPredicate(@Nonnull Number key) {
		return Objects.requireNonNull(newBiNumberPredicate("gte", key));
	}

	/**
	 * @param key
	 * 		Number to match against.
	 *
	 * @return Predicate to target any number less than the given number.
	 */
	@Nonnull
	public NumberPredicate newLessThanPredicate(@Nonnull Number key) {
		return Objects.requireNonNull(newBiNumberPredicate("lt", key));
	}

	/**
	 * @param key
	 * 		Number to match against.
	 *
	 * @return Predicate to target any number less than or equal to the given number.
	 */
	@Nonnull
	public NumberPredicate newLessThanOrEqualPredicate(@Nonnull Number key) {
		return Objects.requireNonNull(newBiNumberPredicate("lte", key));
	}

	/**
	 * @param id
	 * 		Matcher unique ID.
	 * @param key
	 * 		Number to match against.
	 *
	 * @return Predicate to target the given number.
	 *
	 * @throws NoSuchElementException
	 * 		When no matcher implementation is registered with the given ID.
	 */
	@Nullable
	public NumberPredicate newBiNumberPredicate(@Nonnull String id, @Nonnull Number key) throws NoSuchElementException {
		BiNumberMatcher matcher = biNumberMatchers.get(id);
		if (matcher != null)
			return new NumberPredicate(id, target -> matcher.test(key, target));
		throw new NoSuchElementException("No such single-parameter matcher: " + id);
	}

	/**
	 * @param id
	 * 		Matcher unique ID.
	 * @param lower
	 * 		Lower bound number to match against.
	 * @param upper
	 * 		Upper bound number to match against.
	 *
	 * @return Predicate to target the numbers in the given range.
	 *
	 * @throws NoSuchElementException
	 * 		When no matcher implementation is registered with the given ID.
	 */
	@Nullable
	public NumberPredicate newRangeNumberPredicate(@Nonnull String id, @Nonnull Number lower, @Nonnull Number upper) throws NoSuchElementException {
		RangeNumberMatcher matcher = rangeNumberMatchers.get(id);
		if (matcher != null)
			return new NumberPredicate(id, target -> matcher.test(lower, upper, target));
		throw new NoSuchElementException("No such ranged-parameter matcher: " + id);
	}

	/**
	 * @param id
	 * 		Matcher unique ID.
	 * @param keys
	 * 		Collection of numbers to match against.
	 *
	 * @return Predicate to target the given numbers.
	 *
	 * @throws NoSuchElementException
	 * 		When no matcher implementation is registered with the given ID.
	 */
	@Nullable
	public NumberPredicate newMultiNumberPredicate(@Nonnull String id, @Nonnull Collection<Number> keys) throws NoSuchElementException {
		MultiNumberMatcher matcher = multiNumberMatchers.get(id);
		if (matcher != null)
			return new NumberPredicate(id, target -> matcher.test(keys, target));
		throw new NoSuchElementException("No such multi-parameter matcher: " + id);
	}

	/**
	 * @return Map of matcher keys to implementations.
	 */
	@Nonnull
	public Map<String, BiNumberMatcher> getBiNumberMatchers() {
		return Collections.unmodifiableMap(biNumberMatchers);
	}

	/**
	 * @return Map of matcher keys to implementations.
	 */
	@Nonnull
	public Map<String, RangeNumberMatcher> getRangeNumberMatchers() {
		return Collections.unmodifiableMap(rangeNumberMatchers);
	}

	/**
	 * @return Map of matcher keys to implementations.
	 */
	@Nonnull
	public Map<String, MultiNumberMatcher> getMultiNumberMatchers() {
		return Collections.unmodifiableMap(multiNumberMatchers);
	}
}
