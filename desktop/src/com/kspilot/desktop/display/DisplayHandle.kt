package com.kspilot.desktop.display

import co.paralleluniverse.fibers.Suspendable
import co.paralleluniverse.fibers.io.FiberSocketChannel
import co.paralleluniverse.kotlin.Actor
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.IntBuffer
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.*
import kotlin.concurrent.thread

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
class DisplayHandle(val socket: FiberSocketChannel) {

    private var shouldStop: Boolean = false


    fun start() {
        val receive: Actor = object: Actor() {
            @Suspendable
            override fun doRun() {
                handleReceive()
            }
        }
        receive.spawn()
    }


    @Suspendable
    private fun handleReceive() {
        while (!this.shouldStop) {
            val data = this.readNextByteArray()
            if (data.size == 1 && data[0] == 0.toByte()) {
                this.shouldStop = true
            }
        }
    }

    @Suspendable
    private fun handleSend() {

    }

    @Suspendable
    private fun readNextByteArray(): ByteArray {
        val size = this.readBytes(4).getInt()
        return this.readBytes(size).array()
    }

    @Suspendable
    private fun readBytes(count: Int): ByteBuffer {
        val buf = ByteBuffer.allocate(count)
        var totalRead: Int = 0
        var numReadThisIteration: Int = 0

        while (totalRead < count && numReadThisIteration >= -1) {
            numReadThisIteration = this.socket.read(buf) // ADD TIMEOUT
            totalRead += numReadThisIteration
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