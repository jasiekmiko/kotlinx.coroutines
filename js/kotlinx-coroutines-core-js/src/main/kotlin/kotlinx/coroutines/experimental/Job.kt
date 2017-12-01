package kotlinx.coroutines.experimental

import kotlin.coroutines.experimental.CoroutineContext

/**
 * A background job. Conceptually, a job is a cancellable thing with a simple life-cycle that
 * culminates in its completion. Jobs can be arranged into parent-child hierarchies where cancellation
 * or completion of parent immediately cancels all its children.
 *
 * The most basic instances of [Job] are created with [launch] coroutine builder or with a
 * `Job()` factory function.  Other coroutine builders and primitives like
 * [Deferred] also implement [Job] interface.
 *
 * A job has the following states:
 *
 * | **State**                               | [isActive] | [isCompleted] | [isCancelled] |
 * | --------------------------------------- | ---------- | ------------- | ------------- |
 * | _New_ (optional initial state)          | `false`    | `false`       | `false`       |
 * | _Active_ (default initial state)        | `true`     | `false`       | `false`       |
 * | _Completing_ (optional transient state) | `true`     | `false`       | `false`       |
 * | _Cancelling_ (optional transient state) | `false`    | `false`       | `true`        |
 * | _Cancelled_ (final state)               | `false`    | `true`        | `true`        |
 * | _Completed_ (final state)               | `false`    | `true`        | `false`       |
 *
 * Usually, a job is created in _active_ state (it is created and started). However, coroutine builders
 * that provide an optional `start` parameter create a coroutine in _new_ state when this parameter is set to
 * [CoroutineStart.LAZY]. Such a job can be made _active_ by invoking [start] or [join].
 *
 * A job can be _cancelled_ at any time with [cancel] function that forces it to transition to
 * _cancelling_ state immediately. Job that is not backed by a coroutine and does not have
 * [children][attachChild] becomes _cancelled_ on [cancel] immediately.
 * Otherwise, job becomes _cancelled_  when it finishes executing its code and
 * when all its children [complete][isCompleted].
 *
 * ```
 *                                                      wait children
 *    +-----+       start      +--------+   complete   +-------------+  finish  +-----------+
 *    | New | ---------------> | Active | -----------> | Completing  | -------> | Completed |
 *    +-----+                  +--------+              +-------------+          +-----------+
 *       |                         |                         |
 *       | cancel                  | cancel                  | cancel
 *       V                         V                         |
 *  +-----------+   finish   +------------+                  |
 *  | Cancelled | <--------- | Cancelling | <----------------+
 *  |(completed)|            +------------+
 *  +-----------+
 * ```
 *
 * A job in the [coroutineContext][CoroutineScope.coroutineContext] represents the coroutine itself.
 * A job is active while the coroutine is working and job's cancellation aborts the coroutine when
 * the coroutine is suspended on a _cancellable_ suspension point by throwing [CancellationException].
 *
 * A job can have a _parent_ job. A job with a parent is cancelled when its parent is cancelled or completes exceptionally.
 * Parent job waits for all its [children][attachChild] to complete in _completing_ or _cancelling_ state.
 * _Completing_ state is purely internal to the job. For an outside observer a _completing_ job is still active,
 * while internally it is waiting for its children.
 *
 * All functions on this interface and on all interfaces derived from it are **thread-safe** and can
 * be safely invoked from concurrent coroutines without external synchronization.
 */
