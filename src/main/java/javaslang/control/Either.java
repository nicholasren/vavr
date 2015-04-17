/*     / \____  _    ______   _____ / \____   ____  _____
 *    /  \__  \/ \  / \__  \ /  __//  \__  \ /    \/ __  \   Javaslang
 *  _/  // _\  \  \/  / _\  \\_  \/  // _\  \  /\  \__/  /   Copyright 2014-2015 Daniel Dietrich
 * /___/ \_____/\____/\_____/____/\___\_____/_/  \_/____/    Licensed under the Apache License, Version 2.0
 */
package javaslang.control;

import javaslang.Tuple1;
import javaslang.ValueObject;
import javaslang.algebra.HigherKinded;
import javaslang.algebra.Monad;
import javaslang.control.Valences.Bivalent;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Either represents a value of two possible types. An Either is either a {@link javaslang.control.Left} or a
 * {@link javaslang.control.Right}.
 * <p>
 * It is possible to project an Either to a Left or a Right. Both cases can be further processed with
 * {@link javaslang.algebra.Monad} operations.
 * <p>
 * If the given Either is a Right and projected to a Left, the Left operations have no effect on the Right value.<br>
 * If the given Either is a Left and projected to a Right, the Right operations have no effect on the Left value.<br>
 * If a Left is projected to a Left or a Right is projected to a Right, the operations have an effect.
 * <p>
 * <strong>Example:</strong> A compute() function, which results either in an Integer value (in the case of success) or
 * in an error message of type String (in the case of failure). By convention the success case is Right and the failure
 * is Left.
 *
 * <pre>
 * <code>
 * Either&lt;String,Integer&gt; value = compute().right().map(i -&gt; i * 2).toEither();
 * </code>
 * </pre>
 *
 * If the result of compute() is Right(1), the value is Right(2).<br>
 * If the result of compute() is Left("error), the value is Left("error").
 *
 * @param <L> The type of the Left value of an Either.
 * @param <R> The type of the Right value of an Either.
 * @since 1.0.0
 */
public interface Either<L, R> extends ValueObject {

    /**
     * The <a href="https://docs.oracle.com/javase/8/docs/api/index.html">serial version uid</a>.
     */
    long serialVersionUID = 1L;

    /**
     * Returns whether this Either is a Left.
     *
     * @return true, if this is a Left, false otherwise
     */
    boolean isLeft();

    /**
     * Returns whether this Either is a Right.
     *
     * @return true, if this is a Right, false otherwise
     */
    boolean isRight();

    /**
     * Returns a LeftProjection of this Either.
     *
     * @return a new LeftProjection of this
     */
    default LeftProjection<L, R> left() {
        return new LeftProjection<>(this);
    }

    /**
     * Returns a RightProjection of this Either.
     *
     * @return a new RightProjection of this
     */
    default RightProjection<L, R> right() {
        return new RightProjection<>(this);
    }

    /**
     * Maps either the left or the right side of this disjunction.
     *
     * @param leftMapper  maps the left value if this is a Left
     * @param rightMapper maps the right value if this is a Right
     * @param <X>         The new left type of the resulting Either
     * @param <Y>         The new right type of the resulting Either
     * @return A new either instance
     */
    <X, Y> Either<X, Y> bimap(Function<? super L, ? extends X> leftMapper, Function<? super R, ? extends Y> rightMapper);

    // -- Object.*

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();

    @Override
    String toString();

    // -- Left/Right projections

    /**
     * A left projection of an either.
     *
     * @param <L> The type of the Left value of an Either.
     * @param <R> The type of the Right value of an Either.
     * @since 1.0.0
     */
    final class LeftProjection<L, R> implements Bivalent<L, R>, Monad<L, LeftProjection<?, R>> {

        private final Either<L, R> either;

        private LeftProjection(Either<L, R> either) {
            this.either = either;
        }

        /**
         * Gets the Left value or throws.
         *
         * @return the left value, if the underlying Either is a Left
         * @throws NoSuchElementException if the underlying either of this LeftProjection is a Right
         */
        @Override
        public L get() {
            if (either.isLeft()) {
                return asLeft();
            } else {
                throw new NoSuchElementException("Either.left().get() on Right");
            }
        }

        /**
         * Gets the Left value or an alternate value, if the projected Either is a Right.
         *
         * @param other an alternative value
         * @return the left value, if the underlying Either is a Left or else {@code other}
         * @throws NoSuchElementException if the underlying either of this LeftProjection is a Right
         */
        @Override
        public L orElse(L other) {
            return either.isLeft() ? asLeft() : other;
        }

        /**
         * Gets the Left value or an alternate value, if the projected Either is a Right.
         *
         * @param other a function which converts a Right value to an alternative Left value
         * @return the left value, if the underlying Either is a Left or else the alternative Left value provided by
         * {@code other} by applying the Right value.
         */
        @Override
        public L orElseGet(Function<? super R, ? extends L> other) {
            if (either.isLeft()) {
                return asLeft();
            } else {
                return other.apply(asRight());
            }
        }

        /**
         * Runs an action in the case this is a projection on a Right value.
         *
         * @param action an action which consumes a Right value
         */
        @Override
        public void orElseRun(Consumer<? super R> action) {
            if (either.isRight()) {
                action.accept(asRight());
            }
        }

        /**
         * Gets the Left value or throws, if the projected Either is a Right.
         *
         * @param <X>               a throwable type
         * @param exceptionFunction a function which creates an exception based on a Right value
         * @return the left value, if the underlying Either is a Left or else throws the exception provided by
         * {@code exceptionFunction} by applying the Right value.
         * @throws X if the projected Either is a Right
         */
        @Override
        public <X extends Throwable> L orElseThrow(Function<? super R, X> exceptionFunction) throws X {
            if (either.isLeft()) {
                return asLeft();
            } else {
                throw exceptionFunction.apply(asRight());
            }
        }

        /**
         * Converts this Either to an {@linkplain javaslang.control.Option}.
         *
         * @return {@linkplain javaslang.control.Some} of the left value if this is a projection of a Left,
         * {@linkplain javaslang.control.None} otherwise.
         */
        @Override
        public Option<L> toOption() {
            if (either.isLeft()) {
                return new Some<>(asLeft());
            } else {
                return None.instance();
            }
        }

        /**
         * Returns the underlying either of this projection.
         *
         * @return the underlying either
         */
        @Override
        public Either<L, R> toEither() {
            return either;
        }

        /**
         * Converts this Either to a {@linkplain java.util.Optional}.
         *
         * @return {@code Optional.ofNullable(leftValue)} if this is a projection of a Left,
         * {@code Optional.empty()} otherwise.
         */
        @Override
        public Optional<L> toJavaOptional() {
            if (either.isLeft()) {
                return Optional.ofNullable(asLeft());
            } else {
                return Optional.empty();
            }
        }

        /**
         * Returns {@code LeftProjection(Left(value))}, if the underlying Either of this projection is a Left and the
         * left value satisfies the given predicate. Otherwise {@code LeftProjection(Left())} (a left projection of
         * nothing) is returned.
         *
         * @param predicate A predicate
         * @return a LeftProjection
         */
        @SuppressWarnings("unchecked")
        @Override
        public LeftProjection<L, R> filter(Predicate<? super L> predicate) {
            Objects.requireNonNull(predicate);
            if (either.isRight() || (either.isLeft() && predicate.test(asLeft()))) {
                return this;
            } else {
                return Nothing.<L, R>instance().left();
            }
        }

        /**
         * Flattens a {@code LeftProjection}, assuming that the elements are of type LeftProjection&lt;U&gt;
         * <p>
         * Examples:
         * <pre>
         * <code>
         * Left.of(1).left().flatten();                   // throws
         * Left.of(Left.of(1).left()).left().flatten();   // = LeftProjection(Left(1))
         * Left.of(Left.of(1).right()).left().flatten();  // throws
         * Right.of(Right.of(1).left()).left().flatten(); // = LeftProjection(Right(1))
         * </code>
         * </pre>
         *
         * @return a {@code LeftProjection}
         * @throws java.lang.ClassCastException if the projected either is not of type
         *                                      {@code LeftProjection<? extends LeftProjection<U, R>, R>}
         */
        @SuppressWarnings("unchecked")
        @Override
        public <U> LeftProjection<U, R> flatten() {
            return ((LeftProjection<? extends LeftProjection<U, R>, R>) this).flatten(Function.identity());
        }

        /**
         * Flattens a {@code LeftProjection} using a function.
         *
         * @param f a function which maps elements of this LeftProjection to LeftProjections
         * @return a {@code LeftProjection}
         */
        @SuppressWarnings("unchecked")
        @Override
        public <U, LEFT_PROJECTION extends HigherKinded<U, LeftProjection<?, R>>> LeftProjection<U, R> flatten(Function<? super L, ? extends LEFT_PROJECTION> f) {
            if (either.isRight()) {
                return (LeftProjection<U, R>) this;
            } else {
                return (LeftProjection<U, R>) f.apply(get());
            }
        }

        /**
         * Applies the given action to the value if the projected either is a Left. Otherwise nothing happens.
         *
         * @param action An action which takes a left value
         */
        @Override
        public void forEach(Consumer<? super L> action) {
            Objects.requireNonNull(action);
            if (either.isLeft()) {
                action.accept(asLeft());
            }
        }

        /**
         * Applies the given action to the value if the projected either is a Left. Otherwise nothing happens.
         *
         * @param action An action which takes a left value
         * @return this LeftProjection
         */
        @Override
        public LeftProjection<L, R> peek(Consumer<? super L> action) {
            Objects.requireNonNull(action);
            if (either.isLeft()) {
                action.accept(asLeft());
            }
            return this;
        }

        /**
         * Maps the left value if the projected Either is a Left.
         *
         * @param mapper A mapper which takes a left value and returns a value of type U
         * @param <U>    The new type of a Left value
         * @return A new LeftProjection
         */
        @SuppressWarnings("unchecked")
        @Override
        public <U> LeftProjection<U, R> map(Function<? super L, ? extends U> mapper) {
            Objects.requireNonNull(mapper);
            if (either.isLeft())
                return Left.<U, R>of(mapper.apply(asLeft())).left();
            else {
                return (LeftProjection<U, R>) this;
            }
        }

        /**
         * FlatMaps the left value if the projected Either is a Left.
         *
         * @param mapper A mapper which takes a left value and returns a new Either
         * @param <U>    The new type of a Left value
         * @return A new LeftProjection
         */
        @SuppressWarnings("unchecked")
        @Override
        public <U, LEFT_PROJECTION extends HigherKinded<U, LeftProjection<?, R>>> LeftProjection<U, R> flatMap(Function<? super L, ? extends LEFT_PROJECTION> mapper) {
            Objects.requireNonNull(mapper);
            if (either.isLeft()) {
                return (LeftProjection<U, R>) mapper.apply(asLeft());
            } else {
                return (LeftProjection<U, R>) this;
            }
        }

        @Override
        public boolean equals(Object obj) {
            return (obj == this) || (obj instanceof LeftProjection && Objects.equals(either, ((LeftProjection<?, ?>) obj).either));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(either);
        }

        @Override
        public String toString() {
            return String.format("LeftProjection(%s)", either);
        }

        private L asLeft() {
            return ((Left<L, R>) either).get();
        }

        private R asRight() {
            return ((Right<L, R>) either).get();
        }

        /**
         * Represents an empty filter result of a {@code Left}.
         *
         * @param <L> The type of the Left value.
         * @param <R> The type of the Right value.
         * @since 1.3.0
         */
        private static final class Nothing<L, R> implements Left<L, R> {

            private static final long serialVersionUID = 1L;

            private static Nothing<?, ?> INSTANCE = new Nothing<>();

            private Nothing() {
            }

            @SuppressWarnings("unchecked")
            static <L, R> Nothing<L, R> instance() {
                return (Nothing<L, R>) INSTANCE;
            }

            @Override
            public L get() {
                throw new NoSuchElementException("Left.get() on empty Left.LeftProjection.filter()");
            }

            @Override
            public boolean isLeft() {
                return true;
            }

            @Override
            public boolean isRight() {
                return false;
            }

            @Override
            public <X, Y> Left<X, Y> bimap(Function<? super L, ? extends X> leftMapper, Function<? super R, ? extends Y> rightMapper) {
                throw new NoSuchElementException("Left.bimap() on empty Left.LeftProjection.filter()");
            }

            @Override
            public Tuple1<L> unapply() {
                throw new NoSuchElementException("Left.unapply() on empty Left.LeftProjection.filter()");
            }

            @Override
            public boolean equals(Object o) {
                return o == this || o instanceof Nothing;
            }

            @Override
            public int hashCode() {
                return Nothing.class.hashCode();
            }

            @Override
            public String toString() {
                return "Left()";
            }
        }
    }

    /**
     * A right projection of an either.
     *
     * @param <L> The type of the Left value of an Either.
     * @param <R> The type of the Right value of an Either.
     * @since 1.0.0
     */
    final class RightProjection<L, R> implements Bivalent<R, L>, Monad<R, RightProjection<L, ?>> {

        private final Either<L, R> either;

        private RightProjection(Either<L, R> either) {
            this.either = either;
        }

        /**
         * Gets the Right value or throws.
         *
         * @return the left value, if the underlying Either is a Right
         * @throws NoSuchElementException if the underlying either of this RightProjection is a Left
         */
        @Override
        public R get() {
            if (either.isRight()) {
                return asRight();
            } else {
                throw new NoSuchElementException("Either.right().get() on Left");
            }
        }

        /**
         * Gets the Right value or an alternate value, if the projected Either is a Left.
         *
         * @param other an alternative value
         * @return the right value, if the underlying Either is a Right or else {@code other}
         * @throws NoSuchElementException if the underlying either of this RightProjection is a Left
         */
        @Override
        public R orElse(R other) {
            return either.isRight() ? asRight() : other;
        }

        /**
         * Gets the Right value or an alternate value, if the projected Either is a Left.
         *
         * @param other a function which converts a Left value to an alternative Right value
         * @return the right value, if the underlying Either is a Right or else the alternative Right value provided by
         * {@code other} by applying the Left value.
         */
        @Override
        public R orElseGet(Function<? super L, ? extends R> other) {
            if (either.isRight()) {
                return asRight();
            } else {
                return other.apply(asLeft());
            }
        }

        /**
         * Runs an action in the case this is a projection on a Left value.
         *
         * @param action an action which consumes a Left value
         */
        @Override
        public void orElseRun(Consumer<? super L> action) {
            if (either.isLeft()) {
                action.accept(asLeft());
            }
        }

        /**
         * Gets the Right value or throws, if the projected Either is a Left.
         *
         * @param <X>               a throwable type
         * @param exceptionFunction a function which creates an exception based on a Left value
         * @return the right value, if the underlying Either is a Right or else throws the exception provided by
         * {@code exceptionFunction} by applying the Left value.
         * @throws X if the projected Either is a Left
         */
        @Override
        public <X extends Throwable> R orElseThrow(Function<? super L, X> exceptionFunction) throws X {
            if (either.isRight()) {
                return asRight();
            } else {
                throw exceptionFunction.apply(asLeft());
            }
        }

        /**
         * Converts this Either to an {@linkplain javaslang.control.Option}.
         *
         * @return {@linkplain javaslang.control.Some} of the right value if this is a projection of a Right,
         * {@linkplain javaslang.control.None} otherwise.
         */
        @Override
        public Option<R> toOption() {
            if (either.isRight()) {
                return new Some<>(asRight());
            } else {
                return None.instance();
            }
        }

        /**
         * Returns the underlying either of this projection.
         *
         * @return the underlying either
         */
        @Override
        public Either<L, R> toEither() {
            return either;
        }

        /**
         * Converts this Either to a {@linkplain java.util.Optional}.
         *
         * @return {@code Optional.ofNullable(rightValue)} if this is a projection of a Right,
         * {@code Optional.empty()} otherwise.
         */
        @Override
        public Optional<R> toJavaOptional() {
            if (either.isRight()) {
                return Optional.ofNullable(asRight());
            } else {
                return Optional.empty();
            }
        }

        /**
         * Returns {@code RightProjection(Right(value))}, if the underlying Either of this projection is a Right and
         * the right value satisfies the given predicate. Otherwise {@code RightProjection(Right())} (a right projection
         * of nothing) is returned.
         *
         * @param predicate A predicate
         * @return a RightProjection
         */
        @SuppressWarnings("unchecked")
        @Override
        public RightProjection<L, R> filter(Predicate<? super R> predicate) {
            Objects.requireNonNull(predicate);
            if (either.isLeft() || (either.isRight() && predicate.test(asRight()))) {
                return this;
            } else {
                return Nothing.<L, R>instance().right();
            }
        }

        /**
         * Flattens a {@code RightProjection}, assuming that the elements are of type RightProjection&lt;U&gt;
         * <p>
         * Examples:
         * <pre>
         * <code>
         * Right.of(1).right().flatten();                   // throws
         * Right.of(Right.of(1).right()).right().flatten(); // = RightProjection(Right(1))
         * Right.of(Right.of(1).left()).right().flatten();  // throws
         * Left.of(Left.of(1).right()).right().flatten();   // = RightProjection(Left(1))
         * </code>
         * </pre>
         *
         * @return a {@code RightProjection}
         * @throws java.lang.ClassCastException if the projected either is not of type
         *                                      {@code RightProjection<L, ? extends RightProjection<L, U>>}
         */
        @SuppressWarnings("unchecked")
        @Override
        public <U> RightProjection<L, U> flatten() {
            return ((RightProjection<L, ? extends RightProjection<L, U>>) this).flatten(Function.identity());
        }

        /**
         * Flattens a {@code RightProjection} using a function.
         *
         * @param f a function which maps elements of this RightProjection to RightProjections
         * @return a {@code RightProjection}
         */
        @SuppressWarnings("unchecked")
        @Override
        public <U, RIGHT_PROJECTION extends HigherKinded<U, RightProjection<L, ?>>> RightProjection<L, U> flatten(Function<? super R, ? extends RIGHT_PROJECTION> f) {
            if (either.isLeft()) {
                return (RightProjection<L, U>) this;
            } else {
                return (RightProjection<L, U>) f.apply(get());
            }
        }

        /**
         * Applies the given action to the value if the projected either is a Right. Otherwise nothing happens.
         *
         * @param action An action which takes a right value
         */
        public void forEach(Consumer<? super R> action) {
            Objects.requireNonNull(action);
            if (either.isRight()) {
                action.accept(asRight());
            }
        }

        /**
         * Applies the given action to the value if the projected either is a Right. Otherwise nothing happens.
         *
         * @param action An action which takes a right value
         * @return this {@code Either} instance
         */
        @Override
        public RightProjection<L, R> peek(Consumer<? super R> action) {
            Objects.requireNonNull(action);
            if (either.isRight()) {
                action.accept(asRight());
            }
            return this;
        }

        /**
         * Maps the right value if the projected Either is a Right.
         *
         * @param mapper A mapper which takes a right value and returns a value of type U
         * @param <U>    The new type of a Right value
         * @return A new RightProjection
         */
        @SuppressWarnings("unchecked")
        @Override
        public <U> RightProjection<L, U> map(Function<? super R, ? extends U> mapper) {
            Objects.requireNonNull(mapper);
            if (either.isRight())
                return Right.<L, U>of(mapper.apply(asRight())).right();
            else {
                return (RightProjection<L, U>) this;
            }
        }

        /**
         * FlatMaps the right value if the projected Either is a Right.
         *
         * @param mapper A mapper which takes a right value and returns a new Either
         * @param <U>    The new type of a Right value
         * @return A new RightProjection
         */
        @SuppressWarnings("unchecked")
        @Override
        public <U, RIGHT_PROJECTION extends HigherKinded<U, RightProjection<L, ?>>> RightProjection<L, U> flatMap(Function<? super R, ? extends RIGHT_PROJECTION> mapper) {
            Objects.requireNonNull(mapper);
            if (either.isRight()) {
                return (RightProjection<L, U>) mapper.apply(asRight());
            } else {
                return (RightProjection<L, U>) this;
            }
        }

        @Override
        public boolean equals(Object obj) {
            return (obj == this) || (obj instanceof RightProjection && Objects.equals(either, ((RightProjection<?, ?>) obj).either));
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(either);
        }

        @Override
        public String toString() {
            return String.format("RightProjection(%s)", either);
        }

        private L asLeft() {
            return ((Left<L, R>) either).get();
        }

        private R asRight() {
            return ((Right<L, R>) either).get();
        }

        /**
         * Represents an empty filter result of a {@code Right}.
         *
         * @param <L> The type of the Left value.
         * @param <R> The type of the Right value.
         * @since 1.3.0
         */
        private static final class Nothing<L, R> implements Right<L, R> {

            private static final long serialVersionUID = 1L;

            private static Nothing<?, ?> INSTANCE = new Nothing<>();

            private Nothing() {
            }

            @SuppressWarnings("unchecked")
            static <L, R> Nothing<L, R> instance() {
                return (Nothing<L, R>) INSTANCE;
            }

            @Override
            public R get() {
                throw new NoSuchElementException("Right.get() on empty Right.RightProjection.filter()");
            }

            @Override
            public boolean isLeft() {
                return false;
            }

            @Override
            public boolean isRight() {
                return true;
            }

            @Override
            public <X, Y> Right<X, Y> bimap(Function<? super L, ? extends X> leftMapper, Function<? super R, ? extends Y> rightMapper) {
                throw new NoSuchElementException("Right.bimap() on empty Right.RightProjection.filter()");
            }

            @Override
            public Tuple1<R> unapply() {
                throw new NoSuchElementException("Right.unapply() on empty Right.RightProjection.filter()");
            }

            @Override
            public boolean equals(Object o) {
                return o == this || o instanceof Nothing;
            }

            @Override
            public int hashCode() {
                return Nothing.class.hashCode();
            }

            @Override
            public String toString() {
                return "Right()";
            }
        }
    }
}
