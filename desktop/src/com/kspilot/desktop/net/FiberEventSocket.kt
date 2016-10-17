package com.kspilot.desktop.net

import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.io.FiberSocketChannel
import co.paralleluniverse.kotlin.Actor
import com.kspilot.util.Event
import com.kspilot.util.EventArgs
import com.kspilot.util.EventWrapper
import java.net.SocketException
import java.nio.ByteBuffer
import java.nio.channels.InterruptedByTimeoutException
import java.util.concurrent.TimeUnit

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
class FiberEventSocket(private val socket: FiberSocketChannel) {

    private val sendActor: Actor
    private val receiveActor: Actor
    private val dataReceivedEvent: Event<FiberSocketEventArgs> = Event()
    private var shouldStop: Boolean = false

    val dataReceived: EventWrapper<FiberSocketEventArgs> = dataReceivedEvent.wrapper

    init {
        // Send
        this.sendActor = object: Actor() {
            @Suspendable
            override fun doRun() {
                while (!shouldStop) {
                    receive(1000, TimeUnit.MILLISECONDS) {
                        when(it) {
                            is ByteArray -> writeBytes(it)
                            else -> null // discard
                        }
                    }
                }
            }
        }
        this.sendActor.spawn()

        // Receive
        this.receiveActor = object: Actor() {
            @Suspendable
            override fun doRun() {
                while (!shouldStop) {
                    try {
                        val data = readNextByteArray(1000, TimeUnit.MILLISECONDS)
                        dataReceivedEvent.dispatch(this, FiberSocketEventArgs(data))
                    } catch (ex: InterruptedByTimeoutException) {
                        // Timeout - keep going
                    } catch (ex: SocketException) {
                        // End of Stream reached
                        stop()
                    }
                }
            }
        }
        this.receiveActor.spawn()
    }


    fun send(bytes: ByteArray) {
        this.sendActor.ref().send(bytes)
    }

    fun stop() {
        this.shouldStop = true
    }


    @Suspendable
    private fun readNextByteArray(timeout: Long, unit: TimeUnit): ByteArray {
        val size = this.readBytes(4, timeout, unit).getInt()
        return this.readBytes(size, timeout, unit).array()
    }

    @Suspendable
    private fun readBytes(count: Int, timeout: Long, unit: TimeUnit): ByteBuffer {
        val buf = ByteBuffer.allocate(count)
        var totalRead: Int = 0
        var numReadThisIteration: Int = 0

        while (totalRead < count && numReadThisIteration >= 0) {
            numReadThisIteration = this.socket.read(buf, timeout, unit) // ADD TIMEOUT
            totalRead += numReadThisIteration
        }

        if (numReadThisIteration == -1) {
            // End of Stream
            throw SocketException("End of Stream!")
        }

        return buf
    }

    @Suspendable
    private fun writeBytes(bytes: ByteArray) {
        val sizeBuf = ByteBuffer.allocate(4)
        sizeBuf.putInt(bytes.size)
        val buf = ByteBuffer.wrap(bytes)

        this.socket.write(sizeBuf)
        this.socket.write(buf)
    }

}

class FiberSocketEventArgs(val data: ByteArray): EventArgs() { }