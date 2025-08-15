package com.example.fastfruit

import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import kotlin.concurrent.thread
import java.io.PrintWriter
import java.io.ObjectOutputStream
import kotlin.random.Random

fun main() {
    val totalFrutas = 22
    val qtdSelecionadas = 16
    val frutasSorteadas = (0 until totalFrutas).shuffled().take(qtdSelecionadas)
    println("FRUTAS: $frutasSorteadas")
    val frutasDestaque = frutasSorteadas.shuffled().take(11)
    println("FRUTAS DESTAQUE: $frutasDestaque")
    var rodada = 0


    val serverSocket = ServerSocket(12345)
    println("Servidor rodando na porta 12345...")

    var numPlayersConnected = 0
    val clients = mutableMapOf<Int, ObjectOutputStream>()
    var nextId = 1

    while (true) {
        val clientSocket = serverSocket.accept()
        val id = nextId++
        println("Novo cliente conectado!, $id")

        thread {
            val oos = ObjectOutputStream(clientSocket.getOutputStream())
            val reader = BufferedReader(InputStreamReader(clientSocket.getInputStream()))

            synchronized(clients) { clients[id] = oos }

            try {
                while (true) {
                    val mensagem = reader.readLine() ?: break

                    when (mensagem) {
                        "HELLO" -> {
                            // Aqui envia texto, pois cliente usa readLine()
                            oos.writeObject("HELLOOK")
                            oos.flush()
                        }
                        "READY" -> {
                            println("READY CHEGOU!")
                            numPlayersConnected++
                            println("Jogadores prontos: $numPlayersConnected")

                            if (numPlayersConnected == 2) {
                                println("Todos prontos, enviando frutas...")
                                numPlayersConnected = 0
                                synchronized(clients) {
                                    clients.values.forEach { out ->
                                        out.writeObject(frutasSorteadas)
                                        out.writeObject(frutasDestaque)
                                        out.flush()
                                    }
                                }
                            }
                        }
                        "STARTGAME_OK" -> {
                            println("STARTGAME_OK CHEGOU!")
                        }
                        "SENDFRUIT" -> {
                            val msg = reader.readLine().toInt()
                            println("$msg")

                            println("SENDFRUIT FROM JOGADOR, $id")

                            if(msg == frutasDestaque[rodada]){
                                rodada++
                                if(rodada == 11){
                                    synchronized(clients) {
                                        clients.values.forEach { out ->
                                            out.writeObject("GAMEFINISH|")
                                            out.flush()
                                        }
                                    }
                                }
                                println("$rodada")
                                synchronized(clients) {
                                    clients.values.forEach { out ->
                                        out.writeObject("RIGHTANSWER")
                                        out.flush()
                                    }
                                }
                            }
                            else {
                                println("Errou!!!!")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                println("Erro com cliente: ${e.message}")
            } finally {
                synchronized(clients) { clients.values.remove(oos) }
                clientSocket.close()
                numPlayersConnected = 0
                println("Cliente desconectado, jogadores: $numPlayersConnected")
            }
        }
    }
}
