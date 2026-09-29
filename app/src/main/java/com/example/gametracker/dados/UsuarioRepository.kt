package com.example.gametracker.dados

import android.content.Context
import com.example.gametracker.modelos.Usuario
import com.example.gametracker.utils.PasswordUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

class UsuarioRepository(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val dao = AppDatabase.obter(context).usuarioBancoDados()

    suspend fun cadastrar(nome: String, senha: String): Usuario? = withContext(Dispatchers.IO) {
        prepararDadosLegados()
        val login = normalizar(nome)
        val salt = PasswordUtils.gerarSalt()
        val usuario = Usuario(
            id = if (login == Usuario.LOGIN_LEGADO) Usuario.ID_LEGADO else UUID.randomUUID().toString(),
            nome = nome.trim(),
            usuario = login,
            // novos campos iniciados com defaults
            nickname = "",
            email = "",
            bio = "",
            jogoFavorito = "",
            avatar = "avatar_1",
            senhaSalt = PasswordUtils.toHex(salt),
            senhaHash = PasswordUtils.hash(salt, senha)
        )
        if (dao.cadastrar(usuario)) usuario else null
    }

    suspend fun autenticar(nome: String, senha: String): Usuario? = withContext(Dispatchers.IO) {
        prepararDadosLegados()
        val usuario = dao.buscarPorUsuario(normalizar(nome)) ?: return@withContext null
        if (usuario.senhaSalt.isBlank() || usuario.senhaHash.isBlank()) {
            return@withContext null
        }
        val hashInformado = PasswordUtils.hash(
            PasswordUtils.hexToBytes(usuario.senhaSalt),
            senha
        )
        usuario.takeIf {
            PasswordUtils.compararHashes(usuario.senhaHash, hashInformado)
        }
    }

    suspend fun usuarioDaSessao(): Usuario? = withContext(Dispatchers.IO) {
        prepararDadosLegados()
        val sessionId = preferences.getString(KEY_USUARIO_ID, null)
        if (sessionId != null) {
            val usuario = dao.buscarPorId(sessionId)
            if (usuario != null) return@withContext usuario
            preferences.edit().remove(KEY_USUARIO_ID).apply()
        }

        val loginAnterior = preferences.getString(KEY_USUARIO_ATUAL, null)
            ?.let(::normalizar)
            ?: return@withContext null
        val usuarioAnterior = dao.buscarPorUsuario(loginAnterior)
        if (usuarioAnterior != null) iniciarSessao(usuarioAnterior.id)
        preferences.edit().remove(KEY_USUARIO_ATUAL).apply()
        usuarioAnterior
    }

    suspend fun atualizarPerfil(usuario: Usuario) = withContext(Dispatchers.IO) {
        dao.atualizar(usuario)
    }

    suspend fun buscarPorEmail(email: String): Usuario? = withContext(Dispatchers.IO) {
        dao.buscarPorEmail(email)
    }

    fun iniciarSessao(usuarioId: String) {
        preferences.edit()
            .putString(KEY_USUARIO_ID, usuarioId)
            .remove(KEY_USUARIO_ATUAL)
            .apply()
    }

    fun encerrarSessao() {
        preferences.edit()
            .remove(KEY_USUARIO_ID)
            .remove(KEY_USUARIO_ATUAL)
            .apply()
    }

    private suspend fun prepararDadosLegados() {
        dao.garantirUsuarioLegado(usuarioLegado())
        if (dao.migracaoConcluida(LEGACY_USERS_MIGRATION_KEY)) return

        val usuariosLegados = preferences.all.mapNotNull { (chave, valor) ->
            if (
                chave == KEY_USUARIO_ATUAL ||
                !chave.startsWith(LEGACY_USER_PREFIX) ||
                valor !is String
            ) return@mapNotNull null
            val login = normalizar(chave.removePrefix(LEGACY_USER_PREFIX))
            if (login.isBlank() || login == Usuario.USUARIO_LEGADO) return@mapNotNull null

            val dados = JSONObject(valor)
            val salt = dados.optString("salt", "")
            val hash = dados.optString("hashSenha", "")
            if (salt.isBlank() || hash.isBlank()) return@mapNotNull null
            Usuario(
                id = if (login == Usuario.LOGIN_LEGADO) {
                    Usuario.ID_LEGADO
                } else {
                    "legacy-user-$login"
                },
                nome = dados.optString("nome", login).ifBlank { login },
                usuario = login,
                nickname = dados.optString("nickname", ""),
                email = dados.optString("email", ""),
                bio = dados.optString("bio", ""),
                jogoFavorito = dados.optString("jogoFavorito", ""),
                avatar = dados.optString("avatar", "avatar_1"),
                senhaSalt = salt,
                senhaHash = hash
            )
        }
        dao.importarUsuariosLegados(
            LEGACY_USERS_MIGRATION_KEY,
            usuariosLegados,
            usuarioLegado()
        )
    }

    private fun usuarioLegado() = Usuario(
        id = Usuario.ID_LEGADO,
        nome = "Jogador",
        usuario = Usuario.USUARIO_LEGADO,
        nickname = "",
        email = "",
        bio = "",
        jogoFavorito = "",
        avatar = "avatar_1",
        senhaSalt = "",
        senhaHash = ""
    )

    private fun normalizar(nome: String): String = nome.trim().lowercase()

    private companion object {
        const val PREFERENCES_NAME = "game_tracker"
        const val KEY_USUARIO_ID = "usuario_id"
        const val KEY_USUARIO_ATUAL = "usuario_atual"
        const val LEGACY_USER_PREFIX = "usuario_"
        const val LEGACY_USERS_MIGRATION_KEY = "shared_preferences_users_v1"
    }
}
