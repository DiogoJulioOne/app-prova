package com.example.gametracker.modelos

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "usuarios",
    indices = [Index(value = ["usuario"], unique = true)]
)
data class Usuario(
    @PrimaryKey val id: String,
    val nome: String,
    val usuario: String,
    // novos campos de perfil (compatíveis com dados antigos via defaults)
    val nickname: String = "",
    val email: String = "",
    val bio: String = "",
    val jogoFavorito: String = "",
    val avatar: String = "avatar_1",
    val senhaSalt: String,
    val senhaHash: String
) {
    companion object {
        const val ID_LEGADO = "legacy-jogador"
        const val USUARIO_LEGADO = "__legacy_jogador__"
        const val LOGIN_LEGADO = "jogador"
    }
}
