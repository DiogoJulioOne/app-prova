package com.example.gametracker.dados

import android.content.Context
import com.example.gametracker.modelos.Jogo
import com.example.gametracker.modelos.Usuario
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class JogoRepository(context: Context) {
    private val preferences = context.getSharedPreferences("game_tracker", Context.MODE_PRIVATE)
    private val database = AppDatabase.obter(context)
    private val dao = database.jogoBancoDados()
    private val usuarioDao = database.usuarioBancoDados()

    suspend fun listar(usuarioId: String): List<Jogo> = withContext(Dispatchers.IO) {
        importarDadosLegadosSeNecessario()
        dao.listar(usuarioId)
    }

    suspend fun buscarPorId(id: String, usuarioId: String): Jogo? = withContext(Dispatchers.IO) {
        importarDadosLegadosSeNecessario()
        dao.buscarPorId(id, usuarioId)
    }

    suspend fun salvar(jogo: Jogo, usuarioId: String) = withContext(Dispatchers.IO) {
        importarDadosLegadosSeNecessario()
        dao.salvarDoUsuario(jogo, usuarioId)
    }

    suspend fun excluir(id: String, usuarioId: String) = withContext(Dispatchers.IO) {
        importarDadosLegadosSeNecessario()
        dao.excluirPorId(id, usuarioId)
    }

    private suspend fun importarDadosLegadosSeNecessario() {
        usuarioDao.garantirUsuarioLegado(usuarioLegado())
        if (dao.migracaoConcluida(LEGACY_MIGRATION_KEY)) return

        val jsonLegado = preferences.getString(LEGACY_GAMES_KEY, null)
        val jogosLegados = if (jsonLegado.isNullOrBlank()) {
            emptyList()
        } else {
            JSONArray(jsonLegado).toJogosLegados()
        }
        dao.importarLegadoUmaVez(
            LEGACY_MIGRATION_KEY,
            jogosLegados.map { it.copy(usuarioId = Usuario.ID_LEGADO) }
        )
    }

    private fun JSONArray.toJogosLegados(): List<Jogo> =
        (0 until length()).map { indice ->
            val jogo = getJSONObject(indice)
            Jogo(
                id = jogo.getString("id"),
                titulo = jogo.getString("titulo"),
                plataforma = jogo.getString("plataforma"),
                status = jogo.getString("status"),
                dataInicio = jogo.optString("dataInicio", "").takeUnless { it == "null" }.orEmpty(),
                horasJogadas = jogo.opt("horasJogadas")
                    .takeUnless { it == null || it === JSONObject.NULL }
                    ?.let { it as? Number }
                    ?.toDouble()
                    ?.takeIf { it.isFinite() && it >= 0.0 },
                progresso = jogo.opt("progresso")
                    .takeUnless { it == null || it === JSONObject.NULL }
                    ?.let { it as? Number }
                    ?.toInt()
                    ?.coerceIn(0, 100),
                observacoes = jogo.optString("observacoes", "")
                    .takeUnless { it == "null" }
                    .orEmpty()
            )
        }

    private fun usuarioLegado(): Usuario = Usuario(
        id = Usuario.ID_LEGADO,
        nome = "Jogador",
        usuario = Usuario.USUARIO_LEGADO,
        senhaSalt = "",
        senhaHash = ""
    )

    companion object {
        private const val LEGACY_GAMES_KEY = "jogos_jogador"
        private const val LEGACY_MIGRATION_KEY = "shared_preferences_games_v1"
    }
}
