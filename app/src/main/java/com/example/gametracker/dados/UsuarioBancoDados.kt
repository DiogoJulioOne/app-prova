package com.example.gametracker.dados

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.gametracker.modelos.Usuario

@Dao
abstract class UsuarioBancoDados {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun inserirIgnorandoDuplicados(usuario: Usuario): Long

    @Update
    abstract suspend fun atualizar(usuario: Usuario)

    @Query("SELECT * FROM usuarios WHERE id = :id LIMIT 1")
    abstract suspend fun buscarPorId(id: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE usuario = :usuario COLLATE NOCASE LIMIT 1")
    abstract suspend fun buscarPorUsuario(usuario: String): Usuario?

    @Query("SELECT * FROM usuarios WHERE email = :email COLLATE NOCASE LIMIT 1")
    abstract suspend fun buscarPorEmail(email: String): Usuario?

    @Query("SELECT EXISTS(SELECT 1 FROM app_migrations WHERE migrationKey = :migrationKey)")
    abstract suspend fun migracaoConcluida(migrationKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun registrarMigracao(migracao: DatabaseMigration)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    abstract suspend fun garantirUsuarioLegado(usuario: Usuario): Long

    @Transaction
    open suspend fun cadastrar(usuario: Usuario): Boolean {
        if (buscarPorUsuario(usuario.usuario) != null) return false

        if (usuario.usuario.equals(Usuario.LOGIN_LEGADO, ignoreCase = true)) {
            val legado = buscarPorId(Usuario.ID_LEGADO)
            if (legado != null) {
                atualizar(
                    legado.copy(
                        nome = usuario.nome,
                        usuario = usuario.usuario,
                        senhaSalt = usuario.senhaSalt,
                        senhaHash = usuario.senhaHash
                    )
                )
                return true
            }
        }
        return inserirIgnorandoDuplicados(usuario) != -1L
    }

    @Transaction
    open suspend fun importarUsuariosLegados(
        migrationKey: String,
        usuarios: List<Usuario>,
        usuarioLegado: Usuario
    ) {
        if (migracaoConcluida(migrationKey)) return
        garantirUsuarioLegado(usuarioLegado)
        usuarios.forEach { usuario ->
            if (usuario.usuario.equals(Usuario.LOGIN_LEGADO, ignoreCase = true)) {
                buscarPorId(Usuario.ID_LEGADO)?.let { atual ->
                    if (atual.senhaHash.isBlank() && usuario.senhaHash.isNotBlank()) {
                        atualizar(atual.copy(
                            nome = usuario.nome,
                            usuario = usuario.usuario,
                            senhaSalt = usuario.senhaSalt,
                            senhaHash = usuario.senhaHash
                        ))
                    }
                }
            } else {
                inserirIgnorandoDuplicados(usuario)
            }
        }
        registrarMigracao(DatabaseMigration(migrationKey))
    }
}
