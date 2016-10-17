package com.kspilot.desktop.display

import co.paralleluniverse.fibers.io.FiberServerSocketChannel
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.thread
import kotlin.concurrent.withLock

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
class LocalDisplayServer(val settings: DisplayServerSettings = DisplayServerSettings()) {

    private val server: FiberServerSocketChannel by lazy {
        FiberServerSocketChannel.open().bind(InetSocketAddress(this.port))
    }
    private val handleLookup: MutableMap<UUID, DisplayHandle> = mutableMapOf()
    private val clientQueue: BlockingQueue<Socket> = LinkedBlockingQueue<Socket>()
    private val clientWaiters: ConcurrentHashMap<UUID, () -> Unit> = ConcurrentHashMap()
    private val pendingClients: ConcurrentHashMap<UUID, Socket> = ConcurrentHashMap()

    private var shouldStop = false

    val port: Int
        get() = this.settings.port
    val timeout: Int
        get() = this.settings.msTimeout


    fun start() {

    }

    fun stop() {
        this.shouldStop = true
    }

    fun newDisplay(): DisplayHandle {
        val uuid = UUID.randomUUID()
        val lock = Semaphore(1)
        lock.acquire()

        this.clientWaiters[uuid] = {
            lock.release()
        }

        val process = this.startProcess(uuid)

        lock.acquire()
        lock.release()

        val client = this.pendingClients[uuid]!!
        throw NotImplementedError()
//        return DisplayHandle(client)
    }


    private fun startProcess(uuid: UUID): Process {
        val procBuild = ProcessBuilder(JAVA_BIN, COMMAND, CLASSPATH, CLASS_NAME,
                UUID_FLAG, uuid.toString(),
                IP_FLAG, DEFAULT_IP,
                PORT_FLAG, this.port.toString())
        return procBuild.start()
    }

    private fun handle(client: Socket) {
        val idMsg = ObjectInputStream(client.inputStream).readObject() as? DisplayIdMessage
        if (idMsg == null) {
            println("Message was null")
            client.close()
            return
        }

        client.soTimeout = this.settings.msTimeout
        this.pendingClients[idMsg.uuid] = client

        if (this.clientWaiters.containsKey(idMsg.uuid)) {
            this.clientWaiters[idMsg.uuid]?.invoke()
        }
    }


    companion object {
        val JAVA_HOME: String = System.getProperty("java.home")
        val JAVA_BIN: String = Paths.get(JAVA_HOME, "bin", "java").toString()
        val CLASSPATH: String = System.getProperty("java.class.path")
        val CLASS_NAME: String = LocalDisplayServer::class.java.canonicalName
        val COMMAND: String = "-cp"
        val UUID_FLAG: String = "-uuid"
        val IP_FLAG: String = "-ip"
        val PORT_FLAG: String = "-port"
        val DEFAULT_IP: String = "localhost"


        @JvmStatic
        fun main(args: Array<String>) {
            val uuid = UUID.fromString(args[args.indexOf(UUID_FLAG) + 1])
            val ip = args[args.indexOf(IP_FLAG) + 1]
            val port = args[args.indexOf(PORT_FLAG) + 1].toInt()

//            val display = Display(uuid, ip, port)
//            display.start()
        }
    }

}


data class DisplayServerSettings(val port: Int = DEFAULT_PORT, val msTimeout: Int = DEFAULT_TIMEOUT_MS) {

    companion object {
        val DEFAULT_TIMEOUT_MS = 1000 // 1 second
        val DEFAULT_PORT: Int = 49563 // 49152 - 65535 is private ports
    }

}


open class DisplayMessage : java.io.Serializable {

}


class DisplayIdMessage(val uuid: UUID): DisplayMessage() {

}

class DisplayStopMessage(): DisplayMessage() {}