public actual interface Job : CoroutineContext.Element {

    // ------------ state query ------------

    /**
     * Returns `true` when this job is active -- it was already started and has not completed or cancelled yet.
     * The job that is waiting for its [children][attachChild] to complete is still considered to be active if it
     * was not cancelled.
     */
    public actual val isActive: Boolean

    /**
     * Returns `true` when this job has completed for any reason. A job that was cancelled and has
     * finished its execution is also considered complete. Job becomes complete only after
     * all its [children][attachChild] complete.
     */
    public actual val isCompleted: Boolean

    /**
     * Returns `true` if this job was [cancelled][cancel]. In the general case, it does not imply that the
     * job has already [completed][isCompleted] (it may still be cancelling whatever it was doing).
     */
    public actual val isCancelled: Boolean

    /**
     * Returns [CancellationException] that signals the completion of this job.
     *
     * It returns the original [cancel] cause if it is an instance of [CancellationException] or
     * an instance of [JobCancellationException] if this job was cancelled with a cause of
     * different type, was cancelled without a cause or had completed normally.
     *
     * This function throws [IllegalStateException] when invoked for an job that has not
     * [completed][isCompleted] nor [cancelled][isCancelled] yet.
     *
     * The [cancellable][suspendCancellableCoroutine] suspending functions throw this exception
     * when trying to suspend in the context of this job.
     */
    public actual fun getCancellationException(): CancellationException

    // ------------ state update ------------

    /**
     * Starts coroutine related to this job (if any) if it was not started yet.
     * The result `true` if this invocation actually started coroutine or `false`
     * if it was already started or completed.
     */
    public actual fun start(): Boolean

    /**
     * Cancels this job. The result is `true` if this job was
     * cancelled as a result of this invocation and `false` otherwise
     * (if it was already _completed_ or if it is [NonCancellable]).
     * Repeated invocations of this function have no effect and always produce `false`.
     */
    // todo: need support for default to support cause
    public actual fun cancel(): Boolean

    // ------------ parent-child ------------

    /**
     * Attaches child job so that this job becomes its parent and
     * returns a handle that should be used to detach it.
     *
     * A parent-child relation has the following effect:
     * * Cancellation of parent with [cancel] or its exceptional completion (failure)
     *   immediately cancels all its children.
     * * Parent cannot complete until all its children are complete. Parent waits for all its children to
     *   complete in _completing_ or _cancelling_ state.
     *
     * A child must store the resulting [DisposableHandle] and [dispose][DisposableHandle.dispose] the attachment
     * to its parent on its own completion.
     *
     * Coroutine builders and job factory functions that accept `parent` [CoroutineContext] parameter
     * lookup a [Job] instance in the parent context and use this function to attach themselves as a child.
     * They also store a reference to the resulting [DisposableHandle] and dispose a handle when they complete.
     */
    public actual fun attachChild(child: Job): DisposableHandle

    /**
     * Cancels all [children][attachChild] jobs of this coroutine. Unlike [cancel],
     * the state of this job itself is not affected.
     */
    // todo: need support for default to support cause
    public actual fun cancelChildren()

    // ------------ state waiting ------------

    /**
     * Suspends coroutine until this job is complete. This invocation resumes normally (without exception)
     * when the job is complete for any reason and the [Job] of the invoking coroutine is still [active][isActive].
     * This function also [starts][Job.start] the corresponding coroutine if the [Job] was still in _new_ state.
     *
     * Note, that the job becomes complete only when all its [children][attachChild] are complete.
     *
     * This suspending function is cancellable and **always** checks for the cancellation of invoking coroutine's Job.
     * If the [Job] of the invoking coroutine is cancelled or completed when this
     * suspending function is invoked or while it is suspended, this function
     * throws [CancellationException].
     *
     * In particular, it means that a parent coroutine invoking `join` on a child coroutine that was started using
     * `launch(coroutineContext) { ... }` builder throws [CancellationException] if the child
     * had crashed, unless a non-standard [CoroutineExceptionHandler] if installed in the context.
     *
     * This function can be used in [select] invocation with [onJoin] clause.
     * Use [isCompleted] to check for completion of this job without waiting.
     *
     * There is [cancelAndJoin] function that combines an invocation of [cancel] and `join`.
     */
    public actual suspend fun join()

    // ------------ low-level state-notification ------------

    /**
     * Registers handler that is **synchronously** invoked once on cancellation or completion of this job.
     * When job is already complete, then the handler is immediately invoked
     * with a job's cancellation cause or `null`. Otherwise, handler will be invoked once when this
     * job is cancelled or complete.
     *
     * The resulting [DisposableHandle] can be used to [dispose][DisposableHandle.dispose] the
     * registration of this handler and release its memory if its invocation is no longer needed.
     * There is no need to dispose the handler after completion of this job. The references to
     * all the handlers are released when this job completes.
     *
     * Installed [handler] should not throw any exceptions. If it does, they will get caught,
     * wrapped into [CompletionHandlerException], and rethrown, potentially causing crash of unrelated code.
     *
     * **Note**: This function is a part of internal machinery that supports parent-child hierarchies
     * and allows for implementation of suspending functions that wait on the Job's state.
     * This function should not be used in general application code.
     * Implementations of `CompletionHandler` must be fast and _lock-free_.
     */
    public actual fun invokeOnCompletion(handler: CompletionHandler): DisposableHandle

    /**
     * Key for [Job] instance in the coroutine context.
     */
    public actual companion object Key : CoroutineContext.Key<Job>
}

/**
 * A handle to an allocated object that can be disposed to make it eligible for garbage collection.
 */
public actual interface DisposableHandle {
    /**
     * Disposes the corresponding object, making it eligible for garbage collection.
     * Repeated invocation of this function has no effect.
     */
    public actual fun dispose()
}

actual open class CancellationException actual constructor(message: String) : IllegalStateException(message) {
    actual open fun initCause(cause: Throwable): Throwable {
        return this
    }
}
