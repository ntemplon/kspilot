package com.kspilot.net

import com.kspilot.util.Event
import com.kspilot.util.EventArgs
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
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
class EventServerSocket(private val server: ServerSocket) {

    private var shouldStop: Boolean = false
    private val clients: MutableList<EventSocket> = mutableListOf()
    private val clientConnectedEvent: Event<SocketConnectionArgs> = Event()

    val clientConnected = this.clientConnectedEvent.wrapper


    fun start() {
        this.shouldStop = false
        Thread(Runnable {
            server.soTimeout = DEFAULT_TIMEOUT
            while (!this.shouldStop) {
                try {
                    val socket = EventSocket(this.server.accept())
                    println("Socket connected from ${socket.inetAddress}")
                    this.clients.add(socket)
                    this.clientConnectedEvent.dispatch(this, SocketConnectionArgs(socket))
                } catch (ste: SocketTimeoutException) {
                    // Timeout - loop around again
                    println("Timeout!")
                }
            }
            println("Exiting")
        }).start()
    }

    fun stop() {
        println("Server stopping!")
        for(client in this.clients) {
            client.send(byteArrayOf(0.toByte()))
            client.stop()
        }
        this.shouldStop = true
    }

    companion object {
        val DEFAULT_TIMEOUT = 5000 // ms
        val DEFAULT_TIMEOUT_LONG = DEFAULT_TIMEOUT.toLong()
        val DEFAULT_TIMEOUT_UNIT = TimeUnit.MILLISECONDS
        val DEFAULT_PORT = 4999
    }

}

class SocketConnectionArgs(val socket: EventSocket) : EventArgs()