package kotlinx.coroutines.experimental

import kotlinx.coroutines.experimental.NonCancellable.isActive

/**
 * A non-cancelable job that is always [active][isActive]. It is designed to be used with [run] builder
 * to prevent cancellation of code blocks that need to run without cancellation.
 *
 * Use it like this:
 * ```
 * run(NonCancellable) {
 *     // this code will not be cancelled
 * }
 * ```
 */
public expect object NonCancellable : Job