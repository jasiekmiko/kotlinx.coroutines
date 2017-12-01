/*
 * Copyright 2016-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.coroutines.experimental

import kotlinx.coroutines.experimental.CoroutineStart.*
import kotlinx.coroutines.experimental.intrinsics.startCoroutineUndispatched
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.startCoroutine

/**
 * Starts the corresponding block as a coroutine with this coroutine start strategy.
 *
 * * [DEFAULT] uses [startCoroutineCancellable].
 * * [ATOMIC] uses [startCoroutine].
 * * [UNDISPATCHED] uses [startCoroutineUndispatched].
 * * [LAZY] does nothing.
 */
public operator fun <T> CoroutineStart.invoke(block: suspend () -> T, completion: Continuation<T>) =
    when (this) {
        CoroutineStart.DEFAULT -> block.startCoroutineCancellable(completion)
        CoroutineStart.ATOMIC -> block.startCoroutine(completion)
        CoroutineStart.UNDISPATCHED -> block.startCoroutineUndispatched(completion)
        CoroutineStart.LAZY -> Unit // will start lazily
    }

/**
 * Starts the corresponding block with receiver as a coroutine with this coroutine start strategy.
 *
 * * [DEFAULT] uses [startCoroutineCancellable].
 * * [ATOMIC] uses [startCoroutine].
 * * [UNDISPATCHED] uses [startCoroutineUndispatched].
 * * [LAZY] does nothing.
 */
public operator fun <R, T> CoroutineStart.invoke(block: suspend R.() -> T, receiver: R, completion: Continuation<T>) =
    when (this) {
        CoroutineStart.DEFAULT -> block.startCoroutineCancellable(receiver, completion)
        CoroutineStart.ATOMIC -> block.startCoroutine(receiver, completion)
        CoroutineStart.UNDISPATCHED -> block.startCoroutineUndispatched(receiver, completion)
        CoroutineStart.LAZY -> Unit // will start lazily
    }
