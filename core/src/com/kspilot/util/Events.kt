package com.kspilot.util

import javax.swing.JButton

/**
 * Copyright (c) 2016 Nathan S. Templon
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions
 * of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */
class Event<T: EventArgs> {

    private val listeners = mutableSetOf<Listener<T>>()
    private val funListeners = mutableSetOf<(Any, T) -> Unit>()

    val wrapper by lazy { EventWrapper(this) }


    fun addListener(listener: Listener<T>): Boolean = this.listeners.add(listener)

    fun addListener(listener: (Any, T) -> Unit): Boolean = this.funListeners.add(listener)

    operator fun plusAssign(listener: Listener<T>): Unit {
        this.addListener(listener)
    }
    operator fun plusAssign(listener: (Any, T) -> Unit): Unit {
        this.addListener(listener)
    }

    fun dispatch(sender: Any, e: T) {
        this.listeners.forEach { it.handle(sender, e) }
        this.funListeners.forEach { it.invoke(sender, e) }
    }

}

open class EventArgs {
    companion object {
        val EMPTY: EventArgs by lazy { EventArgs() }
    }
}

class EventWrapper<out T: EventArgs>(private val event: Event<T>) {

    fun addListener(listener: Listener<T>): Boolean = this.event.addListener(listener)
    fun addListener(listener: (Any, T) -> Unit): Boolean = this.event.addListener(listener)

    operator fun plusAssign(listener: Listener<T>): Unit {
        this.addListener(listener)
    }
    operator fun plusAssign(listener: (Any, T) -> Unit): Unit {
        this.addListener(listener)
    }

}


interface Listener<in T> {
    fun handle(sender: Any, e: T): Unit
}