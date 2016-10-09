package com.kspilot.desktop

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.backends.lwjgl.LwjglApplication
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration
import com.badlogic.gdx.backends.lwjgl.LwjglFrame
import com.kspilot.KSPilotGame
import com.kspilot.desktop.display.DisplayHandle
import com.kspilot.desktop.display.DisplayServerSettings
import com.kspilot.desktop.display.LocalDisplayServer
import com.kspilot.desktop.form.MainForm
import java.awt.Desktop
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.*
import javax.swing.JFrame
import kotlin.concurrent.thread

object DesktopLauncher {
    @JvmStatic fun main(args: Array<String>) {
        var gl30ByArg = false
        var gl20ByArg = false
        var vsyncByArg = false
        var noVsyncByArg = false

        for (arg: String in args.map { it.toLowerCase(Locale.US) }) {
            when (arg) {
                GL_30_ARG -> gl30ByArg = true
                GL_20_ARG -> gl20ByArg = true
                VSYNC_ARG -> vsyncByArg = true
                NO_VSYNC_ARG -> noVsyncByArg = true
            }
        }


        val config = LwjglApplicationConfiguration().apply {
            title = WINDOW_TITLE
            resizable = RESIZABLE
            vSyncEnabled = !noVsyncByArg || vsyncByArg
            allowSoftwareMode = false
//            foregroundFPS = game.settings.targetFps
            fullscreen = false
            useGL30 = gl30ByArg && !gl20ByArg
        }

        val server = LocalDisplayServer()
        server.start()

        val frame = MainForm(WINDOW_TITLE)

        var display: DisplayHandle? = null
        frame.createClient = { display = server.newDisplay() }
        frame.destroyClient = { server.stop() }

        frame.apply {
            defaultCloseOperation = JFrame.HIDE_ON_CLOSE
            addComponentListener(object: ComponentAdapter() {
                override fun componentHidden(e: ComponentEvent) {
                    server.stop()
                    dispose()
                }
            })
        }

        frame.isVisible = true


//        val frame1 = LwjglFrame(KSPilotGame(), config).apply {
//
//            defaultCloseOperation = JFrame.HIDE_ON_CLOSE
//
//            addComponentListener(object: ComponentAdapter() {
//                override fun componentHidden(e: ComponentEvent) {
////                    handle.close()
//                    server.stop()
//                    Gdx.app.exit()
//                }
//            })
//        }
//
//        val handle = server.newDisplay()
////        handle.start()
    }

    val MAJOR_VERSION: Int = 0
    val MINOR_VERSION: Int = 0
    val REVISION: Int = 0
    val APPLICATION_NAME: String = "KSPilot"
    val WINDOW_TITLE = "$APPLICATION_NAME v$MAJOR_VERSION.$MINOR_VERSION.$REVISION"

    val RESIZABLE: Boolean = true

    // Command Lines Arguments
    val GL_30_ARG = "--gl30"
    val GL_20_ARG = "--gl20"
    val VSYNC_ARG = "--force-vsync"
    val NO_VSYNC_ARG = "--no-vsync"
}
