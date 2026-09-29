package com.example.gametracker.modelos

data class JogoMock(
    val titulo: String,
    val status: String? = null
)

data class JogadorMock(
    val id: String,
    val nome: String,
    val jogos: List<JogoMock>
)
