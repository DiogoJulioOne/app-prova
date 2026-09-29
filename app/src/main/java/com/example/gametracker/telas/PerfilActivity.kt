package com.example.gametracker.telas

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gametracker.R
import com.example.gametracker.dados.JogoRepository
import com.example.gametracker.dados.UsuarioRepository
import com.example.gametracker.modelos.Jogo
import com.example.gametracker.modelos.Usuario
import kotlinx.coroutines.launch
import java.util.Locale

class PerfilActivity : AppCompatActivity() {
    private val usuarios by lazy { UsuarioRepository(this) }
    private val jogos by lazy { JogoRepository(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        findViewById<ImageButton>(R.id.buttonVoltarPerfil).setOnClickListener { finish() }
        findViewById<Button>(R.id.buttonEditarPerfil).setOnClickListener {
            startActivity(Intent(this, EditarPerfilActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
        findViewById<Button>(R.id.buttonSairPerfil).setOnClickListener {
            usuarios.encerrarSessao()
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
            finishAffinity()
        }
    }

    override fun onResume() {
        super.onResume()
        lifecycleScope.launch {
            val usuario = usuarios.usuarioDaSessao()
            if (usuario == null) {
                voltarAoLogin()
                return@launch
            }
            val biblioteca = jogos.listar(usuario.id)
            exibirPerfil(usuario, biblioteca)
        }
    }

    private fun exibirPerfil(usuario: Usuario, biblioteca: List<Jogo>) {
        findViewById<TextView>(R.id.textoNomePerfil).text = usuario.nome
        // novo: nickname, email, bio, jogo favorito e avatar
        findViewById<TextView>(R.id.textoNicknamePerfil)?.text = "@${usuario.nickname.ifBlank { usuario.usuario }}"
        findViewById<TextView>(R.id.textoEmailPerfil)?.text = usuario.email.ifBlank { getString(R.string.nao_informado) }
        findViewById<TextView>(R.id.textoBioPerfil)?.text = usuario.bio.ifBlank { getString(R.string.nao_informado) }
        findViewById<TextView>(R.id.textoJogoFavoritoPerfil)?.text = usuario.jogoFavorito.ifBlank { getString(R.string.nao_informado) }
        val avatarView =
            findViewById<android.widget.ImageView>(R.id.imagemAvatarPerfil)

        val avatarKey = usuario.avatar.ifBlank { "avatar_1" }

        try {
            when {
                avatarKey.startsWith("uri:") -> {
                    val uriString = avatarKey.removePrefix("uri:")
                    val uri = android.net.Uri.parse(uriString)

                    try {
                        val inputStream = contentResolver.openInputStream(uri)

                        inputStream?.use { stream ->
                            val bmp =
                                android.graphics.BitmapFactory.decodeStream(stream)

                            if (bmp != null) {
                                avatarView.setImageBitmap(bmp)
                            } else {
                                avatarView.setImageResource(R.drawable.avatar_real_1)
                            }
                        } ?: avatarView.setImageResource(R.drawable.avatar_real_1)

                    } catch (e: Exception) {
                        try {
                            avatarView.setImageURI(uri)
                        } catch (_: Exception) {
                            avatarView.setImageResource(R.drawable.avatar_real_1)
                        }
                    }
                }

                avatarKey.startsWith("file:") -> {
                    val path = avatarKey.removePrefix("file:")
                    val file = java.io.File(path)

                    if (file.exists()) {
                        try {
                            val bmp =
                                android.graphics.BitmapFactory.decodeFile(file.absolutePath)

                            if (bmp != null) {
                                avatarView.setImageBitmap(bmp)
                            } else {
                                avatarView.setImageResource(R.drawable.avatar_real_1)
                            }
                        } catch (e: Exception) {
                            avatarView.setImageResource(R.drawable.avatar_real_1)
                        }
                    } else {
                        avatarView.setImageResource(R.drawable.avatar_real_1)
                    }
                }

                else -> {
                    val res = when (avatarKey) {
                        "avatar_1" -> R.drawable.avatar_real_1
                        "avatar_2" -> R.drawable.avatar_real_2
                        "avatar_3" -> R.drawable.avatar_real_3
                        "avatar_4" -> R.drawable.avatar_real_4
                        "avatar_5" -> R.drawable.avatar_real_5
                        "avatar_6" -> R.drawable.avatar_real_6
                        "avatar_7" -> R.drawable.avatar_real_7
                        "avatar_8" -> R.drawable.avatar_real_8
                        "avatar_9" -> R.drawable.avatar_real_9
                        else -> R.drawable.avatar_real_1
                    }

                    avatarView.setImageResource(res)
                }
            }

        } catch (e: Exception) {
            avatarView.setImageResource(R.drawable.avatar_real_1)
        }
        preencherCard(R.id.cardTotalJogos, biblioteca.size, R.string.perfil_jogos)
        preencherCard(R.id.cardJogando, contarStatus(biblioteca, STATUS_JOGANDO), R.string.perfil_jogando)
        preencherCard(R.id.cardPausados, contarStatus(biblioteca, STATUS_PAUSADO), R.string.perfil_pausados)
        preencherCard(
            R.id.cardZerados,
            contarStatus(biblioteca, STATUS_ZERADO, STATUS_CONCLUIDO_ANTERIOR),
            R.string.perfil_zerados
        )
        preencherCard(
            R.id.cardQueroJogar,
            contarStatus(biblioteca, STATUS_QUERO_JOGAR),
            R.string.perfil_quero_jogar
        )

        val totalHoras = biblioteca.sumOf { it.horasJogadas?.takeIf(Double::isFinite)?.coerceAtLeast(0.0) ?: 0.0 }
        findViewById<TextView>(R.id.textoTotalHoras).text = getString(
            R.string.perfil_horas_valor,
            formatNumero(totalHoras)
        )

        val maisJogado = biblioteca
            .filter { it.horasJogadas?.isFinite() == true && it.horasJogadas >= 0.0 }
            .maxByOrNull { it.horasJogadas ?: 0.0 }
        findViewById<TextView>(R.id.textoMaisJogado).text =
            maisJogado?.let {
                getString(R.string.perfil_mais_jogado_valor, it.titulo, formatNumero(it.horasJogadas ?: 0.0))
            } ?: getString(R.string.nao_informado)

        val progressos = biblioteca.mapNotNull { it.progresso?.coerceIn(0, 100) }
        val progressoMedio = if (progressos.isEmpty()) 0 else progressos.average().toInt()
        findViewById<TextView>(R.id.textoProgressoMedio).text =
            getString(R.string.perfil_progresso_valor, progressoMedio)
        findViewById<ProgressBar>(R.id.progressBarProgressoMedio).progress = progressoMedio

        val zerados = contarStatus(biblioteca, STATUS_ZERADO, STATUS_CONCLUIDO_ANTERIOR)
        findViewById<TextView>(R.id.textoJogosConcluidos).text =
            getString(R.string.perfil_concluidos_valor, zerados, biblioteca.size)
        findViewById<TextView>(R.id.textoPerfilVazio).visibility =
            if (biblioteca.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun preencherCard(cardId: Int, valor: Int, label: Int) {
        val card = findViewById<android.widget.LinearLayout>(cardId)
        (card.getChildAt(0) as TextView).text = valor.toString()
        (card.getChildAt(1) as TextView).setText(label)
    }

    private fun contarStatus(jogos: List<Jogo>, vararg status: String): Int =
        jogos.count { jogo -> status.any { jogo.status.equals(it, ignoreCase = true) } }

    private fun formatNumero(valor: Double): String =
        String.format(Locale.getDefault(), if (valor % 1.0 == 0.0) "%.0f" else "%.1f", valor)

    private fun voltarAoLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finishAffinity()
    }

    private companion object {
        const val STATUS_JOGANDO = "Jogando"
        const val STATUS_PAUSADO = "Pausado"
        const val STATUS_ZERADO = "Zerado"
        const val STATUS_QUERO_JOGAR = "Quero jogar"
        const val STATUS_CONCLUIDO_ANTERIOR = "Concluído"
    }
}
