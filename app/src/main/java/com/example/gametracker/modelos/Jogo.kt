package com.example.gametracker.modelos

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "jogos",
    foreignKeys = [
        ForeignKey(
            entity = Usuario::class,
            parentColumns = ["id"],
            childColumns = ["usuarioId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["usuarioId"])]
)
data class Jogo(
    @PrimaryKey
    val id: String,
    val titulo: String,
    val plataforma: String,
    val status: String,
    val dataInicio: String = "",
    val horasJogadas: Double? = null,
    val progresso: Int? = null,
    val observacoes: String = "",
    val usuarioId: String = Usuario.ID_LEGADO
)
