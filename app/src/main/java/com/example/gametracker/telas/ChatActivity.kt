package com.example.gametracker.telas

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.gametracker.R
import com.example.gametracker.dados.DadosSociaisMock
import com.example.gametracker.modelos.MensagemMock

class ChatActivity : AppCompatActivity() {
    private var jogadorId: String? = null
    private lateinit var listaMensagens: LinearLayout
    private lateinit var scrollMensagens: ScrollView
    private lateinit var campoMensagem: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        jogadorId = intent.getStringExtra(EXTRA_JOGADOR_ID)
        val jogador = jogadorId?.let(DadosSociaisMock::jogador)
        if (jogador == null) {
            finish()
            return
        }

        findViewById<Button>(R.id.buttonVoltarChat).setOnClickListener { finish() }
        findViewById<TextView>(R.id.textoNomeJogadorChat).text = jogador.nome
        listaMensagens = findViewById(R.id.listaMensagens)
        scrollMensagens = findViewById(R.id.scrollMensagens)
        campoMensagem = findViewById(R.id.editMensagem)
        atualizarMensagens()

        findViewById<Button>(R.id.buttonEnviarMensagem).setOnClickListener {
            val texto = campoMensagem.text.toString().trim()
            val id = jogadorId
            if (texto.isNotEmpty() && id != null) {
                DadosSociaisMock.enviar(id, texto)
                campoMensagem.text.clear()
                atualizarMensagens()
            }
        }
    }

    private fun atualizarMensagens() {
        listaMensagens.removeAllViews()
        DadosSociaisMock.mensagens(jogadorId.orEmpty()).forEach { mensagem ->
            listaMensagens.addView(criarMensagem(mensagem))
        }
        scrollMensagens.post { scrollMensagens.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun criarMensagem(mensagem: MensagemMock): LinearLayout {
        val linha = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = if (mensagem.enviadaPeloUsuario) Gravity.END else Gravity.START
            setPadding(dp(4), dp(3), dp(4), dp(3))
        }
        val balao = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(13), dp(9), dp(13), dp(7))
            setBackgroundResource(
                if (mensagem.enviadaPeloUsuario) R.drawable.bg_bolha_enviada
                else R.drawable.bg_bolha_recebida
            )
        }
        balao.addView(TextView(this).apply {
            text = mensagem.texto
            textSize = 15f
            setTextColor(getColor(R.color.texto))
            maxWidth = (resources.displayMetrics.widthPixels * 0.78f).toInt()
        })
        balao.addView(TextView(this).apply {
            text = mensagem.horario
            textSize = 11f
            gravity = Gravity.END
            setTextColor(getColor(R.color.texto_secundario))
            setPadding(0, dp(3), 0, 0)
        })
        linha.addView(balao, LinearLayout.LayoutParams(-2, -2))
        return linha
    }

    private fun dp(valor: Int) = (valor * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_JOGADOR_ID = "jogador_id"
    }
}
