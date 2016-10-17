package com.kspilot.net

import com.kspilot.util.Event
import com.kspilot.util.EventArgs
import java.net.InetAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
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
class EventSocket(private val socket: Socket) {

    private var shouldStop = false
    private val sendQueue: LinkedBlockingQueue<ByteArray> = LinkedBlockingQueue()
    private val dataReceivedEvent: Event<DataReceivedArgs> = Event()
    private val closingEvent: Event<EventArgs> = Event()

    val inetAddress: InetAddress
        get() = this.socket.inetAddress
    val dataReceived = this.dataReceivedEvent.wrapper
    val closing = this.closingEvent.wrapper


    init {
        this.socket.soTimeout = EventServerSocket.DEFAULT_TIMEOUT
    }


    fun start() {
        this.shouldStop = false

        Thread(Runnable {
            while(!this.shouldStop) {
                try {
                    val data: ByteArray = this.readNextByteArray()
                    if (data.size == 1 && data[0] == 0.toByte()) {
                        this.stop()
                    } else {
                        this.dataReceivedEvent.dispatch(this, DataReceivedArgs(data))
                    }
                } catch(sto: SocketTimeoutException) {
                    // Keep going
                    println("Client Receive Timeout!")
                } catch(so: SocketException) {
                    println("Client Socket Exception")
                    this.stop()
                }
            }
        }).start()

        Thread(Runnable {
            while(!this.shouldStop) {
                try {
                    val data: ByteArray? = this.sendQueue.poll(EventServerSocket.DEFAULT_TIMEOUT_LONG, EventServerSocket.DEFAULT_TIMEOUT_UNIT)
                    if (data != null) {
                        println("Ready to send data of length ${data.size}.")
                        this.socket.outputStream.write(ByteBuffer.allocate(4).putInt(data.size).array())
                        this.socket.outputStream.write(data)
                        println("Sent data.")
                    } else {
                        println("Client Send Timeout!")
                    }
                } catch(so: SocketException) {
                    println("Client Socket Exception")
                    this.stop()
                }
            }
        }).start()
    }

    fun stop() {
        println("Client stopping!")
        this.closingEvent.dispatch(this, EventArgs())
        this.shouldStop = true
    }

    fun send(data: ByteArray) {
        this.sendQueue.add(data)
    }


    private fun readNextByteArray(): ByteArray {
        val size = ByteBuffer.wrap(this.readBytes(4)).getInt()
        println("Expecting packet of size $size.")
        return this.readBytes(size)
    }

    private fun readBytes(length: Int): ByteArray {
        val data = ByteArray(length)
        var totalBytesRead = 0
        var bytesReadThisRound = 0
        while(totalBytesRead < length && bytesReadThisRound >= 0) {
            bytesReadThisRound = this.socket.inputStream.read(data, totalBytesRead, length - totalBytesRead)
            println("Read $bytesReadThisRound bytes.")
            if (bytesReadThisRound >= 0) {
                totalBytesRead += bytesReadThisRound
            }
        }

        if (bytesReadThisRound < 0) {
            throw SocketException("The socket has been disconnected!")
        }

        return data
    }

}

class DataReceivedArgs(val data: ByteArray): EventArgs()