package com.example.gametracker.dados

import com.example.gametracker.modelos.JogadorMock
import com.example.gametracker.modelos.JogoMock
import com.example.gametracker.modelos.MensagemMock

object DadosSociaisMock {
    val jogadores = listOf(
        JogadorMock("lucas", "Lucas", listOf(
            JogoMock("Minecraft", "Jogando"), JogoMock("Valorant", "Jogando"),
            JogoMock("Elden Ring", "Zerado"), JogoMock("GTA V", "Pausado"),
            JogoMock("Terraria", "Jogando")
        )),
        JogadorMock("marina", "Marina", listOf(
            JogoMock("Minecraft", "Jogando"), JogoMock("Stardew Valley", "Jogando"),
            JogoMock("Terraria", "Zerado"), JogoMock("The Witcher 3", "Quero jogar")
        )),
        JogadorMock("rafael", "Rafael", listOf(
            JogoMock("Valorant", "Jogando"), JogoMock("Counter-Strike 2", "Jogando"),
            JogoMock("GTA V", "Zerado"), JogoMock("Fortnite", "Pausado")
        )),
        JogadorMock("bia", "Bia", listOf(
            JogoMock("Stardew Valley", "Jogando"), JogoMock("Minecraft", "Quero jogar"),
            JogoMock("Fortnite", "Jogando"), JogoMock("Terraria", "Jogando")
        )),
        JogadorMock("pedro", "Pedro", listOf(
            JogoMock("Elden Ring", "Jogando"), JogoMock("The Witcher 3", "Zerado"),
            JogoMock("Red Dead Redemption 2", "Jogando"), JogoMock("GTA V", "Jogando")
        )),
        JogadorMock("ana", "Ana", listOf(
            JogoMock("Minecraft", "Jogando"), JogoMock("The Witcher 3", "Jogando"),
            JogoMock("Stardew Valley", "Zerado"), JogoMock("Counter-Strike 2", "Quero jogar")
        )),
        JogadorMock("gui", "Gui", listOf(
            JogoMock("Fortnite", "Jogando"), JogoMock("Valorant", "Pausado"),
            JogoMock("Minecraft", "Jogando"), JogoMock("GTA V", "Zerado")
        )),
        JogadorMock("sofia", "Sofia", listOf(
            JogoMock("Red Dead Redemption 2", "Jogando"), JogoMock("Elden Ring", "Zerado"),
            JogoMock("Stardew Valley", "Jogando"), JogoMock("Terraria", "Quero jogar")
        )),
        JogadorMock("diego", "Diego", listOf(
            JogoMock("Counter-Strike 2", "Jogando"), JogoMock("GTA V", "Jogando"),
            JogoMock("Minecraft", "Pausado"), JogoMock("Fortnite", "Zerado")
        )),
        JogadorMock("julia", "Julia", listOf(
            JogoMock("The Witcher 3", "Jogando"), JogoMock("Minecraft", "Jogando"),
            JogoMock("Red Dead Redemption 2", "Zerado"), JogoMock("Valorant", "Quero jogar")
        ))
    )

    private val idsDeConversa = listOf("lucas", "marina", "rafael", "bia", "pedro")
    private val mensagens = mutableMapOf(
        "lucas" to mutableListOf(
            MensagemMock("Oi! Vi que você também curte Minecraft.", false, "14:20"),
            MensagemMock("Sim! Estou montando um mundo novo.", true, "14:23"),
            MensagemMock("Legal, vamos jogar qualquer dia!", false, "14:25")
        ),
        "marina" to mutableListOf(
            MensagemMock("Você já chegou ao inverno no Stardew?", false, "Ontem"),
            MensagemMock("Ainda não, estou quase lá!", true, "Ontem")
        ),
        "rafael" to mutableListOf(
            MensagemMock("Bora uma partida mais tarde?", false, "Ontem")
        ),
        "bia" to mutableListOf(
            MensagemMock("Adorei sua lista de jogos!", false, "Seg")
        ),
        "pedro" to mutableListOf(
            MensagemMock("Qual build você usou em Elden Ring?", false, "Dom")
        )
    )

    fun jogador(id: String): JogadorMock? = jogadores.firstOrNull { it.id == id }

    fun conversas(): List<Pair<JogadorMock, MensagemMock?>> =
        idsDeConversa.mapNotNull { id ->
            jogador(id)?.let { it to mensagens[id]?.lastOrNull() }
        }

    fun mensagens(jogadorId: String): List<MensagemMock> =
        mensagens[jogadorId]?.toList().orEmpty()

    fun enviar(jogadorId: String, texto: String) {
        mensagens.getOrPut(jogadorId) { mutableListOf() }
            .add(MensagemMock(texto, true, "Agora"))
    }
}
