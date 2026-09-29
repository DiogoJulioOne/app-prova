package com.example.gametracker.telas

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.gametracker.R
import com.example.gametracker.dados.DadosSociaisMock
import com.example.gametracker.dados.JogoRepository
import com.example.gametracker.dados.UsuarioRepository
import com.example.gametracker.modelos.JogadorMock
import kotlinx.coroutines.launch

class MatchActivity : AppCompatActivity() {
    private val usuarios by lazy { UsuarioRepository(this) }
    private val jogos by lazy { JogoRepository(this) }
    private lateinit var campoPesquisa: EditText
    private lateinit var resultados: LinearLayout
    private lateinit var textoVazio: TextView
    private var titulosJogosDoUsuario = emptySet<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_match)
        campoPesquisa = findViewById(R.id.editPesquisaMatch)
        resultados = findViewById(R.id.listaResultadosMatch)
        textoVazio = findViewById(R.id.textoVazioMatch)

        findViewById<Button>(R.id.buttonVoltarMatch).setOnClickListener { finish() }
        campoPesquisa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mostrarResultados()
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        lifecycleScope.launch {
            val usuario = usuarios.usuarioDaSessao()
            if (usuario == null) {
                finish()
                return@launch
            }
            titulosJogosDoUsuario = jogos.listar(usuario.id)
                .map { normalizar(it.titulo) }
                .toSet()
            mostrarResultados()
        }
    }

    private fun mostrarResultados() {
        val pesquisa = campoPesquisa.text.toString().trim()
        resultados.removeAllViews()
        if (pesquisa.isBlank()) {
            // Show all players when search field is empty
            textoVazio.visibility = View.GONE
            DadosSociaisMock.jogadores.forEach { jogador ->
                val primeiroJogo = jogador.jogos.firstOrNull()
                if (primeiroJogo != null) {
                    resultados.addView(cardJogador(jogador, primeiroJogo.titulo, primeiroJogo.status))
                }
            }
            return
        }

        // Filter players by name or nickname (id)
        val encontrados = DadosSociaisMock.jogadores.filter { jogador ->
            jogador.nome.contains(pesquisa, ignoreCase = true) ||
            jogador.id.contains(pesquisa, ignoreCase = true)
        }
        textoVazio.visibility = if (encontrados.isEmpty()) View.VISIBLE else View.GONE
        if (encontrados.isEmpty()) {
            textoVazio.setText(R.string.match_nenhum_jogador)
        }
        encontrados.forEach { jogador ->
            val primeiroJogo = jogador.jogos.firstOrNull()
            if (primeiroJogo != null) {
                resultados.addView(cardJogador(jogador, primeiroJogo.titulo, primeiroJogo.status))
            }
        }
    }

    private fun cardJogador(jogador: JogadorMock, tituloJogo: String, status: String?): View {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
            background = getDrawable(R.drawable.bg_card_jogo)
        }
        card.addView(TextView(this).apply {
            text = jogador.nome
            textSize = 18f
            setTextColor(getColor(R.color.texto))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        card.addView(TextView(this).apply {
            text = if (status.isNullOrBlank()) tituloJogo else "$tituloJogo • $status"
            textSize = 14f
            setTextColor(getColor(R.color.texto_secundario))
            setPadding(0, dp(6), 0, dp(4))
        })
        val comuns = jogador.jogos.map { normalizar(it.titulo) }.toSet()
            .intersect(titulosJogosDoUsuario).size
        card.addView(TextView(this).apply {
            text = resources.getQuantityString(R.plurals.match_jogos_em_comum, comuns, comuns)
            textSize = 13f
            setTextColor(getColor(R.color.destaque))
        })
        card.addView(Button(this).apply {
            text = getString(R.string.conversar)
            setTextColor(getColor(R.color.texto))
            setBackgroundResource(R.drawable.bg_botao_principal)
            isAllCaps = false
            setOnClickListener { abrirChat(jogador.id) }
        }, LinearLayout.LayoutParams(-1, dp(46)).apply { topMargin = dp(10) })
        return card.also {
            val params = LinearLayout.LayoutParams(-1, -2)
            params.bottomMargin = dp(12)
            it.layoutParams = params
        }
    }

    private fun abrirChat(jogadorId: String) {
        startActivity(Intent(this, ChatActivity::class.java).putExtra(ChatActivity.EXTRA_JOGADOR_ID, jogadorId))
    }

    private fun normalizar(valor: String) = valor.trim().lowercase()
    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()
}
