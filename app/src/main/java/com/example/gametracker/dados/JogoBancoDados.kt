package com.example.gametracker.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.gametracker.modelos.Jogo
import com.example.gametracker.modelos.Usuario

@Dao
abstract class JogoBancoDados {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun inserirSeNovo(jogo: Jogo): Long

    @Insert
    abstract suspend fun inserirTodos(jogos: List<Jogo>)

    @Query("SELECT * FROM jogos WHERE usuarioId = :usuarioId ORDER BY titulo COLLATE NOCASE")
    abstract suspend fun listar(usuarioId: String): List<Jogo>

    @Query("SELECT * FROM jogos WHERE id = :id AND usuarioId = :usuarioId LIMIT 1")
    abstract suspend fun buscarPorId(id: String, usuarioId: String): Jogo?

    @Query(
        "UPDATE jogos SET titulo = :titulo, plataforma = :plataforma, status = :status, " +
            "dataInicio = :dataInicio, horasJogadas = :horasJogadas, progresso = :progresso, " +
            "observacoes = :observacoes WHERE id = :id AND usuarioId = :usuarioId"
    )
    abstract suspend fun atualizar(
        id: String,
        usuarioId: String,
        titulo: String,
        plataforma: String,
        status: String,
        dataInicio: String,
        horasJogadas: Double?,
        progresso: Int?,
        observacoes: String
    ): Int

    @Query("DELETE FROM jogos WHERE id = :id AND usuarioId = :usuarioId")
    abstract suspend fun excluirPorId(id: String, usuarioId: String)

    @Transaction
    open suspend fun salvarDoUsuario(jogo: Jogo, usuarioId: String) {
        if (buscarPorId(jogo.id, usuarioId) == null) {
            inserirSeNovo(jogo.copy(usuarioId = usuarioId))
        } else {
            atualizar(
                id = jogo.id,
                usuarioId = usuarioId,
                titulo = jogo.titulo,
                plataforma = jogo.plataforma,
                status = jogo.status,
                dataInicio = jogo.dataInicio,
                horasJogadas = jogo.horasJogadas,
                progresso = jogo.progresso,
                observacoes = jogo.observacoes
            )
        }
    }

    @Query("SELECT EXISTS(SELECT 1 FROM app_migrations WHERE migrationKey = :migrationKey)")
    abstract suspend fun migracaoConcluida(migrationKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun registrarMigracao(migracao: DatabaseMigration)

    @Transaction
    open suspend fun importarLegadoUmaVez(migrationKey: String, jogos: List<Jogo>) {
        if (!migracaoConcluida(migrationKey)) {
            if (jogos.isNotEmpty()) inserirTodos(jogos.map {
                it.copy(usuarioId = Usuario.ID_LEGADO)
            })
            registrarMigracao(DatabaseMigration(migrationKey))
        }
    }
}
