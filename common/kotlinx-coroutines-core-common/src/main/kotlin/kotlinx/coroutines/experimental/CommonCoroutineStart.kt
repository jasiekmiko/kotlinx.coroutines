package kotlinx.coroutines.experimental

/**
 * Defines start option for coroutines builders.
 * It is used in `start` parameter of [launch], [async], and [actor][kotlinx.coroutines.experimental.channels.actor]
 * coroutine builder functions.
 *
 * The summary of coroutine start options is:
 * * [DEFAULT] -- immediately schedules coroutine for execution according to its context;
 * * [LAZY] -- starts coroutine lazily, only when it is needed;
 * * [ATOMIC] -- atomically (non-cancellably) schedules coroutine for execution according to its context;
 * * [UNDISPATCHED] -- immediately executes coroutine until its first suspension point _in the current thread_.
 */
public enum class CoroutineStart {
    /**
     * Default -- immediately schedules coroutine for execution according to its context.
     *
     * If the [CoroutineDispatcher] of the coroutine context returns `true` from [CoroutineDispatcher.isDispatchNeeded]
     * function as most dispatchers do, then the coroutine code is dispatched for execution later, while the code that
     * invoked the coroutine builder continues execution.
     *
     * Note, that [Unconfined] dispatcher always returns `false` from its [CoroutineDispatcher.isDispatchNeeded]
     * function, so starting coroutine with [Unconfined] dispatcher by [DEFAULT] is the same as using [UNDISPATCHED].
     *
     * If coroutine [Job] is cancelled before it even had a chance to start executing, then it will not start its
     * execution at all, but complete with an exception.
     *
     * Cancellability of coroutine at suspension points depends on the particular implementation details of
     * suspending functions. Use [suspendCancellableCoroutine] to implement cancellable suspending functions.
     */
    DEFAULT,

    /**
     * Starts coroutine lazily, only when it is needed.
     *
     * See the documentation for the corresponding coroutine builders for details:
     * [launch], [async], and [actor][kotlinx.coroutines.experimental.channels.actor].
     *
     * If coroutine [Job] is cancelled before it even had a chance to start executing, then it will not start its
     * execution at all, but complete with an exception.
     */
    LAZY,

    /**
     * Atomically (non-cancellably) schedules coroutine for execution according to its context.
     * This is similar to [DEFAULT], but the coroutine cannot be cancelled before it starts executing.
     *
     * Cancellability of coroutine at suspension points depends on the particular implementation details of
     * suspending functions as in [DEFAULT].
     */
    ATOMIC,

    /**
     * Immediately executes coroutine until its first suspension point _in the current thread_ as if it the
     * coroutine was started using [Unconfined] dispatcher. However, when coroutine is resumed from suspension
     * it is dispatched according to the [CoroutineDispatcher] in its context.
     *
     * This is similar to [ATOMIC] in the sense that coroutine starts executing even if it was already cancelled,
     * but the difference is that it start executing in the same thread.
     *
     * Cancellability of coroutine at suspension points depends on the particular implementation details of
     * suspending functions as in [DEFAULT].
     */
    UNDISPATCHED;

    /**
     * Returns `true` when [LAZY].
     */
    public val isLazy: Boolean get() = this === LAZY
}
